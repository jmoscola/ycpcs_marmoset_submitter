package edu.ycp.cs.marmosetsubmitter.services

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File

class AssignmentMappingServiceTest : BasePlatformTestCase() {

    private lateinit var projectDir: File

    override fun setUp() {
        super.setUp()
        projectDir = File(project.basePath!!)
        projectDir.mkdirs()
    }

    override fun tearDown() {
        projectDir.walkTopDown()
            .filter { it != projectDir }
            .sortedDescending()
            .forEach { it.delete() }
        super.tearDown()
    }

    private fun writeMappingFile(filename: String, content: String) {
        File(projectDir, filename).writeText(content)
    }

    // ── Successful resolution - subdirectory style ────────────────────────────

    fun testResolveSubdirectoryStyleMapping() {
        writeMappingFile("assignment_info_mapping.cmake", """
            set(DonQuixote      "CS370_Assign01_Fa25/assignment_info.cmake")
            set(RollinTrain_MS1 "CS370_Assign02_Fa25/assignment_info_ms1.cmake")
            set(RollinTrain_MS2 "CS370_Assign02_Fa25/assignment_info_ms2.cmake")
            set(LimeLight       "CS370_Assign03_Fa25/assignment_info.cmake")
        """.trimIndent())

        val result = AssignmentMappingService(project).resolve(
            "assignment_info_mapping.cmake",
            "DonQuixote"
        )

        assertEquals("CS370_Assign01_Fa25/assignment_info.cmake", result)
    }

    fun testResolveSubdirectoryStyleMappingSecondEntry() {
        writeMappingFile("assignment_info_mapping.cmake", """
            set(DonQuixote      "CS370_Assign01_Fa25/assignment_info.cmake")
            set(RollinTrain_MS1 "CS370_Assign02_Fa25/assignment_info_ms1.cmake")
            set(RollinTrain_MS2 "CS370_Assign02_Fa25/assignment_info_ms2.cmake")
            set(LimeLight       "CS370_Assign03_Fa25/assignment_info.cmake")
        """.trimIndent())

        val result = AssignmentMappingService(project).resolve(
            "assignment_info_mapping.cmake",
            "RollinTrain_MS1"
        )

        assertEquals("CS370_Assign02_Fa25/assignment_info_ms1.cmake", result)
    }

    // ── Successful resolution - project root style ────────────────────────────

    fun testResolveProjectRootStyleMapping() {
        writeMappingFile("assignment_info_mapping.cmake", """
            set(DonQuixote      "assign01_info.cmake")
            set(RollinTrain_MS1 "assign02_info_ms1.cmake")
            set(RollinTrain_MS2 "assign02_info_ms2.cmake")
            set(LimeLight       "assign03_info.cmake")
        """.trimIndent())

        val result = AssignmentMappingService(project).resolve(
            "assignment_info_mapping.cmake",
            "DonQuixote"
        )

        assertEquals("assign01_info.cmake", result)
    }

    fun testResolveProjectRootStyleMappingThirdEntry() {
        writeMappingFile("assignment_info_mapping.cmake", """
            set(DonQuixote      "assign01_info.cmake")
            set(RollinTrain_MS1 "assign02_info_ms1.cmake")
            set(RollinTrain_MS2 "assign02_info_ms2.cmake")
            set(LimeLight       "assign03_info.cmake")
        """.trimIndent())

        val result = AssignmentMappingService(project).resolve(
            "assignment_info_mapping.cmake",
            "RollinTrain_MS2"
        )

        assertEquals("assign02_info_ms2.cmake", result)
    }

    fun testResolveProjectRootStyleMappingFourthEntry() {
        writeMappingFile("assignment_info_mapping.cmake", """
            set(DonQuixote      "assign01_info.cmake")
            set(RollinTrain_MS1 "assign02_info_ms1.cmake")
            set(RollinTrain_MS2 "assign02_info_ms2.cmake")
            set(LimeLight       "assign03_info.cmake")
        """.trimIndent())

        val result = AssignmentMappingService(project).resolve(
            "assignment_info_mapping.cmake",
            "LimeLight"
        )

        assertEquals("assign03_info.cmake", result)
    }

    // ── Successful resolution - mixed styles ──────────────────────────────────

    fun testResolveMixedStyleMapping() {
        writeMappingFile("assignment_info_mapping.cmake", """
            set(DonQuixote      "CS370_Assign01_Fa25/assignment_info.cmake")
            set(RollinTrain_MS1 "assign02_info_ms1.cmake")
            set(RollinTrain_MS2 "assign02_info_ms2.cmake")
        """.trimIndent())

        val subdirResult = AssignmentMappingService(project).resolve(
            "assignment_info_mapping.cmake",
            "DonQuixote"
        )
        val rootResult = AssignmentMappingService(project).resolve(
            "assignment_info_mapping.cmake",
            "RollinTrain_MS1"
        )

        assertEquals("CS370_Assign01_Fa25/assignment_info.cmake", subdirResult)
        assertEquals("assign02_info_ms1.cmake",                        rootResult)
    }

    // ── Quoted and unquoted values ────────────────────────────────────────────

    fun testResolveQuotedValue() {
        writeMappingFile("assignment_info_mapping.cmake", """
            set(DonQuixote "CS370_Assign01_Fa25/assignment_info.cmake")
        """.trimIndent())

        val result = AssignmentMappingService(project).resolve(
            "assignment_info_mapping.cmake",
            "DonQuixote"
        )

        assertEquals("CS370_Assign01_Fa25/assignment_info.cmake", result)
    }

