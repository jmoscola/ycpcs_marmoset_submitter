package edu.ycp.cs.marmosetsubmitter.services

import edu.ycp.cs.marmosetsubmitter.MarmosetSubmitterBundle
import com.intellij.openapi.project.Project
import java.io.File

/**
 * Service that parses the assignment mapping file and resolves the
 * assignment info file path for a given run configuration name. The
 * mapping file maps JetBrains Run Configuration names to their
 * corresponding assignment info file paths using CMake [set()] syntax.
 *
 * The assignment info files referenced in the mapping may be located
 * anywhere relative to the project root, either in subdirectories
 * of the project (one per assignment) or all in the project root
 * directory itself. Both of the following mapping styles are supported:
 *
 * Subdirectory style:
 * ```
 * set(DonQuixote      "CS370_Assign01_Fa25/assignment_info.cmake")
 * set(RollinTrain_MS1 "CS370_Assign02_Fa25/assignment_info_ms1.cmake")
 * ```
 *
 * Project root style:
 * ```
 * set(DonQuixote      "assign01_info.cmake")
 * set(RollinTrain_MS1 "assign02_info_ms1.cmake")
 * ```
 *
 * @param project The current project.
 */
class AssignmentMappingService(private val project: Project) {

    /**
     * Contains the regular expression used to parse CMake [set()] commands
     * from the mapping file. Reuses the same pattern as
     * [CMakeAssignmentInfoService] since both files use CMake set() syntax.
     * Capture group 1 contains the run configuration name.
     * Capture group 2 contains the assignment info file path.
     */
    companion object {
        // Matches:  set(KEY "value")  or  set(KEY value)
        private val SET_REGEX = Regex("""^\s*set\(\s*(\w+)\s+"?([^")]+)"?\s*\)""")
    }

    /**
     * Parses the assignment info mapping file and resolves the assignment info
     * file path for the specified run configuration name. The returned path
     * is relative to the project root directory and may include subdirectory
     * path components (e.g. "CS370_Assign01_Fa25/assignment_info.cmake") or
     * may be a filename in the project root (e.g. "assign01_info.cmake"),
     * depending on how the mapping file is structured.
     *
     * @param mappingFilename The name of the mapping file to parse, relative
     *                        to the project root directory.
     * @param runConfigName   The name of the currently selected run
     *                        configuration to look up in the mapping.
     * @return The path to the assignment info file for the given run
     *         configuration, relative to the project root directory.
     * @throws IllegalStateException if the project base path cannot be
     *         determined, if the mapping file does not exist, or if the
     *         run configuration name is not found in the mapping file.
     */
    fun resolve(mappingFilename: String, runConfigName: String): String {
        val basePath = project.basePath
            ?: error(MarmosetSubmitterBundle.message(
                "assignmentMappingService.error.projectPathNotFound"))

        val mappingFile = File(basePath, mappingFilename)

        if (!mappingFile.exists()) {
            error(MarmosetSubmitterBundle.message(
                "assignmentMappingService.error.assignmentMappingFileNotFound", mappingFilename))
        }

        val mappings = mutableMapOf<String, String>()

        mappingFile.forEachLine { line ->
            val match = SET_REGEX.find(line) ?: return@forEachLine
            val (key, value) = match.destructured
            mappings[key] = value.trim()
        }

        return mappings[runConfigName]
            ?: error(MarmosetSubmitterBundle.message(
                "assignmentMappingService.error.runConfigNotFound",
                runConfigName,
                mappingFilename))
    }
}