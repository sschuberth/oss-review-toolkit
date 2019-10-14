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

package com.here.ort

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.findObject
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.pair
import com.github.ajalt.clikt.parameters.options.switch
import com.github.ajalt.clikt.parameters.options.versionOption
import com.github.ajalt.clikt.parameters.types.file

import com.here.ort.commands.*
import com.here.ort.model.Environment
import com.here.ort.model.config.OrtConfiguration
import com.here.ort.utils.expandTilde

import com.typesafe.config.ConfigFactory

import io.github.config4k.extract

import org.apache.logging.log4j.Level

/**
 * The main entry point of the application.
 */
class Ort : CliktCommand() {
    private val configFile by option("--config", "-c", help = "The path to a configuration file.")
        .file(exists = true, readable = true)

    private val env = Environment()

    private val logLevel by option(help = "Set the verbosity level of log output.").switch(
        "--info" to Level.INFO,
        "--debug" to Level.DEBUG
    ).default(Level.WARN)

    private val stacktrace by option(help = "Print out the stacktrace for all exceptions.").flag()

    init {
        context {
            expandArgumentFiles = false
        }

        findObject {
            loadConfig()
        }

        subcommands(
            //AnalyzerCommand,
            //DownloaderCommand,
            //EvaluatorCommand,
            //ReporterCommand,
            //RequirementsCommand,
            ScannerCommand()
        )

        versionOption(
            version = env.ortVersion,
            names = setOf("--version", "-v"),
            help = "Show version information and exit.",
            message = ::getVersionHeader
        )
    }

    private val configArguments by option("-P", help = "Allows to override configuration parameters.").pair().multiple()

    override fun run() = Unit

    private fun getVersionHeader(version: String): String {
        val commandName = context.invokedSubcommand?.commandName
        val variables = env.variables.entries.map { (key, value) -> "$key = $value" }

        val command = commandName?.let { " '$commandName'" }.orEmpty()
        val with = "with".takeUnless { variables.isEmpty() }.orEmpty()

        var variableIndex = 0

        val header = mutableListOf<String>()
        """
            ________ _____________________
            \_____  \\______   \__    ___/ the OSS Review Toolkit, version $version.
             /   |   \|       _/ |    |    Running$command on Java ${env.javaVersion} and ${env.os} $with
            /    |    \    |   \ |    |    ${variables.getOrElse(variableIndex++) { "" }}
            \_______  /____|_  / |____|    ${variables.getOrElse(variableIndex++) { "" }}
                    \/       \/
        """.trimIndent().lines().mapTo(header) { it.trimEnd() }

        val moreVariables = variables.drop(variableIndex)
        if (moreVariables.isNotEmpty()) {
            header += "More environment variables:"
            header += moreVariables
        }

        return header.joinToString("\n", postfix = "\n")
    }

    private fun loadConfig(): OrtConfiguration {
        val argsConfig = ConfigFactory.parseMap(configArguments.toMap(), "Command line").withOnlyPath("ort")
        val fileConfig = configFile?.expandTilde()?.let {
            require(it.isFile) {
                "The provided configuration file '$it' is not actually a file."
            }

            ConfigFactory.parseFile(it).withOnlyPath("ort")
        }
        val defaultConfig = ConfigFactory.parseResources("default.conf")

        var combinedConfig = argsConfig
        if (fileConfig != null) {
            combinedConfig = combinedConfig.withFallback(fileConfig)
        }
        combinedConfig = combinedConfig.withFallback(defaultConfig).resolve()

        return combinedConfig.extract("ort")
    }
}

/**
 * The entry point for the application with [args] being the list of arguments.
 *
 */
fun main(args: Array<String>) = Ort().main(args)
