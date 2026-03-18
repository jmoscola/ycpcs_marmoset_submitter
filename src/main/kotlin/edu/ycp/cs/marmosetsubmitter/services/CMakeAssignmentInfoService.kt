package edu.ycp.cs.marmosetsubmitter.services

import edu.ycp.cs.marmosetsubmitter.MarmosetSubmitterBundle
import com.intellij.openapi.project.Project
import java.io.File

/**
 * Data class representing the assignment information parsed from a CMake
 * assignment info file. The semester field is derived automatically from
 * the term and the current year and does not need to be present in the
 * CMake file.
 *
 * @property courseName    The name of the course (e.g. "CS 350").
 * @property term          The academic term (e.g. "Fall").
 * @property projectNumber The assignment identifier (the name field in Marmoset) (e.g. "assign01").
 * @property semester      The full semester string derived from the term and
 *                         the current year (e.g. "Fall 2026").
 */
data class AssignmentInfo(
    val courseName: String,
    val term: String,
    val projectNumber: String,
    val semester: String
)

/**
 * Service that locates and parses a CMake assignment info file in the project
 * root directory. The assignment info file uses CMake [set()] syntax to define
 * the course name, term, and project number required for submission to the
 * Marmoset server.
 *
 * The expected format of the assignment info file is as follows:
 * ```
 * set(COURSE_NAME "CS 350")
 * set(TERM "Fall")
 * set(PROJECT_NUMBER "assign01")
 * ```
 *
 * @param project The current IntelliJ project, used to resolve the project
 *                root directory.
 * @see AssignmentInfo
 */
class CMakeAssignmentInfoService(private val project: Project) {

    /**
     * Contains the regular expression used to parse CMake [set()] commands
     * from the assignment info file. The pattern matches both quoted and
     * unquoted values:
     * ```
     * set(COURSE_NAME "CS 350")   // quoted
     * set(PROJECT_NUMBER assign01) // unquoted
     * ```
     * Capture group 1 contains the key (e.g. "COURSE_NAME").
     * Capture group 2 contains the value (e.g. "CS 350").
     */
    companion object {
        // Matches:  set(KEY "value")  or  set(KEY value)
        private val SET_REGEX = Regex("""^\s*set\(\s*(\w+)\s+"?([^")]+)"?\s*\)""")
    }

    /**
     * Locates and parses the specified CMake assignment info file in the
     * project root directory. Extracts the COURSE_NAME, TERM, and
     * PROJECT_NUMBER fields and returns them as an [AssignmentInfo] instance.
     * The semester field is derived automatically from the parsed term and
     * the current year.
     *
     * Both quoted and unquoted CMake values are supported:
     * ```
     * set(COURSE_NAME "CS 350")   // quoted
     * set(PROJECT_NUMBER assign01) // unquoted
     * ```
     *
     * @param filename The name of the CMake assignment info file to parse,
     *                 relative to the project root directory.
     * @return An [AssignmentInfo] containing the parsed course name, term,
     *         project number, and derived semester.
     * @throws IllegalStateException if the project base path cannot be
     *         determined, if the file does not exist, or if any required
     *         field is absent from the file. Note that Kotlin's [error]
     *         function is used to throw this exception.
     */
    fun parse(filename: String): AssignmentInfo {
        val basePath = project.basePath
            ?: error(
                MarmosetSubmitterBundle.message("cmakeAssignmentInfoService.error.projectPathNotFound")
            )

        val cmakeFile = File(basePath, filename)

        if (!cmakeFile.exists()) {
            error(
                MarmosetSubmitterBundle.message("cmakeAssignmentInfoService.error.assignmentInfoFileNotFound", filename)
            )
        }

        val properties = mutableMapOf<String, String>()

        cmakeFile.forEachLine { line ->
            val match = SET_REGEX.find(line) ?: return@forEachLine
            val (key, value) = match.destructured
            properties[key] = value.trim()
        }

        return AssignmentInfo(
            courseName    = properties["COURSE_NAME"]    ?: error(MarmosetSubmitterBundle.message("cmakeAssignmentInfoService.error.missingCourseName", filename)),
            term          = properties["TERM"]           ?: error(MarmosetSubmitterBundle.message("cmakeAssignmentInfoService.error.missingTerm", filename)),
            projectNumber = properties["PROJECT_NUMBER"] ?: error(MarmosetSubmitterBundle.message("cmakeAssignmentInfoService.error.missingProjectNumber", filename)),
            semester      = "${properties["TERM"]} ${java.time.Year.now()}"
        )
    }
}
