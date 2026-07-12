package edu.ycp.cs.marmosetsubmitter.services

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File

class CMakeAssignmentInfoServiceTest : BasePlatformTestCase() {

    private lateinit var projectDir: File

    override fun setUp() {
        super.setUp()
        projectDir = File(project.basePath!!)
        projectDir.mkdirs()
    }

    override fun tearDown() {
        // clean up any files and subdirectories created during tests
        projectDir.walkTopDown()
            .filter { it != projectDir }
            .sortedDescending()     // delete files before their parent directories
            .forEach { it.delete() }
        super.tearDown()
    }

    private fun writeAssignmentInfoFile(filename: String, content: String) {
        val file = File(projectDir, filename)
        file.parentFile.mkdirs()
        file.writeText(content)
    }

    // ── Successful parsing ───────────────────────────────────────────────────

    fun testParseQuotedValues() {
        writeAssignmentInfoFile("assignment_info.cmake", """
            set(COURSE_NAME "CS 350")
            set(TERM "Fall")
            set(PROJECT_NUMBER "assign01")
        """.trimIndent())

        val info = CMakeAssignmentInfoService(project).parse(
            filename      = "assignment_info.cmake",
            submissionDir = projectDir
        )

        assertEquals("CS 350",   info.courseName)
        assertEquals("Fall",     info.term)
        assertEquals("assign01", info.projectNumber)
        assertEquals(projectDir, info.submissionDir)
    }

    fun testParseUnquotedValues() {
        writeAssignmentInfoFile("assignment_info.cmake", """
            set(COURSE_NAME CS350)
            set(TERM Fall)
            set(PROJECT_NUMBER assign01)
        """.trimIndent())

        val info = CMakeAssignmentInfoService(project).parse(
            filename      = "assignment_info.cmake",
            submissionDir = projectDir
        )

        assertEquals("CS350",    info.courseName)
        assertEquals("Fall",     info.term)
        assertEquals("assign01", info.projectNumber)
        assertEquals(projectDir, info.submissionDir)
    }

    fun testSemesterIsDerivedFromTermAndCurrentYear() {
        writeAssignmentInfoFile("assignment_info.cmake", """
            set(COURSE_NAME "CS 350")
            set(TERM "Fall")
            set(PROJECT_NUMBER "assign01")
        """.trimIndent())

        val info = CMakeAssignmentInfoService(project).parse(
            filename      = "assignment_info.cmake",
            submissionDir = projectDir
        )
        val currentYear = java.time.Year.now().toString()

        assertEquals("Fall $currentYear", info.semester)
    }

    fun testParseIgnoresUnrecognizedKeys() {
        writeAssignmentInfoFile("assignment_info.cmake", """
            set(COURSE_NAME "CS 350")
            set(TERM "Fall")
            set(PROJECT_NUMBER "assign01")
            set(PROJECT_NAME IntArrayStack)
            set(UNKNOWN_KEY "somevalue")
        """.trimIndent())

        val info = CMakeAssignmentInfoService(project).parse(
            filename      = "assignment_info.cmake",
            submissionDir = projectDir
        )

        assertEquals("CS 350",   info.courseName)
        assertEquals("Fall",     info.term)
        assertEquals("assign01", info.projectNumber)
    }

    fun testParseIgnoresCommentLines() {
        writeAssignmentInfoFile("assignment_info.cmake", """
            # This is a comment
            set(COURSE_NAME "CS 350")
            # Another comment
            set(TERM "Fall")
            set(PROJECT_NUMBER "assign01")
        """.trimIndent())

        val info = CMakeAssignmentInfoService(project).parse(
            filename      = "assignment_info.cmake",
            submissionDir = projectDir
        )

        assertEquals("CS 350",   info.courseName)
        assertEquals("Fall",     info.term)
        assertEquals("assign01", info.projectNumber)
    }

    // ── submissionDir is stored correctly ────────────────────────────────────

    fun testSubmissionDirIsProjectRootInModeOne() {
        writeAssignmentInfoFile("assignment_info.cmake", """
            set(COURSE_NAME "CS 350")
            set(TERM "Fall")
            set(PROJECT_NUMBER "assign01")
        """.trimIndent())

        val info = CMakeAssignmentInfoService(project).parse(
            filename      = "assignment_info.cmake",
            submissionDir = projectDir
        )

        assertEquals(projectDir, info.submissionDir)
    }

    fun testSubmissionDirIsSubdirectoryInModeTwo() {
        val subDir = File(projectDir, "CS370_Assign01_Fa25")
        subDir.mkdirs()

        writeAssignmentInfoFile("CS370_Assign01_Fa25/assignment_info.cmake", """
            set(COURSE_NAME "CS 370")
            set(TERM "Fall")
            set(PROJECT_NUMBER "assign01")
        """.trimIndent())

        val info = CMakeAssignmentInfoService(project).parse(
            filename      = "CS370_Assign01_Fa25/assignment_info.cmake",
            submissionDir = subDir
        )

        assertEquals(subDir,     info.submissionDir)
        assertEquals("CS 370",   info.courseName)
        assertEquals("assign01", info.projectNumber)
    }

    fun testSubmissionDirIsProjectRootWhenMappingHasNoSubdirectory() {
        writeAssignmentInfoFile("assign01_info.cmake", """
            set(COURSE_NAME "CS 370")
            set(TERM "Fall")
            set(PROJECT_NUMBER "assign01")
        """.trimIndent())

        val info = CMakeAssignmentInfoService(project).parse(
            filename      = "assign01_info.cmake",
            submissionDir = projectDir
        )

        // mapping file with no subdirectory component resolves to project root
        assertEquals(projectDir, info.submissionDir)
    }

    // ── useAssignmentInfoYear = false (default) ──────────────────────────────

