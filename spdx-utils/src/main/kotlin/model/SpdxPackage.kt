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

package com.here.ort.spdx.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonRootName
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement

/**
 * Describes a software package
 */
@JacksonXmlRootElement(
    localName = "spdx:Package"
)
@JsonRootName(value = "Package")
data class SpdxPackage(
    /**
     * Name as given by package originator.
     * Cardinality: Mandatory, one.
     */
    @JacksonXmlProperty(
        isAttribute = false,
        namespace = "spdx"
    )
    @JsonProperty("PackageName")
    val name: String,

    /**
     * Identifier for the package.
     * Cardinality: Mandatory, one.
     */
    @JacksonXmlProperty(
      isAttribute = true,
      namespace = "rdf",
      localName = "ID"
    )
    @JsonProperty("SPDXID")
    val id: String,

    /**
     * Version of the package.
     * Cardinality: Optional, one.
     */
    @JacksonXmlProperty(
        isAttribute = false,
        namespace = "spdx",
        localName = "versionInfo"
    )
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("PackageVersion")
    val version: String? = null,

    /**
     * File name of the package, or path of the directory being treated as a package.
     * Cardinality: Optional, one.
     */
    @JacksonXmlProperty(
        isAttribute = false,
        namespace = "spdx",
        localName = "packageFileName"
    )
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("PackageFileName")
    val fileName: String? = null,

    /**
     * Distribution source for the package/directory identified.
     * Cardinality: Optional, one.
     */
    @JacksonXmlProperty(
        isAttribute = false,
        namespace = "spdx"
    )
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("PackageSupplier")
    val supplier: String? = null,

    /**
     * Identifies from where or whom the package originally came.
     * Cardinality: Optional, one.
     */
    @JacksonXmlProperty(
        isAttribute = false,
        namespace = "spdx"
    )
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("PackageOriginator")
    val originator: String? = null,

    /**
     * Indicates whether the file content of this package
     * has been available for or subjected to analysis
     * when the SPDX document was created.
     * Cardinality: Optional, one.
     */
    @JacksonXmlProperty(
        isAttribute = false,
        namespace = "spdx"
    )
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("FilesAnalyzed")
    val filesAnalyzed: Boolean = true

    ) : Comparable<SpdxPackage> {
    companion object {
        /**
         * A constant for a [SpdxPackage] where all properties are empty.
         */
        @JvmField
        val EMPTY = SpdxPackage(
            name = "",
            id = "",
            version = "",
            fileName = null,
            supplier = null,
            originator = null,
            filesAnalyzed = true
        )
    }

    /**
     * A comparison function to sort packages by their SPDX id.
     */
    override fun compareTo(other: SpdxPackage) = id.compareTo(other.id)
}
