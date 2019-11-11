/*
 * Copyright (C) 2019 Bosch Software Innovations GmbH
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

package com.here.ort.clearlydefined

import com.here.ort.clearlydefined.ClearlyDefinedService.ContributionInfo
import com.here.ort.clearlydefined.ClearlyDefinedService.ContributionPatch
import com.here.ort.clearlydefined.ClearlyDefinedService.ContributionSummary
import com.here.ort.clearlydefined.ClearlyDefinedService.Coordinates
import com.here.ort.clearlydefined.ClearlyDefinedService.Curation
import com.here.ort.clearlydefined.ClearlyDefinedService.Licensed
import com.here.ort.clearlydefined.ClearlyDefinedService.Patch
import com.here.ort.clearlydefined.ClearlyDefinedService.Provider
import com.here.ort.clearlydefined.ClearlyDefinedService.Server
import com.here.ort.clearlydefined.ClearlyDefinedService.Type
import com.here.ort.model.jsonMapper

import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer

import io.kotlintest.Spec
import io.kotlintest.matchers.numerics.shouldBeGreaterThan
import io.kotlintest.matchers.string.shouldNotContain
import io.kotlintest.matchers.string.shouldStartWith
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.specs.WordSpec

import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress

class ClearlyDefinedServiceTest : WordSpec() {
    private val loopback = InetAddress.getLoopbackAddress()
    private val port = 4000 // This is what ClearlyDefinedService.Service.LOCALHOST uses.

    private val handler = HttpHandler { exchange ->
        when (exchange.requestMethod) {
            "PATCH" -> {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0)
                val body = exchange.requestBody.reader().use { it.readText() }
                exchange.responseBody.writer().use { it.write(body) }
            }
        }
    }

    // Start the local HTTP server with the system default value for queued incoming connections.
    private val server = HttpServer.create(InetSocketAddress(loopback, port), 0).apply {
        createContext("/", handler)
        start()
    }

    override fun afterSpec(spec: Spec) {
        // Ensure the server is properly stopped even in case of exceptions, but wait at most 5 seconds.
        server.stop(5)

        super.afterSpec(spec)
    }

    init {
        "Uploading a contribution patch" should {
            val info = ContributionInfo(
                type = "type",
                summary = "summary",
                details = "details",
                resolution = "resolution",
                removedDefinitions = false
            )

            val revisions = mapOf(
                "6.2.3" to Curation(licensed = Licensed(declared = "Apache-1.0"))
            )

            val patch = Patch(
                Coordinates(
                    name = "platform-express",
                    namespace = "@nestjs",
                    provider = Provider.NPM_JS,
                    type = Type.NPM
                ),
                revisions
            )

            "only serlialize non-null values" {
                val service = ClearlyDefinedService.create(Server.LOCALHOST)

                val patchCall = service.putCuration(ContributionPatch(info, listOf(patch)))
                val response = patchCall.execute()
                val responseCode = response.code()
                val responseBody = response.body()

                responseCode shouldBe HttpURLConnection.HTTP_OK
                responseBody shouldNotBe null
                responseBody!!.toString() shouldNotContain "null"
            }

            // Disable this test by default as it talks to the real development instance of ClearlyDefined and creates
            // pull-requests at https://github.com/clearlydefined/curated-data-dev.
            "return a summary of the created pull-request".config(enabled = false) {
                val service = ClearlyDefinedService.create(Server.DEVELOPMENT)

                val patchCall = service.putCuration(ContributionPatch(info, listOf(patch)))
                val response = patchCall.execute()
                val responseCode = response.code()
                val responseBody = response.body()

                responseCode shouldBe HttpURLConnection.HTTP_OK
                responseBody shouldNotBe null

                val summary = jsonMapper.treeToValue(responseBody!!, ContributionSummary::class.java)
                summary.prNumber shouldBeGreaterThan 0
                summary.url shouldStartWith "https://github.com/clearlydefined/curated-data-dev/pull/"
            }
        }
    }
}
