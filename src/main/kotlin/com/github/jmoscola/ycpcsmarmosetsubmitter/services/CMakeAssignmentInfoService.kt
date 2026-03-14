package com.github.jmoscola.ycpcsmarmosetsubmitter.services

import com.intellij.openapi.project.Project
import java.io.File

data class AssignmentInfo(
    val courseName: String,
    val term: String,
    val projectNumber: String
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
            ?: error("Project base path not found")

        val cmakeFile = File(basePath, filename)

        if (!cmakeFile.exists()) {
            error("Assignment info file not found: $filename")
        }

        val properties = mutableMapOf<String, String>()

        cmakeFile.forEachLine { line ->
            val match = SET_REGEX.find(line) ?: return@forEachLine
            val (key, value) = match.destructured
            properties[key] = value.trim()
        }

        return AssignmentInfo(
            courseName    = properties["COURSE_NAME"]    ?: error("Missing COURSE_NAME in $filename"),
            term          = properties["TERM"]           ?: error("Missing TERM in $filename"),
            projectNumber = properties["PROJECT_NUMBER"] ?: error("Missing PROJECT_NUMBER in $filename")
        )
    }
}
