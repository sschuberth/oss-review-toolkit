/*
 * Copyright (C) 2019 HERE Europe B.V.
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

package com.here.ort.helper.commands

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.here.ort.helper.CommandWithHelp
import com.here.ort.helper.common.IdentifierConverter
import com.here.ort.helper.common.getPackageOrProject
import com.here.ort.model.Identifier
import com.here.ort.model.OrtResult
import com.here.ort.model.readValue
import com.here.ort.utils.PARAMETER_ORDER_MANDATORY
import com.here.ort.utils.PARAMETER_ORDER_OPTIONAL
import java.io.File

@Parameters(
    commandNames = ["list-copyrights"],
    commandDescription = "Lists the license findings for a given package as distinct text locations."
)
internal class ListCopyrightsCommand : CommandWithHelp() {
    @Parameter(
        names = ["--ort-result-file"],
        required = true,
        order = PARAMETER_ORDER_MANDATORY,
        description = "The ORT result file to read as input."
    )
    private lateinit var ortResultFile: File

    @Parameter(
        names = ["--package-id"],
        required = true,
        order = PARAMETER_ORDER_MANDATORY,
        converter = IdentifierConverter::class,
        description = "The target package for which the licenses shall be listed."
    )
    private lateinit var packageId: Identifier

    @Parameter(
        names = ["--license-id"],
        required = false,
        order = PARAMETER_ORDER_OPTIONAL,
        description = ""
    )
    private var licenseId: String? = null

    override fun runCommand(jc: JCommander): Int {
        var ortResult = ortResultFile.readValue<OrtResult>()
        if (ortResult.getPackageOrProject(packageId) == null) {
            println("Could not find a package for the given id `${packageId.toCoordinates()}`.")
            return 2
        }

        ortResult.collectLicenseFindings(packageId).forEach { (findings, _) ->
            if (licenseId == null || licenseId == findings.license) {
                println("--- ${findings.license} ---")
                println()
                findings.copyrights.forEach {
                    println("${it.statement}:")
                    it.locations.forEach {
                        println("  ${it.path}:${it.startLine}-${it.endLine}")
                    }
                }
            }
        }

        return 0
    }
}
