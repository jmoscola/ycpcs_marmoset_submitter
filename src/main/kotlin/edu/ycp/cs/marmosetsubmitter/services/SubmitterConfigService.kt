package edu.ycp.cs.marmosetsubmitter.services

import edu.ycp.cs.marmosetsubmitter.MarmosetSubmitterBundle
import com.intellij.openapi.project.Project
import java.io.File
import java.util.Properties

/**
 * Data class representing the parsed contents of the plugin configuration
 * file (marmoset_submitter.properties). Each instance contains all
 * settings required to control the submission workflow, including the
 * server URL, assignment info filename, zip file options, and file
 * exclusion rules.
 *
 * @property submissionUrl          The URL of the Marmoset submission server. Required.
 * @property assignmentInfoFilename The name of the CMake assignment info file
 *                                  in the project root directory. Required.
 * @property allowedFilenames       A whitelist of exact filenames to include in
 *                                  the submission zip file. A value of null indicates
 *                                  that all filenames are allowed. When set, only
 *                                  files whose names appear in this set will be
 *                                  included, regardless of extension. This is a
 *                                  more restrictive filter than [allowedExtensions].
 * @property allowedExtensions      A whitelist of file extensions to include in
 *                                  the submission zip file. A value of null indicates
 *                                  that all extensions are allowed. An empty set
 *                                  indicates that no files will be included.
 * @property excludedFilenames      A set of filenames to exclude from the submission
 *                                  zip file. Defaults to an empty set if not specified.
 * @property excludedDirectories    A set of directory names to exclude from the
 *                                  submission zip file. Neither the directory nor any
 *                                  of its contents will be included. Defaults to an
 *                                  empty set if not specified.
 * @property excludedExtensions     A set of file extensions to exclude from the
 *                                  submission zip file. Defaults to an empty set
 *                                  if not specified.
 * @property zipFilenameSuffix      The suffix appended to the project number to
 *                                  form the zip filename (e.g. "_submission" produces
 *                                  "assign01_submission.zip"). Defaults to "_submission"
 *                                  if not specified.
 */
data class ProjectConfig(
    val submissionUrl: String,            // required
    val assignmentInfoFilename: String,   // required
    val allowedFilenames: Set<String>?,   // null = no restriction; if set, allow only these filenames
    val allowedExtensions: Set<String>?,  // null = allow all; emptySet = allow nothing
    val excludedFilenames: Set<String>,   // not required, can be emptySet
    val excludedDirectories: Set<String>, // not required, can be emptySet
    val excludedExtensions: Set<String>,  // not required, can be emptySet
    val zipFilenameSuffix: String         // not required, when not specified, a default val is assigned
)

/**
 * Service that loads and parses the plugin configuration file
 * (ycpcs_marmoset_submitter.properties) from the root directory of the
 * current project. The configuration file controls all aspects of the
 * submission workflow, including the server URL, file exclusion rules,
 * and zip file naming.
 *
 * The expected format of the configuration file is as follows:
 * ```
 * submissionUrl=https://cs.ycp.edu/marmoset/bluej/SubmitProjectViaBlueJSubmitter
 * assignmentInfoFilename=CMakeLists.assignment_info.txt
 * allowedFilenames=main.cpp,main.h,Makefile
 * allowedExtensions=h,cpp
 * excludedFilenames=.DS_Store,Flags.h,tests.cpp
 * excludedDirectories=.git,.idea,build,out
 * excludedExtensions=o,d,a,iml,log,stackdump,exe,zip
 * zipFilenameSuffix=_submission
 * ```
 *
 * @param project The current IntelliJ project, used to resolve the project
 *                root directory.
 * @see ProjectConfig
 */
class SubmitterConfigService(private val project: Project) {

    companion object {
        private val CONFIG_FILENAME = MarmosetSubmitterBundle.message("submitterConfigService.configFilename")
        private val DEFAULT_ZIP_FILENAME_SUFFIX = MarmosetSubmitterBundle.message("submitterConfigService.defaultZipFilenameSuffix")
    }

    /**
     * Loads and parses the plugin configuration file from the project root
     * directory. Required properties are validated and an [IllegalStateException]
     * is thrown if any are absent. Optional properties fall back to sensible
     * defaults if not specified in the configuration file.
     *
     * Required properties:
     *   - submissionUrl
     *   - assignmentInfoFilename
     *
     * Optional properties (with defaults):
     *   - allowedFilenames   (default: null — allow all filenames)
     *   - allowedExtensions  (default: null — allow all extensions)
     *   - excludedFilenames  (default: empty — exclude nothing)
     *   - excludedDirectories (default: empty — exclude nothing)
     *   - excludedExtensions (default: empty — exclude nothing)
     *   - zipFilenameSuffix  (default: "_submission")
     *
     * @return A [ProjectConfig] containing the parsed configuration values.
     * @throws IllegalStateException if the project base path cannot be
     *         determined, if the configuration file does not exist, or if
     *         a required property is absent from the configuration file.
     */
    fun load(): ProjectConfig {
        val basePath = project.basePath
            ?: error(MarmosetSubmitterBundle.message("submitterConfigService.error.projectPathNotFound"))

        val configFile = File(basePath, CONFIG_FILENAME)

        if (!configFile.exists()) {
            error(MarmosetSubmitterBundle.message("submitterConfigService.error.configFileNotFound", CONFIG_FILENAME))
        }

        val props = Properties()
        configFile.inputStream().use { props.load(it) }

        return ProjectConfig(
            submissionUrl          = props.require("submissionUrl"),
            assignmentInfoFilename = props.require("assignmentInfoFilename"),
            allowedFilenames       = props.parseSet("allowedFilenames"),     // null = no restriction; if set, allow only these filenames
            allowedExtensions      = props.parseSet("allowedExtensions"),    // null = allow all; emptySet = allow nothing
            excludedExtensions     = props.parseSet("excludedExtensions")    ?: emptySet(),
            excludedFilenames      = props.parseSet("excludedFilenames")     ?: emptySet(),
            excludedDirectories    = props.parseSet("excludedDirectories")   ?: emptySet(),
            zipFilenameSuffix      = props.getProperty("zipFilenameSuffix", DEFAULT_ZIP_FILENAME_SUFFIX)
        )
    }

    /**
     * Parses a comma-separated property value into a [Set] of trimmed,
     * non-empty strings.
     *
     * @param key The property key to look up.
     * @return A [Set] of strings parsed from the comma-separated value, or
     *         null if the property is not present in the configuration file.
     *         A null return value indicates "no restriction" rather than
     *         "empty set", which would indicate "restrict everything".
     */
    private fun Properties.parseSet(key: String): Set<String>? {
        val value = getProperty(key) ?: return null
        return value.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    /**
     * Retrieves a required property value from the configuration file.
     *
     * @param key The property key to look up.
     * @return The value associated with the specified key.
     * @throws IllegalStateException if the property is not present in the
     *         configuration file.
     */
    private fun Properties.require(key: String): String =
        getProperty(key) ?: error(
            MarmosetSubmitterBundle.message("submitterConfigService.error.requiredKeyNotFound",
                key,
                CONFIG_FILENAME
            )
        )
}
