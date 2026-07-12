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
            assignmentInfoFilename=assignment_info.cmake
        """.trimIndent())

        val config = SubmitterConfigService(project).load()

        assertEquals("https://cs.ycp.edu/marmoset/submit", config.submissionUrl)
        assertEquals("assignment_info.cmake", config.assignmentInfoFilename)
    }

    fun testLoadThrowsWhenSubmissionUrlMissing() {
        writeConfig("""
            assignmentInfoFilename=assignment_info.cmake
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

    // ── assignmentInfoFilename - Mode 1 (direct file) ────────────────────────

    fun testAssignmentInfoFilenameDirectCmakeFile() {
        writeConfig("""
            submissionUrl=https://cs.ycp.edu/marmoset/submit
            assignmentInfoFilename=assignment_info.cmake
        """.trimIndent())

        val config = SubmitterConfigService(project).load()

        assertEquals("assignment_info.cmake", config.assignmentInfoFilename)
    }

    // ── assignmentInfoFilename - Mode 2 (mapping file) ───────────────────────

    fun testAssignmentInfoFilenameAssMappingFile() {
        writeConfig("""
            submissionUrl=https://cs.ycp.edu/marmoset/submit
            assignmentInfoFilename=assignment_info_mapping.cmake
            useRunConfigurationBasedSubmissions=true
        """.trimIndent())

        val config = SubmitterConfigService(project).load()

        assertEquals("assignment_info_mapping.cmake", config.assignmentInfoFilename)
    }

    // ── useRunConfigurationBasedSubmissions ──────────────────────────────────

    fun testUseRunConfigurationBasedSubmissionsDefaultsToFalseWhenOmitted() {
        writeConfig("""
            submissionUrl=https://cs.ycp.edu/marmoset/submit
            assignmentInfoFilename=assignment_info.cmake
        """.trimIndent())

        val config = SubmitterConfigService(project).load()

        assertFalse(config.useRunConfigurationBasedSubmissions)
    }

    fun testUseRunConfigurationBasedSubmissionsParsedTrueCorrectly() {
        writeConfig("""
            submissionUrl=https://cs.ycp.edu/marmoset/submit
            assignmentInfoFilename=assignment_info_mapping.cmake
            useRunConfigurationBasedSubmissions=true
        """.trimIndent())

        val config = SubmitterConfigService(project).load()

        assertTrue(config.useRunConfigurationBasedSubmissions)
    }

    fun testUseRunConfigurationBasedSubmissionsParsedFalseCorrectly() {
        writeConfig("""
            submissionUrl=https://cs.ycp.edu/marmoset/submit
            assignmentInfoFilename=assignment_info.cmake
            useRunConfigurationBasedSubmissions=false
        """.trimIndent())

        val config = SubmitterConfigService(project).load()

        assertFalse(config.useRunConfigurationBasedSubmissions)
    }

    fun testUseRunConfigurationBasedSubmissionsCaseInsensitive() {
        writeConfig("""
            submissionUrl=https://cs.ycp.edu/marmoset/submit
            assignmentInfoFilename=assignment_info_mapping.cmake
            useRunConfigurationBasedSubmissions=TRUE
        """.trimIndent())

        val config = SubmitterConfigService(project).load()

        assertTrue(config.useRunConfigurationBasedSubmissions)
    }

    fun testUseRunConfigurationBasedSubmissionsDefaultsToFalseForInvalidValue() {
        writeConfig("""
            submissionUrl=https://cs.ycp.edu/marmoset/submit
            assignmentInfoFilename=assignment_info.cmake
            useRunConfigurationBasedSubmissions=yes
        """.trimIndent())

        val config = SubmitterConfigService(project).load()

        // Kotlin's toBoolean() returns false for anything other than "true"
        assertFalse(config.useRunConfigurationBasedSubmissions)
    }

    fun testModeOneConfigLoadsCorrectlyWithoutRunConfigProperty() {
        // verify that a complete Mode 1 config works with no
        // useRunConfigurationBasedSubmissions property present
        writeConfig("""
            submissionUrl=https://cs.ycp.edu/marmoset/submit
            assignmentInfoFilename=assignment_info.cmake
            allowedExtensions=h,cpp
            excludedFilenames=.DS_Store,Flags.h,tests.cpp
            excludedDirectories=.git,.idea,build,out
            excludedExtensions=o,d,a,exe,zip
            zipFilenameSuffix=_submission
        """.trimIndent())

        val config = SubmitterConfigService(project).load()

        assertFalse(config.useRunConfigurationBasedSubmissions)
        assertEquals("assignment_info.cmake",               config.assignmentInfoFilename)
        assertEquals(setOf("h", "cpp"),                     config.allowedExtensions)
        assertEquals(setOf(".DS_Store", "Flags.h", "tests.cpp"), config.excludedFilenames)
        assertEquals(setOf(".git", ".idea", "build", "out"), config.excludedDirectories)
        assertEquals(setOf("o", "d", "a", "exe", "zip"),   config.excludedExtensions)
        assertEquals("_submission",                          config.zipFilenameSuffix)
    }

    fun testModeTwoConfigLoadsCorrectlyWithRunConfigProperty() {
        // verify that a complete Mode 2 config loads correctly
        writeConfig("""
            submissionUrl=https://cs.ycp.edu/marmoset/submit
            assignmentInfoFilename=assignment_info_mapping.cmake
            useRunConfigurationBasedSubmissions=true
            excludedDirectories=.git,.idea,build,out
            excludedExtensions=o,d,a,exe,zip
            zipFilenameSuffix=_submission
        """.trimIndent())

        val config = SubmitterConfigService(project).load()

        assertTrue(config.useRunConfigurationBasedSubmissions)
        assertEquals("assignment_info_mapping.cmake", config.assignmentInfoFilename)
        assertEquals(setOf(".git", ".idea", "build", "out"), config.excludedDirectories)
        assertEquals(setOf("o", "d", "a", "exe", "zip"),   config.excludedExtensions)
        assertEquals("_submission",                          config.zipFilenameSuffix)
    }

    // ── allowedExtensions ────────────────────────────────────────────────────

    fun testAllowedExtensionsNullWhenOmitted() {
        writeConfig("""
            submissionUrl=https://cs.ycp.edu/marmoset/submit
            assignmentInfoFilename=assignment_info.cmake
        """.trimIndent())

        val config = SubmitterConfigService(project).load()

        assertNull(config.allowedExtensions)
    }

    fun testAllowedExtensionsParsedCorrectly() {
        writeConfig("""
            submissionUrl=https://cs.ycp.edu/marmoset/submit
            assignmentInfoFilename=assignment_info.cmake
            allowedExtensions=h,cpp
        """.trimIndent())

        val config = SubmitterConfigService(project).load()

        assertEquals(setOf("h", "cpp"), config.allowedExtensions)
    }

    fun testAllowedExtensionsEmptySetWhenPresentButUnset() {
        writeConfig("""
            submissionUrl=https://cs.ycp.edu/marmoset/submit
            assignmentInfoFilename=assignment_info.cmake
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
            assignmentInfoFilename=assignment_info.cmake
        """.trimIndent())

        val config = SubmitterConfigService(project).load()

        assertNull(config.allowedFilenames)
    }

    fun testAllowedFilenamesParsedCorrectly() {
        writeConfig("""
            submissionUrl=https://cs.ycp.edu/marmoset/submit
            assignmentInfoFilename=assignment_info.cmake
            allowedFilenames=main.cpp,main.h,Makefile
        """.trimIndent())

        val config = SubmitterConfigService(project).load()

        assertEquals(setOf("main.cpp", "main.h", "Makefile"), config.allowedFilenames)
    }

    fun testAllowedFilenamesEmptySetWhenPresentButUnset() {
        writeConfig("""
            submissionUrl=https://cs.ycp.edu/marmoset/submit
            assignmentInfoFilename=assignment_info.cmake
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
            assignmentInfoFilename=assignment_info.cmake
        """.trimIndent())

        val config = SubmitterConfigService(project).load()

        assertTrue(config.excludedFilenames.isEmpty())
        assertTrue(config.excludedDirectories.isEmpty())
        assertTrue(config.excludedExtensions.isEmpty())
    }

    fun testExclusionSetsParsedCorrectly() {
        writeConfig("""
            submissionUrl=https://cs.ycp.edu/marmoset/submit
            assignmentInfoFilename=assignment_info.cmake
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
            assignmentInfoFilename=assignment_info.cmake
        """.trimIndent())

        val config = SubmitterConfigService(project).load()

        assertEquals("_submission", config.zipFilenameSuffix)
    }

    fun testZipFilenameSuffixParsedCorrectly() {
        writeConfig("""
            submissionUrl=https://cs.ycp.edu/marmoset/submit
            assignmentInfoFilename=assignment_info.cmake
            zipFilenameSuffix=_submit
        """.trimIndent())

        val config = SubmitterConfigService(project).load()

        assertEquals("_submit", config.zipFilenameSuffix)
    }

    // ── useAssignmentInfoYear ────────────────────────────────────────────────

    fun testUseAssignmentInfoYearDefaultsToFalseWhenOmitted() {
        writeConfig("""
            submissionUrl=https://cs.ycp.edu/marmoset/submit
            assignmentInfoFilename=assignment_info.cmake
        """.trimIndent())

        val config = SubmitterConfigService(project).load()

        assertFalse(config.useAssignmentInfoYear)
    }

    fun testUseAssignmentInfoYearParsedTrueCorrectly() {
        writeConfig("""
            submissionUrl=https://cs.ycp.edu/marmoset/submit
            assignmentInfoFilename=assignment_info.cmake
            useAssignmentInfoYear=true
        """.trimIndent())

        val config = SubmitterConfigService(project).load()

        assertTrue(config.useAssignmentInfoYear)
    }

    fun testUseAssignmentInfoYearParsedFalseCorrectly() {
        writeConfig("""
            submissionUrl=https://cs.ycp.edu/marmoset/submit
            assignmentInfoFilename=assignment_info.cmake
            useAssignmentInfoYear=false
        """.trimIndent())

        val config = SubmitterConfigService(project).load()

        assertFalse(config.useAssignmentInfoYear)
    }

    fun testUseAssignmentInfoYearCaseInsensitive() {
        writeConfig("""
            submissionUrl=https://cs.ycp.edu/marmoset/submit
            assignmentInfoFilename=assignment_info.cmake
            useAssignmentInfoYear=TRUE
        """.trimIndent())

        val config = SubmitterConfigService(project).load()

        assertTrue(config.useAssignmentInfoYear)
    }

    fun testUseAssignmentInfoYearDefaultsToFalseForInvalidValue() {
        writeConfig("""
            submissionUrl=https://cs.ycp.edu/marmoset/submit
            assignmentInfoFilename=assignment_info.cmake
            useAssignmentInfoYear=yes
        """.trimIndent())

        val config = SubmitterConfigService(project).load()

        // Kotlin's toBoolean() returns false for anything other than "true"
        assertFalse(config.useAssignmentInfoYear)
    }

    // ── Whitespace trimming ──────────────────────────────────────────────────

    fun testWhitespaceIsTrimmedFromSetValues() {
        writeConfig("""
            submissionUrl=https://cs.ycp.edu/marmoset/submit
            assignmentInfoFilename=assignment_info.cmake
            allowedExtensions= h , cpp , java 
        """.trimIndent())

        val config = SubmitterConfigService(project).load()

        assertEquals(setOf("h", "cpp", "java"), config.allowedExtensions)
    }
}