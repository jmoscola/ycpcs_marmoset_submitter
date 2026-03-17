package com.github.jmoscola.ycpcsmarmosetsubmitter.services

import com.github.jmoscola.ycpcsmarmosetsubmitter.SubmitterBundle
import com.intellij.openapi.project.Project
import java.io.File

data class AssignmentInfo(
    val courseName: String,
    val term: String,
    val projectNumber: String,
    val semester: String
)

class CMakeAssignmentInfoService(private val project: Project) {

    companion object {
        // Matches:  set(KEY "value")  or  set(KEY value)
        private val SET_REGEX = Regex("""^\s*set\(\s*(\w+)\s+"?([^")]+)"?\s*\)""")
    }

    /**
     * Finds and parses the CMake assignment info file in the project root.
     * @param filename The name of the CMake assignment info file to parse.
     * @throws IllegalStateException if the file is missing or required fields are absent.
     */
    fun parse(filename: String): AssignmentInfo {
        val basePath = project.basePath
            ?: error(
                SubmitterBundle.message("cmakeAssignmentInfoService.error.projectPathNotFound")
            )

        val cmakeFile = File(basePath, filename)

        if (!cmakeFile.exists()) {
            error(
                SubmitterBundle.message("cmakeAssignmentInfoService.error.assignmentInfoFileNotFound", filename)
            )
        }

        val properties = mutableMapOf<String, String>()

        cmakeFile.forEachLine { line ->
            val match = SET_REGEX.find(line) ?: return@forEachLine
            val (key, value) = match.destructured
            properties[key] = value.trim()
        }

        return AssignmentInfo(
            courseName    = properties["COURSE_NAME"]    ?: error(SubmitterBundle.message("cmakeAssignmentInfoService.error.missingCourseName", filename)),
            term          = properties["TERM"]           ?: error(SubmitterBundle.message("cmakeAssignmentInfoService.error.missingTerm", filename)),
            projectNumber = properties["PROJECT_NUMBER"] ?: error(SubmitterBundle.message("cmakeAssignmentInfoService.error.missingProjectNumber", filename)),
            semester      = "${properties["TERM"]} ${java.time.Year.now()}"
        )
    }
}
