/*
 * Copyright (C) 2017-2019 HERE Europe B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package com.here.ort.analyzer.managers

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.module.kotlin.readValues

import com.here.ort.analyzer.AbstractPackageManagerFactory
import com.here.ort.analyzer.PackageManager
import com.here.ort.downloader.VersionControlSystem
import com.here.ort.model.Identifier
import com.here.ort.model.Package
import com.here.ort.model.PackageLinkage
import com.here.ort.model.Project
import com.here.ort.model.ProjectAnalyzerResult
import com.here.ort.model.RemoteArtifact
import com.here.ort.model.Scope
import com.here.ort.model.VcsInfo
import com.here.ort.model.Hash
import com.here.ort.model.jsonMapper
import com.here.ort.model.config.AnalyzerConfiguration
import com.here.ort.model.config.RepositoryConfiguration
import com.here.ort.model.yamlMapper
import com.here.ort.utils.CommandLineTool
import com.here.ort.utils.stashDirectories

import java.io.File


/**
 * The [Go Modules](https://github.com/golang/go/wiki/Modules) package manager for Go.
 */
class GoMod(
    name: String,
    analysisRoot: File,
    analyzerConfig: AnalyzerConfiguration,
    repoConfig: RepositoryConfiguration
) : PackageManager(name, analysisRoot, analyzerConfig, repoConfig), CommandLineTool {
    class Factory : AbstractPackageManagerFactory<GoMod>("GoMod") {
        override val globsForDefinitionFiles = listOf("go.mod")

        override fun create(
            analysisRoot: File,
            analyzerConfig: AnalyzerConfiguration,
            repoConfig: RepositoryConfiguration
        ) = GoMod(managerName, analysisRoot, analyzerConfig, repoConfig)
    }

    companion object {
        const val DEFAULT_GO_PROXY = "https://proxy.golang.org"
    }

    override fun command(workingDir: File?) = "go"

    override fun resolveDependencies(definitionFile: File): ProjectAnalyzerResult? {
        val projectDir = definitionFile.parentFile

        if (!analyzerConfig.allowDynamicVersions) {
            requireLockfile(projectDir) { projectDir.resolve("go.sum").isFile }
        }

        val modules = stashDirectories(File(projectDir, "node_modules")).use {
            installDependencies(projectDir)
            listModules(projectDir)
        }

        val goProxy = getGoProxy()

        val (mains, dependencies) = modules.partition { it.isMain }
        val projectName = mains.firstOrNull()?.name.orEmpty()
        val packages = dependencies.asSequence()
            .map {
                val sourceArtifact = if (goProxy != null) {
                    RemoteArtifact(url = "$goProxy/${it.name}/@v/${it.version}.zip", hash = Hash.NONE)
                } else {
                    RemoteArtifact.EMPTY
                }

                Package(
                    id = Identifier(managerName, "", it.name, it.version.orEmpty()),
                    declaredLicenses = sortedSetOf(), // Go mod doesn't support declared licenses
                    description = "",
                    homepageUrl = "",
                    binaryArtifact = RemoteArtifact.EMPTY,
                    sourceArtifact = sourceArtifact,
                    vcs = VcsInfo.EMPTY
                )
            }

        val packageRefs = packages.map { it.toReference(linkage = PackageLinkage.STATIC) }
        val scope = Scope("default", packageRefs.toSortedSet())

        val projectVcs = processProjectVcs(projectDir)
        return ProjectAnalyzerResult(
            project = Project(
                id = Identifier(
                    type = managerName,
                    namespace = "",
                    name = projectName,
                    version = projectVcs.revision
                ),
                definitionFilePath = VersionControlSystem.getPathInfo(definitionFile).path,
                declaredLicenses = sortedSetOf(), // Go mod doesn't support declared licenses
                vcs = projectVcs,
                vcsProcessed = projectVcs,
                homepageUrl = "",
                scopes = sortedSetOf(scope)
            ),
            packages = packages.mapTo(sortedSetOf()) { it.toCuratedPackage() }
        )
    }

    private fun installDependencies(workingDir: File) {
        run("mod", "vendor", workingDir = workingDir).requireSuccess()
        run("mod", "tidy", workingDir = workingDir).requireSuccess()
    }

    private fun listModules(projectDir: File): List<ModuleDependency> {
        val vendorModulesJson = run("list", "-json", "-m", "-mod=vendor", "all", workingDir = projectDir)
            .requireSuccess()
            .stdout

        return jsonMapper
            .readValues<ModuleDependency>(JsonFactory().createParser(vendorModulesJson))
            .asSequence()
            .toList()
    }

    private fun getGoProxy(): String? {
        var goProxy = System.getenv("GOPROXY")
        if (goProxy == "off") {
            return null
        } else if (goProxy == "" || goProxy == null) {
            goProxy = DEFAULT_GO_PROXY
        }

        return goProxy
            .split(",")
            .filterNot { it == "direct" }
            .firstOrNull()
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class ModuleDependency(
    @JsonProperty("Path") val name: String,
    @JsonProperty("Dir") val dir: String,
    @JsonProperty("Version") val version: String?,
    @JsonProperty("Main") val isMain: Boolean = false
)

fun main() {
    val projectDir = File("/home/viernau/sources/github/athens")
    val definitionFile = projectDir.resolve("go.mod")
    val go = GoMod.Factory().create(
        projectDir,
        AnalyzerConfiguration(
            ignoreToolVersions = true,
            allowDynamicVersions = true
        ),
        RepositoryConfiguration()
    )

    val result = go.resolveDependencies(definitionFile)
    println(yamlMapper.writeValueAsString(result))
}
