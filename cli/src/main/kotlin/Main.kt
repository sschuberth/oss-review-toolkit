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

import com.beust.jcommander.DynamicParameter
import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter

import com.here.ort.commands.*
import com.here.ort.model.Environment
import com.here.ort.model.config.OrtConfiguration
import com.here.ort.utils.PARAMETER_ORDER_LOGGING
import com.here.ort.utils.PARAMETER_ORDER_OPTIONAL
import com.here.ort.utils.expandTilde
import com.here.ort.utils.printStackTrace

import com.typesafe.config.ConfigFactory

import io.github.config4k.extract

import java.io.File

import kotlin.system.exitProcess

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator

const val TOOL_NAME = "ort"

/**
 * The main entry point of the application.
 */
object Main : CommandWithHelp() {
    @Parameter(
        description = "The path to a configuration file.",
        names = ["--config", "-c"],
        order = PARAMETER_ORDER_OPTIONAL
    )
    private var configFile: File? = null

    @Parameter(
        description = "Enable info logging.",
        names = ["--info"],
        order = PARAMETER_ORDER_LOGGING
    )
    private var info = false

    @Parameter(
        description = "Enable debug logging and keep any temporary files.",
        names = ["--debug"],
        order = PARAMETER_ORDER_LOGGING
    )
    private var debug = false

    @Parameter(
        description = "Print out the stacktrace for all exceptions.",
        names = ["--stacktrace"],
        order = PARAMETER_ORDER_LOGGING
    )
    private var stacktrace = false

    @Parameter(
        description = "Show version information and exit.",
        names = ["--version", "-v"],
        order = PARAMETER_ORDER_OPTIONAL
    )
    private var version = false

    @DynamicParameter(
        description = "Allows to override configuration parameters.",
        names = ["-P"]
    )
    private var configArguments = mutableMapOf<String, String>()

    /**
     * The entry point for the application.
     *
     * @param args The list of application arguments.
     */
    @JvmStatic
    fun main(args: Array<String>) {
        exitProcess(run(args))
    }

    /**
     * Run the ORT CLI with the provided [args] and return the exit code of [CommandWithHelp.run].
     */
    fun run(args: Array<String>): Int {
        val jc = JCommander(this).apply {
            programName = TOOL_NAME
            setExpandAtSign(false)
            addCommand(AnalyzerCommand)
            addCommand(ClearlyDefinedUploadCommand)
            addCommand(DownloaderCommand)
            addCommand(EvaluatorCommand)
            addCommand(ReporterCommand)
            addCommand(RequirementsCommand)
            addCommand(ScannerCommand)
            parse(*args)
        }

        showVersionHeader(jc.parsedCommand)

        val config = loadConfig()

        return if (version) 0 else run(jc, config)
    }

    override fun runCommand(jc: JCommander, config: OrtConfiguration): Int {
        when {
            debug -> Configurator.setRootLevel(Level.DEBUG)
            info -> Configurator.setRootLevel(Level.INFO)
        }

        // Make the parameter globally available.
        printStackTrace = stacktrace

        // JCommander already validates the command names.
        val command = jc.commands[jc.parsedCommand]!!
        val commandObject = command.objects.first() as CommandWithHelp

        // Delegate running actions to the specified command.
        return commandObject.run(jc, config)
    }

    private fun showVersionHeader(commandName: String?) {
        val env = Environment()
        val variables = env.variables.entries.map { (key, value) -> "$key = $value" }

        val command = commandName?.let { " '$commandName'" }.orEmpty()
        val with = "with".takeUnless { variables.isEmpty() }.orEmpty()

        var variableIndex = 0

        """
            ________ _____________________
            \_____  \\______   \__    ___/ the OSS Review Toolkit, version ${env.ortVersion}.
             /   |   \|       _/ |    |    Running$command on Java ${env.javaVersion} and ${env.os} $with
            /    |    \    |   \ |    |    ${variables.getOrElse(variableIndex++) { "" }}
            \_______  /____|_  / |____|    ${variables.getOrElse(variableIndex++) { "" }}
                    \/       \/
        """.trimIndent().lines().forEach { println(it.trimEnd()) }

        val moreVariables = variables.drop(variableIndex)
        if (moreVariables.isNotEmpty()) {
            println("More environment variables:")
            moreVariables.forEach(::println)
        }

        println()
    }

    private fun loadConfig(): OrtConfiguration {
        val argsConfig = ConfigFactory.parseMap(configArguments, "Command line").withOnlyPath("ort")
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
