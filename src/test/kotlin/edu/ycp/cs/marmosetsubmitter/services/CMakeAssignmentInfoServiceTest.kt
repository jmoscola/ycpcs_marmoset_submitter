package edu.ycp.cs.marmosetsubmitter.services

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File

class CMakeAssignmentInfoServiceTest : BasePlatformTestCase() {

    private lateinit var projectDir: File

    override fun setUp() {
        super.setUp()
        projectDir = File(project.basePath!!)
        projectDir.mkdirs() // ensure the directory exists on disk
    }

    private fun writeCMakeFile(filename: String, content: String) {
        File(projectDir, filename).writeText(content)
    }

    // ── Successful parsing ───────────────────────────────────────────────────

    fun testParseQuotedValues() {
        writeCMakeFile("CMakeLists.assignment_info.txt", """
            set(COURSE_NAME "CS 350")
            set(TERM "Fall")
            set(PROJECT_NUMBER "assign01")
        """.trimIndent())

        val info = CMakeAssignmentInfoService(project).parse("CMakeLists.assignment_info.txt")

        assertEquals("CS 350",   info.courseName)
        assertEquals("Fall",     info.term)
        assertEquals("assign01", info.projectNumber)
    }

    fun testParseUnquotedValues() {
        writeCMakeFile("CMakeLists.assignment_info.txt", """
            set(COURSE_NAME CS350)
            set(TERM Fall)
            set(PROJECT_NUMBER assign01)
        """.trimIndent())

        val info = CMakeAssignmentInfoService(project).parse("CMakeLists.assignment_info.txt")

        assertEquals("CS350",    info.courseName)
        assertEquals("Fall",     info.term)
        assertEquals("assign01", info.projectNumber)
    }

    fun testSemesterIsDerivedFromTermAndCurrentYear() {
        writeCMakeFile("CMakeLists.assignment_info.txt", """
            set(COURSE_NAME "CS 350")
            set(TERM "Fall")
            set(PROJECT_NUMBER "assign01")
        """.trimIndent())

        val info = CMakeAssignmentInfoService(project).parse("CMakeLists.assignment_info.txt")
        val currentYear = java.time.Year.now().toString()

        assertEquals("Fall $currentYear", info.semester)
    }

    fun testParseIgnoresUnrecognizedKeys() {
        writeCMakeFile("CMakeLists.assignment_info.txt", """
            set(COURSE_NAME "CS 350")
            set(TERM "Fall")
            set(PROJECT_NUMBER "assign01")
            set(PROJECT_NAME_STR IntArrayStack)
            set(UNKNOWN_KEY "somevalue")
        """.trimIndent())

        val info = CMakeAssignmentInfoService(project).parse("CMakeLists.assignment_info.txt")

        assertEquals("CS 350",   info.courseName)
        assertEquals("Fall",     info.term)
        assertEquals("assign01", info.projectNumber)
    }

    fun testParseIgnoresCommentLines() {
        writeCMakeFile("CMakeLists.assignment_info.txt", """
            # This is a comment
            set(COURSE_NAME "CS 350")
            # Another comment
            set(TERM "Fall")
            set(PROJECT_NUMBER "assign01")
        """.trimIndent())

        val info = CMakeAssignmentInfoService(project).parse("CMakeLists.assignment_info.txt")

        assertEquals("CS 350",   info.courseName)
        assertEquals("Fall",     info.term)
        assertEquals("assign01", info.projectNumber)
    }

    // ── Missing file ─────────────────────────────────────────────────────────

    fun testParseThrowsWhenFileNotFound() {
        assertThrows(IllegalStateException::class.java) {
            CMakeAssignmentInfoService(project).parse("nonexistent.txt")
        }
    }

    // ── Missing required fields ──────────────────────────────────────────────

    fun testParseThrowsWhenCourseNameMissing() {
        writeCMakeFile("CMakeLists.assignment_info.txt", """
            set(TERM "Fall")
            set(PROJECT_NUMBER "assign01")
        """.trimIndent())

        assertThrows(IllegalStateException::class.java) {
            CMakeAssignmentInfoService(project).parse("CMakeLists.assignment_info.txt")
        }
    }

    fun testParseThrowsWhenTermMissing() {
        writeCMakeFile("CMakeLists.assignment_info.txt", """
            set(COURSE_NAME "CS 350")
            set(PROJECT_NUMBER "assign01")
        """.trimIndent())

        assertThrows(IllegalStateException::class.java) {
            CMakeAssignmentInfoService(project).parse("CMakeLists.assignment_info.txt")
        }
    }

    fun testParseThrowsWhenProjectNumberMissing() {
        writeCMakeFile("CMakeLists.assignment_info.txt", """
            set(COURSE_NAME "CS 350")
            set(TERM "Fall")
        """.trimIndent())

        assertThrows(IllegalStateException::class.java) {
            CMakeAssignmentInfoService(project).parse("CMakeLists.assignment_info.txt")
        }
    }

    fun testParseThrowsWhenFileIsEmpty() {
        writeCMakeFile("CMakeLists.assignment_info.txt", "")

        assertThrows(IllegalStateException::class.java) {
            CMakeAssignmentInfoService(project).parse("CMakeLists.assignment_info.txt")
        }
    }
}