    fun testParseUsesSystemYearByDefault() {
        writeAssignmentInfoFile("assignment_info.cmake", """
            set(COURSE_NAME "CS 350")
            set(TERM "Fall")
            set(PROJECT_NUMBER "assign01")
        """.trimIndent())

        val info = CMakeAssignmentInfoService(project).parse(
            filename      = "assignment_info.cmake",
            submissionDir = projectDir
        )
        val currentYear = java.time.Year.now().toString()

        assertEquals("Fall $currentYear", info.semester)
    }

    fun testParseIgnoresYearFieldWhenFlagIsFalse() {
        writeAssignmentInfoFile("assignment_info.cmake", """
            set(COURSE_NAME "CS 350")
            set(TERM "Fall")
            set(YEAR "2020")
            set(PROJECT_NUMBER "assign01")
        """.trimIndent())

        val info = CMakeAssignmentInfoService(project).parse(
            filename              = "assignment_info.cmake",
            useAssignmentInfoYear = false,
            submissionDir         = projectDir
        )
        val currentYear = java.time.Year.now().toString()

        assertEquals("Fall $currentYear", info.semester)
        assertFalse(info.semester.contains("2020"))
    }

    // ── useAssignmentInfoYear = true ─────────────────────────────────────────

    fun testParseReadsYearFromFileWhenFlagIsTrue() {
        writeAssignmentInfoFile("assignment_info.cmake", """
            set(COURSE_NAME "CS 350")
            set(TERM "Fall")
            set(YEAR "2026")
            set(PROJECT_NUMBER "assign01")
        """.trimIndent())

        val info = CMakeAssignmentInfoService(project).parse(
            filename              = "assignment_info.cmake",
            useAssignmentInfoYear = true,
            submissionDir         = projectDir
        )

        assertEquals("Fall 2026", info.semester)
    }

    fun testParseThrowsWhenYearMissingAndFlagIsTrue() {
        writeAssignmentInfoFile("assignment_info.cmake", """
            set(COURSE_NAME "CS 350")
            set(TERM "Fall")
            set(PROJECT_NUMBER "assign01")
        """.trimIndent())

        assertThrows(IllegalStateException::class.java) {
            CMakeAssignmentInfoService(project).parse(
                filename              = "assignment_info.cmake",
                useAssignmentInfoYear = true,
                submissionDir         = projectDir
            )
        }
    }

    fun testParseQuotedYearValue() {
        writeAssignmentInfoFile("assignment_info.cmake", """
            set(COURSE_NAME "CS 350")
            set(TERM "Spring")
            set(YEAR "2025")
            set(PROJECT_NUMBER "assign02")
        """.trimIndent())

        val info = CMakeAssignmentInfoService(project).parse(
            filename              = "assignment_info.cmake",
            useAssignmentInfoYear = true,
            submissionDir         = projectDir
        )

        assertEquals("Spring 2025", info.semester)
    }

    fun testParseUnquotedYearValue() {
        writeAssignmentInfoFile("assignment_info.cmake", """
            set(COURSE_NAME "CS 350")
            set(TERM "Fall")
            set(YEAR 2026)
            set(PROJECT_NUMBER "assign01")
        """.trimIndent())

        val info = CMakeAssignmentInfoService(project).parse(
            filename              = "assignment_info.cmake",
            useAssignmentInfoYear = true,
            submissionDir         = projectDir
        )

        assertEquals("Fall 2026", info.semester)
    }

    // ── Missing file ─────────────────────────────────────────────────────────

    fun testParseThrowsWhenFileNotFound() {
        assertThrows(IllegalStateException::class.java) {
            CMakeAssignmentInfoService(project).parse(
                filename      = "nonexistent.cmake",
                submissionDir = projectDir
            )
        }
    }

    fun testParseThrowsWhenFileInSubdirectoryNotFound() {
        assertThrows(IllegalStateException::class.java) {
            CMakeAssignmentInfoService(project).parse(
                filename      = "CS370_Assign01_Fa25/assignment_info.cmake",
                submissionDir = File(projectDir, "CS370_Assign01_Fa25")
            )
        }
    }

    // ── Missing required fields ──────────────────────────────────────────────

    fun testParseThrowsWhenCourseNameMissing() {
        writeAssignmentInfoFile("assignment_info.cmake", """
            set(TERM "Fall")
            set(PROJECT_NUMBER "assign01")
        """.trimIndent())

        assertThrows(IllegalStateException::class.java) {
            CMakeAssignmentInfoService(project).parse(
                filename      = "assignment_info.cmake",
                submissionDir = projectDir
            )
        }
    }

    fun testParseThrowsWhenTermMissing() {
        writeAssignmentInfoFile("assignment_info.cmake", """
            set(COURSE_NAME "CS 350")
            set(PROJECT_NUMBER "assign01")
        """.trimIndent())

        assertThrows(IllegalStateException::class.java) {
            CMakeAssignmentInfoService(project).parse(
                filename      = "assignment_info.cmake",
                submissionDir = projectDir
            )
        }
    }

    fun testParseThrowsWhenProjectNumberMissing() {
        writeAssignmentInfoFile("assignment_info.cmake", """
            set(COURSE_NAME "CS 350")
            set(TERM "Fall")
        """.trimIndent())

        assertThrows(IllegalStateException::class.java) {
            CMakeAssignmentInfoService(project).parse(
                filename      = "assignment_info.cmake",
                submissionDir = projectDir
            )
        }
    }

    fun testParseThrowsWhenFileIsEmpty() {
        writeAssignmentInfoFile("assignment_info.cmake", "")

        assertThrows(IllegalStateException::class.java) {
            CMakeAssignmentInfoService(project).parse(
                filename      = "assignment_info.cmake",
                submissionDir = projectDir
            )
        }
    }
}