package edu.ycp.cs.marmosetsubmitter.services

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File

class SubmitterConfigServiceTest : BasePlatformTestCase() {

    private lateinit var projectDir: File
    private val configFilename = "marmoset_submitter.properties"

    override fun setUp() {
        super.setUp()
        projectDir = File(project.basePath!!)
        projectDir.mkdirs()
    }

    override fun tearDown() {
        File(projectDir, configFilename).delete()
        super.tearDown()
    }

    private fun writeConfig(content: String) {
        File(projectDir, configFilename).writeText(content)
    }

    // ── Required properties ──────────────────────────────────────────────────

    fun testLoadSucceedsWithAllRequiredProperties() {
        writeConfig("""
            submissionUrl=https://cs.ycp.edu/marmoset/submit
            assignmentInfoFilename=CMakeLists.assignment_info.txt
        """.trimIndent())

        val config = SubmitterConfigService(project).load()

        assertEquals("https://cs.ycp.edu/marmoset/submit", config.submissionUrl)
        assertEquals("CMakeLists.assignment_info.txt", config.assignmentInfoFilename)
    }

    fun testLoadThrowsWhenSubmissionUrlMissing() {
        writeConfig("""
            assignmentInfoFilename=CMakeLists.assignment_info.txt
        """.trimIndent())

        assertThrows(IllegalStateException::class.java) {
            SubmitterConfigService(project).load()
        }
    }

    fun testLoadThrowsWhenAssignmentInfoFilenameMissing() {
        writeConfig("""
            submissionUrl=https://cs.ycp.edu/marmoset/submit
        """.trimIndent())

        assertThrows(IllegalStateException::class.java) {
            SubmitterConfigService(project).load()
        }
    }

    fun testLoadThrowsWhenConfigFileMissing() {
        assertThrows(IllegalStateException::class.java) {
            SubmitterConfigService(project).load()
        }
    }

    // ── allowedExtensions ────────────────────────────────────────────────────

    fun testAllowedExtensionsNullWhenOmitted() {
        writeConfig("""
            submissionUrl=https://cs.ycp.edu/marmoset/submit
            assignmentInfoFilename=CMakeLists.assignment_info.txt
        """.trimIndent())

        val config = SubmitterConfigService(project).load()

        assertNull(config.allowedExtensions)
    }

    fun testAllowedExtensionsParsedCorrectly() {
        writeConfig("""
            submissionUrl=https://cs.ycp.edu/marmoset/submit
            assignmentInfoFilename=CMakeLists.assignment_info.txt
            allowedExtensions=h,cpp
        """.trimIndent())

        val config = SubmitterConfigService(project).load()

        assertEquals(setOf("h", "cpp"), config.allowedExtensions)
    }

    fun testAllowedExtensionsEmptySetWhenPresentButUnset() {
        writeConfig("""
            submissionUrl=https://cs.ycp.edu/marmoset/submit
            assignmentInfoFilename=CMakeLists.assignment_info.txt
            allowedExtensions=
        """.trimIndent())

        val config = SubmitterConfigService(project).load()

        assertNotNull(config.allowedExtensions)
        assertTrue(config.allowedExtensions!!.isEmpty())
    }

    // ── allowedFilenames ─────────────────────────────────────────────────────

    fun testAllowedFilenamesNullWhenOmitted() {
        writeConfig("""
            submissionUrl=https://cs.ycp.edu/marmoset/submit
            assignmentInfoFilename=CMakeLists.assignment_info.txt
        """.trimIndent())

        val config = SubmitterConfigService(project).load()

        assertNull(config.allowedFilenames)
    }

    fun testAllowedFilenamesParsedCorrectly() {
        writeConfig("""
            submissionUrl=https://cs.ycp.edu/marmoset/submit
            assignmentInfoFilename=CMakeLists.assignment_info.txt
            allowedFilenames=main.cpp,main.h,Makefile
        """.trimIndent())

        val config = SubmitterConfigService(project).load()

        assertEquals(setOf("main.cpp", "main.h", "Makefile"), config.allowedFilenames)
    }

    fun testAllowedFilenamesEmptySetWhenPresentButUnset() {
        writeConfig("""
            submissionUrl=https://cs.ycp.edu/marmoset/submit
            assignmentInfoFilename=CMakeLists.assignment_info.txt
            allowedFilenames=
        """.trimIndent())

        val config = SubmitterConfigService(project).load()

        assertNotNull(config.allowedFilenames)
        assertTrue(config.allowedFilenames!!.isEmpty())
    }

    // ── Exclusion sets ───────────────────────────────────────────────────────

    fun testExclusionSetsDefaultToEmptySetWhenOmitted() {
        writeConfig("""
            submissionUrl=https://cs.ycp.edu/marmoset/submit
            assignmentInfoFilename=CMakeLists.assignment_info.txt
        """.trimIndent())

        val config = SubmitterConfigService(project).load()

        assertTrue(config.excludedFilenames.isEmpty())
        assertTrue(config.excludedDirectories.isEmpty())
        assertTrue(config.excludedExtensions.isEmpty())
    }

    fun testExclusionSetsParsedCorrectly() {
        writeConfig("""
            submissionUrl=https://cs.ycp.edu/marmoset/submit
            assignmentInfoFilename=CMakeLists.assignment_info.txt
            excludedFilenames=.DS_Store,Flags.h
            excludedDirectories=.git,build
            excludedExtensions=o,d,exe
        """.trimIndent())

        val config = SubmitterConfigService(project).load()

        assertEquals(setOf(".DS_Store", "Flags.h"), config.excludedFilenames)
        assertEquals(setOf(".git", "build"),        config.excludedDirectories)
        assertEquals(setOf("o", "d", "exe"),        config.excludedExtensions)
    }

    // ── zipFilenameSuffix ────────────────────────────────────────────────────

    fun testZipFilenameSuffixDefaultsToSubmission() {
        writeConfig("""
            submissionUrl=https://cs.ycp.edu/marmoset/submit
            assignmentInfoFilename=CMakeLists.assignment_info.txt
        """.trimIndent())

        val config = SubmitterConfigService(project).load()

        assertEquals("_submission", config.zipFilenameSuffix)
    }

    fun testZipFilenameSuffixParsedCorrectly() {
        writeConfig("""
            submissionUrl=https://cs.ycp.edu/marmoset/submit
            assignmentInfoFilename=CMakeLists.assignment_info.txt
            zipFilenameSuffix=_submit
        """.trimIndent())

        val config = SubmitterConfigService(project).load()

        assertEquals("_submit", config.zipFilenameSuffix)
    }

    // ── Whitespace trimming ──────────────────────────────────────────────────

    fun testWhitespaceIsTrimmedFromSetValues() {
        writeConfig("""
            submissionUrl=https://cs.ycp.edu/marmoset/submit
            assignmentInfoFilename=CMakeLists.assignment_info.txt
            allowedExtensions= h , cpp , java 
        """.trimIndent())

        val config = SubmitterConfigService(project).load()

        assertEquals(setOf("h", "cpp", "java"), config.allowedExtensions)
    }
}