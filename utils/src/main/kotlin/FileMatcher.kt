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

package com.here.ort.utils

import com.here.ort.spdx.LICENSE_FILENAMES
import com.here.ort.spdx.ROOT_LICENSE_FILENAMES

import java.nio.file.FileSystems
import java.nio.file.InvalidPathException
import java.nio.file.Paths

/**
 * A class to determine whether a path is matched by any of the given globs.
 */
class FileMatcher(
    /**
     * The list of [glob patterns][1] to consider for matching.
     *
     * [1]: https://docs.oracle.com/javase/tutorial/essential/io/fileOps.html#glob
     */
    val patterns: List<String>
) {
    companion object {
        /**
         * A matcher which uses the default license file names.
         */
        val LICENSE_FILE_MATCHER = FileMatcher(LICENSE_FILENAMES + ROOT_LICENSE_FILENAMES)
    }

    constructor(vararg patterns: String) : this(patterns.asList())

    private val matchers = patterns.map {
        FileSystems.getDefault().getPathMatcher("glob:$it")
    }

    /**
     * Return true if and only if the given [path] is matched by any of the file globs passed to the
     * constructor.
     */
    fun matches(path: String): Boolean =
        try {
            matchers.any { it.matches(Paths.get(path)) }
        } catch (e: InvalidPathException) {
            false
        }
}
