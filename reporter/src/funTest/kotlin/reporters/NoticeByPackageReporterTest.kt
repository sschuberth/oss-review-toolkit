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

package com.here.ort.reporter.reporters

import com.here.ort.model.OrtResult
import com.here.ort.model.config.CopyrightGarbage
import com.here.ort.model.config.FileArchiverConfiguration
import com.here.ort.model.config.FileStorageConfiguration
import com.here.ort.model.config.LocalFileStorageConfiguration
import com.here.ort.model.config.OrtConfiguration
import com.here.ort.model.config.ScannerConfiguration
import com.here.ort.spdx.LICENSE_FILENAMES
import com.here.ort.utils.test.readOrtResult

import io.kotlintest.shouldBe
import io.kotlintest.specs.WordSpec

import java.io.ByteArrayOutputStream
import java.io.File

private fun generateReport(
    ortResult: OrtResult,
    config: OrtConfiguration = OrtConfiguration(),
    copyrightGarbage: CopyrightGarbage = CopyrightGarbage(),
    postProcessingScript: String? = null
): String =
    ByteArrayOutputStream().also { outputStream ->
        NoticeByPackageReporter().generateReport(
            outputStream,
            ortResult,
            config,
            copyrightGarbage = copyrightGarbage,
            postProcessingScript = postProcessingScript
        )
    }.toString("UTF-8")

class NoticeByPackageReporterTest : WordSpec({
    "NoticeByPackageReporter" should {
        "generate the correct license notes" {
            val expectedText = File("src/funTest/assets/NPM-is-windows-1.0.2-expected-NOTICE_BY_PACKAGE").readText()
            val ortResult = readOrtResult("src/funTest/assets/NPM-is-windows-1.0.2-scan-result.json")

            val report = generateReport(ortResult)

            report shouldBe expectedText
        }

        "generate the correct license notes with archived license files" {
            val expectedText =
                File("src/funTest/assets/NPM-is-windows-1.0.2-expected-NOTICE_BY_PACKAGE_WITH_LICENSE_FILES").readText()
            val ortResult = readOrtResult("src/funTest/assets/NPM-is-windows-1.0.2-scan-result.json")

            val archiveDir = File("src/funTest/assets/NPM-is-windows-archive")
            val config = OrtConfiguration(
                ScannerConfiguration(
                    archive = FileArchiverConfiguration(
                        patterns = LICENSE_FILENAMES,
                        storage = FileStorageConfiguration(localFileStorage = LocalFileStorageConfiguration(archiveDir))
                    )
                )
            )
            val report = generateReport(ortResult, config)

            report shouldBe expectedText
        }
    }
})
