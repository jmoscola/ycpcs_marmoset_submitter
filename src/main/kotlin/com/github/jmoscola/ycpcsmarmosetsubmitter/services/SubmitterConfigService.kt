package com.github.jmoscola.ycpcsmarmosetsubmitter.services

import com.github.jmoscola.ycpcsmarmosetsubmitter.SubmitterBundle
import com.intellij.openapi.project.Project
import java.io.File
import java.util.Properties

data class ProjectConfig(
    val submissionUrl: String,            // required
    val assignmentInfoFilename: String,   // required
    val allowedExtensions: Set<String>?,  // null = allow all; emptySet = allow nothing
    val excludedFilenames: Set<String>,   // not required, can be emptySet
    val excludedDirectories: Set<String>, // not required, can be emptySet
    val excludedExtensions: Set<String>,  // not required, can be emptySet
    val zipFilenameSuffix: String         // not required, when not specified, a default val is assigned
)

class SubmitterConfigService(private val project: Project) {

    companion object {
        private val CONFIG_FILENAME = SubmitterBundle.message("submitterConfigService.configFilename")
        private val DEFAULT_ZIP_FILENAME_SUFFIX = SubmitterBundle.message("submitterConfigService.defaultZipFilenameSuffix")
    }

    /**
     * Loads and parses the ycpcs_marmoset_submitter.properties file from the project root.
     * @throws IllegalStateException if the config file is not found or required properties are missing.
     */
    fun load(): ProjectConfig {
        val basePath = project.basePath
            ?: error(SubmitterBundle.message("submitterConfigService.error.projectPathNotFound"))

        val configFile = File(basePath, CONFIG_FILENAME)

        if (!configFile.exists()) {
            error(SubmitterBundle.message("submitterConfigService.error.configFileNotFound", CONFIG_FILENAME))
        }

        val props = Properties()
        configFile.inputStream().use { props.load(it) }

        return ProjectConfig(
            submissionUrl          = props.require("submissionUrl"),
            assignmentInfoFilename = props.require("assignmentInfoFilename"),
            allowedExtensions      = props.parseSet("allowedExtensions"),    // null = allow all; emptySet = allow nothing
            excludedFilenames      = props.parseSet("excludedFilenames")     ?: emptySet(),
            excludedDirectories    = props.parseSet("excludedDirectories")   ?: emptySet(),
            excludedExtensions     = props.parseSet("excludedExtensions")    ?: emptySet(),
            zipFilenameSuffix      = props.getProperty("zipFilenameSuffix", DEFAULT_ZIP_FILENAME_SUFFIX)
        )
    }

    // Parses a comma-separated property into a Set<String>.
    // Returns null if the property is missing entirely, indicating "no restriction".
    private fun Properties.parseSet(key: String): Set<String>? {
        val value = getProperty(key) ?: return null
        return value.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    // Retrieves a required property, throwing a descriptive error if it is missing.
    private fun Properties.require(key: String): String =
        getProperty(key) ?: error(
            SubmitterBundle.message("submitterConfigService.error.requiredKeyNotFound",
                key,
                CONFIG_FILENAME
            )
        )
}