    fun testResolveUnquotedValue() {
        writeMappingFile("assignment_info_mapping.cmake", """
            set(DonQuixote CS370_Assign01_Fa25/assignment_info.cmake)
        """.trimIndent())

        val result = AssignmentMappingService(project).resolve(
            "assignment_info_mapping.cmake",
            "DonQuixote"
        )

        assertEquals("CS370_Assign01_Fa25/assignment_info.cmake", result)
    }

    // ── Comment lines are ignored ─────────────────────────────────────────────

    fun testResolveIgnoresCommentLines() {
        writeMappingFile("assignment_info_mapping.cmake", """
            ############################################################
            # Mappings from:
            # Run Configuration names -> Assignment specific info files
            ############################################################
            set(DonQuixote      "CS370_Assign01_Fa25/assignment_info.cmake")
            set(RollinTrain_MS1 "CS370_Assign02_Fa25/assignment_info_ms1.cmake")
            set(RollinTrain_MS2 "CS370_Assign02_Fa25/assignment_info_ms2.cmake")
        """.trimIndent())

        val result = AssignmentMappingService(project).resolve(
            "assignment_info_mapping.cmake",
            "DonQuixote"
        )

        assertEquals("CS370_Assign01_Fa25/assignment_info.cmake", result)
    }

    // ── Missing mapping file ──────────────────────────────────────────────────

    fun testResolveThrowsWhenMappingFileNotFound() {
        assertThrows(IllegalStateException::class.java) {
            AssignmentMappingService(project).resolve(
                "nonexistent_mapping.cmake",
                "DonQuixote"
            )
        }
    }

    // ── Run configuration name not found ─────────────────────────────────────

    fun testResolveThrowsWhenRunConfigNotFound() {
        writeMappingFile("assignment_info_mapping.cmake", """
            set(DonQuixote      "CS370_Assign01_Fa25/assignment_info.cmake")
            set(RollinTrain_MS1 "CS370_Assign02_Fa25/assignment_info_ms1.cmake")
            set(RollinTrain_MS2 "CS370_Assign02_Fa25/assignment_info_ms2.cmake")
        """.trimIndent())

        assertThrows(IllegalStateException::class.java) {
            AssignmentMappingService(project).resolve(
                "assignment_info_mapping.cmake",
                "NonExistentRunConfig"
            )
        }
    }

    fun testResolveThrowsWhenRunConfigNameIsCaseSensitive() {
        writeMappingFile("assignment_info_mapping.cmake", """
            set(DonQuixote "CS370_Assign01_Fa25/assignment_info.cmake")
        """.trimIndent())

        // run config names are case-sensitive - "donquixote" should not match "DonQuixote"
        assertThrows(IllegalStateException::class.java) {
            AssignmentMappingService(project).resolve(
                "assignment_info_mapping.cmake",
                "donquixote"
            )
        }
    }

    fun testResolveErrorMessageContainsRunConfigName() {
        writeMappingFile("assignment_info_mapping.cmake", """
        set(DonQuixote "CS370_Assign01_Fa25/assignment_info.cmake")
    """.trimIndent())

        var exception: IllegalStateException? = null
        try {
            AssignmentMappingService(project).resolve(
                "assignment_info_mapping.cmake",
                "NonExistentRunConfig"
            )
        } catch (e: IllegalStateException) {
            exception = e
        }

        assertNotNull("Expected IllegalStateException to be thrown", exception)
        assertTrue(exception!!.message?.contains("NonExistentRunConfig") == true)
    }

    fun testResolveErrorMessageContainsMappingFilename() {
        writeMappingFile("assignment_info_mapping.cmake", """
        set(DonQuixote "CS370_Assign01_Fa25/assignment_info.cmake")
    """.trimIndent())

        var exception: IllegalStateException? = null
        try {
            AssignmentMappingService(project).resolve(
                "assignment_info_mapping.cmake",
                "NonExistentRunConfig"
            )
        } catch (e: IllegalStateException) {
            exception = e
        }

        assertNotNull("Expected IllegalStateException to be thrown", exception)
        assertTrue(exception!!.message?.contains("assignment_info_mapping.cmake") == true)
    }

    // ── Empty mapping file ────────────────────────────────────────────────────

    fun testResolveThrowsWhenMappingFileIsEmpty() {
        writeMappingFile("assignment_info_mapping.cmake", "")

        assertThrows(IllegalStateException::class.java) {
            AssignmentMappingService(project).resolve(
                "assignment_info_mapping.cmake",
                "DonQuixote"
            )
        }
    }

    // ── Large mapping file ────────────────────────────────────────────────────

    fun testResolveLargeMappingFile() {
        val entries = (1..20).joinToString("\n") { i ->
            """set(Assignment${i.toString().padStart(2, '0')} "CS370_Assign${i.toString().padStart(2, '0')}_Fa25/assignment_info.cmake")"""
        }
        writeMappingFile("assignment_info_mapping.cmake", entries)

        val result = AssignmentMappingService(project).resolve(
            "assignment_info_mapping.cmake",
            "Assignment15"
        )

        assertEquals("CS370_Assign15_Fa25/assignment_info.cmake", result)
    }
}