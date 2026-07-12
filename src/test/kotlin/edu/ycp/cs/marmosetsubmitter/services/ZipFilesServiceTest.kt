package edu.ycp.cs.marmosetsubmitter.services

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File
import java.util.zip.ZipFile

class ZipFilesServiceTest : BasePlatformTestCase() {

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

    private fun createFile(relativePath: String, content: String = "test content") {
        val file = File(projectDir, relativePath)
        file.parentFile.mkdirs()
        file.writeText(content)
    }

    private fun zipEntryNames(zipFile: File): Set<String> {
        return ZipFile(zipFile).use { zip ->
            zip.entries().asSequence().map { it.name }.toSet()
        }
    }

    // ── Basic zipping - Mode 1 (project root) ────────────────────────────────

    fun testZipProjectCreatesZipFile() {
        createFile("main.cpp")
        createFile("main.h")

        val zipFile = ZipFilesService(project).zipProject(
            zipFilename         = "test.zip",
            excludedExtensions  = emptySet(),
            excludedFilenames   = emptySet(),
            excludedDirectories = emptySet(),
            submissionDir       = projectDir
        )

        assertTrue(zipFile.exists())
        assertTrue(zipFile.parentFile == projectDir)
    }

    fun testZipProjectIncludesAllFilesWhenNoFiltersSet() {
        createFile("main.cpp")
        createFile("main.h")
        createFile("README.md")

        val zipFile = ZipFilesService(project).zipProject(
            zipFilename         = "test.zip",
            excludedExtensions  = emptySet(),
            excludedFilenames   = emptySet(),
            excludedDirectories = emptySet(),
            submissionDir       = projectDir
        )

        val entries = zipEntryNames(zipFile)
        assertTrue(entries.any { it.endsWith("main.cpp") })
        assertTrue(entries.any { it.endsWith("main.h") })
        assertTrue(entries.any { it.endsWith("README.md") })
    }

    fun testZipFileIsCreatedInProjectRootInModeOne() {
        createFile("main.cpp")

        val zipFile = ZipFilesService(project).zipProject(
            zipFilename         = "test.zip",
            excludedExtensions  = emptySet(),
            excludedFilenames   = emptySet(),
            excludedDirectories = emptySet(),
            submissionDir       = projectDir
        )

        assertEquals(projectDir, zipFile.parentFile)
    }

    // ── Basic zipping - Mode 2 (subdirectory) ────────────────────────────────

    fun testZipProjectCreatesZipFileInSubdirectory() {
        val subDir = File(projectDir, "CS370_Assign01_Fa25")
        subDir.mkdirs()
        createFile("CS370_Assign01_Fa25/main.cpp")
        createFile("CS370_Assign01_Fa25/main.h")

        val zipFile = ZipFilesService(project).zipProject(
            zipFilename         = "assign01_submission.zip",
            excludedExtensions  = emptySet(),
            excludedFilenames   = emptySet(),
            excludedDirectories = emptySet(),
            submissionDir       = subDir
        )

        assertTrue(zipFile.exists())
        assertEquals(subDir, zipFile.parentFile)
    }

    fun testZipProjectOnlyIncludesSubdirectoryFilesInModeTwo() {
        val subDir = File(projectDir, "CS370_Assign01_Fa25")
        subDir.mkdirs()
        createFile("CS370_Assign01_Fa25/main.cpp")
        createFile("CS370_Assign01_Fa25/main.h")
        createFile("CS370_Assign02_Fa25/main.cpp") // different assignment - should not be included

        val zipFile = ZipFilesService(project).zipProject(
            zipFilename         = "assign01_submission.zip",
            excludedExtensions  = emptySet(),
            excludedFilenames   = emptySet(),
            excludedDirectories = emptySet(),
            submissionDir       = subDir
        )

        val entries = zipEntryNames(zipFile)
        assertTrue(entries.any  { it.endsWith("main.cpp") })
        assertTrue(entries.any  { it.endsWith("main.h") })
        // files from other assignment subdirectories must not be included
        assertFalse(entries.any { it.contains("CS370_Assign02_Fa25") })
    }

    fun testZipEntryPathsAreRelativeToSubmissionDir() {
        val subDir = File(projectDir, "CS370_Assign01_Fa25")
        subDir.mkdirs()
        createFile("CS370_Assign01_Fa25/main.cpp")

        val zipFile = ZipFilesService(project).zipProject(
            zipFilename         = "assign01_submission.zip",
            excludedExtensions  = emptySet(),
            excludedFilenames   = emptySet(),
            excludedDirectories = emptySet(),
            submissionDir       = subDir
        )

        val entries = zipEntryNames(zipFile)
        // entry paths should be relative to the subdirectory, not the project root
        assertTrue(entries.any  { it == "main.cpp" || it.endsWith("/main.cpp") })
        assertFalse(entries.any { it.contains("CS370_Assign01_Fa25") })
    }

    // ── allowedExtensions ────────────────────────────────────────────────────

    fun testAllowedExtensionsFiltersCorrectly() {
        createFile("main.cpp")
        createFile("main.h")
        createFile("notes.txt")

        val zipFile = ZipFilesService(project).zipProject(
            zipFilename         = "test.zip",
            allowedExtensions   = setOf("cpp", "h"),
            excludedExtensions  = emptySet(),
            excludedFilenames   = emptySet(),
            excludedDirectories = emptySet(),
            submissionDir       = projectDir
        )

        val entries = zipEntryNames(zipFile)
        assertTrue(entries.any  { it.endsWith("main.cpp") })
        assertTrue(entries.any  { it.endsWith("main.h") })
        assertFalse(entries.any { it.endsWith("notes.txt") })
    }

    fun testEmptyAllowedExtensionsProducesEmptyZip() {
        createFile("main.cpp")
        createFile("main.h")

        val zipFile = ZipFilesService(project).zipProject(
            zipFilename         = "test.zip",
            allowedExtensions   = emptySet(),
            excludedExtensions  = emptySet(),
            excludedFilenames   = emptySet(),
            excludedDirectories = emptySet(),
            submissionDir       = projectDir
        )

        assertTrue(zipEntryNames(zipFile).isEmpty())
    }

    // ── allowedFilenames ─────────────────────────────────────────────────────

    fun testAllowedFilenamesFiltersCorrectly() {
        createFile("main.cpp")
        createFile("main.h")
        createFile("Flags.h")

        val zipFile = ZipFilesService(project).zipProject(
            zipFilename         = "test.zip",
            allowedFilenames    = setOf("main.cpp", "main.h"),
            excludedExtensions  = emptySet(),
            excludedFilenames   = emptySet(),
            excludedDirectories = emptySet(),
            submissionDir       = projectDir
        )

        val entries = zipEntryNames(zipFile)
        assertTrue(entries.any  { it.endsWith("main.cpp") })
        assertTrue(entries.any  { it.endsWith("main.h") })
        assertFalse(entries.any { it.endsWith("Flags.h") })
    }

    fun testEmptyAllowedFilenamesProducesEmptyZip() {
        createFile("main.cpp")
        createFile("main.h")

        val zipFile = ZipFilesService(project).zipProject(
            zipFilename         = "test.zip",
            allowedFilenames    = emptySet(),
            excludedExtensions  = emptySet(),
            excludedFilenames   = emptySet(),
            excludedDirectories = emptySet(),
            submissionDir       = projectDir
        )

        assertTrue(zipEntryNames(zipFile).isEmpty())
    }

    fun testAllowedFilenamesAndAllowedExtensionsBothMustMatch() {
        createFile("main.cpp")
        createFile("main.h")
        createFile("helper.cpp")

        val zipFile = ZipFilesService(project).zipProject(
            zipFilename         = "test.zip",
            allowedExtensions   = setOf("cpp"),
            allowedFilenames    = setOf("main.cpp", "main.h"),
            excludedExtensions  = emptySet(),
            excludedFilenames   = emptySet(),
            excludedDirectories = emptySet(),
            submissionDir       = projectDir
        )

        val entries = zipEntryNames(zipFile)
        assertTrue(entries.any  { it.endsWith("main.cpp") })   // passes both filters
        assertFalse(entries.any { it.endsWith("main.h") })     // fails allowedExtensions
        assertFalse(entries.any { it.endsWith("helper.cpp") }) // fails allowedFilenames
    }

    // ── excludedFilenames ────────────────────────────────────────────────────

    fun testExcludedFilenamesAreExcluded() {
        createFile("main.cpp")
        createFile("Flags.h")
        createFile("tests.cpp")

        val zipFile = ZipFilesService(project).zipProject(
            zipFilename         = "test.zip",
            excludedExtensions  = emptySet(),
            excludedFilenames   = setOf("Flags.h", "tests.cpp"),
            excludedDirectories = emptySet(),
            submissionDir       = projectDir
        )

        val entries = zipEntryNames(zipFile)
        assertTrue(entries.any  { it.endsWith("main.cpp") })
        assertFalse(entries.any { it.endsWith("Flags.h") })
        assertFalse(entries.any { it.endsWith("tests.cpp") })
    }

    // ── excludedExtensions ───────────────────────────────────────────────────

    fun testExcludedExtensionsAreExcluded() {
        createFile("main.cpp")
        createFile("main.o")
        createFile("main.d")

        val zipFile = ZipFilesService(project).zipProject(
            zipFilename         = "test.zip",
            excludedExtensions  = setOf("o", "d"),
            excludedFilenames   = emptySet(),
            excludedDirectories = emptySet(),
            submissionDir       = projectDir
        )

        val entries = zipEntryNames(zipFile)
        assertTrue(entries.any  { it.endsWith("main.cpp") })
        assertFalse(entries.any { it.endsWith("main.o") })
        assertFalse(entries.any { it.endsWith("main.d") })
    }

    fun testExcludedExtensionsCaseInsensitive() {
        createFile("main.CPP")
        createFile("main.h")

        val zipFile = ZipFilesService(project).zipProject(
            zipFilename         = "test.zip",
            excludedExtensions  = setOf("cpp"),
            excludedFilenames   = emptySet(),
            excludedDirectories = emptySet(),
            submissionDir       = projectDir
        )

        val entries = zipEntryNames(zipFile)
        assertFalse(entries.any { it.endsWith("main.CPP") })
        assertTrue(entries.any  { it.endsWith("main.h") })
    }

    // ── excludedDirectories ──────────────────────────────────────────────────

    fun testExcludedDirectoriesAreNotRecursed() {
        createFile("main.cpp")
        createFile("build/output.o")
        createFile("build/output.d")

        val zipFile = ZipFilesService(project).zipProject(
            zipFilename         = "test.zip",
            excludedExtensions  = emptySet(),
            excludedFilenames   = emptySet(),
            excludedDirectories = setOf("build"),
            submissionDir       = projectDir
        )

        val entries = zipEntryNames(zipFile)
        assertTrue(entries.any  { it.endsWith("main.cpp") })
        assertFalse(entries.any { it.contains("build/") })
    }

    fun testNestedExcludedDirectoriesAreNotRecursed() {
        createFile("main.cpp")
        createFile("src/build/output.o")

        val zipFile = ZipFilesService(project).zipProject(
            zipFilename         = "test.zip",
            excludedExtensions  = emptySet(),
            excludedFilenames   = emptySet(),
            excludedDirectories = setOf("build"),
            submissionDir       = projectDir
        )

        val entries = zipEntryNames(zipFile)
        assertTrue(entries.any  { it.endsWith("main.cpp") })
        assertFalse(entries.any { it.contains("build/") })
    }

    // ── Wildcard excludedFilenames ────────────────────────────────────────────

    fun testWildcardExcludedFilenamesSuffixMatch() {
        createFile("test1_output.txt")
        createFile("anotherTest_output.txt")
        createFile("main.cpp")

        val zipFile = ZipFilesService(project).zipProject(
            zipFilename         = "test.zip",
            excludedExtensions  = emptySet(),
            excludedFilenames   = setOf("*_output.txt"),
            excludedDirectories = emptySet(),
            submissionDir       = projectDir
        )

        val entries = zipEntryNames(zipFile)
        assertFalse(entries.any { it.endsWith("test1_output.txt") })
        assertFalse(entries.any { it.endsWith("anotherTest_output.txt") })
        assertTrue(entries.any  { it.endsWith("main.cpp") })
    }

    fun testWildcardExcludedFilenamesPrefixMatch() {
        createFile("filename1.txt")
        createFile("filename99.txt")
        createFile("main.cpp")

        val zipFile = ZipFilesService(project).zipProject(
            zipFilename         = "test.zip",
            excludedExtensions  = emptySet(),
            excludedFilenames   = setOf("filename*.txt"),
            excludedDirectories = emptySet(),
            submissionDir       = projectDir
        )

        val entries = zipEntryNames(zipFile)
        assertFalse(entries.any { it.endsWith("filename1.txt") })
        assertFalse(entries.any { it.endsWith("filename99.txt") })
        assertTrue(entries.any  { it.endsWith("main.cpp") })
    }

    fun testWildcardExcludedFilenamesMixedExactAndWildcard() {
        createFile("test1_output.txt")
        createFile(".DS_Store")
        createFile("main.cpp")

        val zipFile = ZipFilesService(project).zipProject(
            zipFilename         = "test.zip",
            excludedExtensions  = emptySet(),
            excludedFilenames   = setOf(".DS_Store", "*_output.txt"),
            excludedDirectories = emptySet(),
            submissionDir       = projectDir
        )

        val entries = zipEntryNames(zipFile)
        assertFalse(entries.any { it.endsWith("test1_output.txt") })
        assertFalse(entries.any { it.endsWith(".DS_Store") })
        assertTrue(entries.any  { it.endsWith("main.cpp") })
    }

    // ── Wildcard excludedDirectories ──────────────────────────────────────────

    fun testWildcardExcludedDirectoriesPrefixMatch() {
        createFile("main.cpp")
        createFile("cmake-build-debug/output.o")
        createFile("cmake-build-release/output.o")

        val zipFile = ZipFilesService(project).zipProject(
            zipFilename         = "test.zip",
            excludedExtensions  = emptySet(),
            excludedFilenames   = emptySet(),
            excludedDirectories = setOf("cmake-build-*"),
            submissionDir       = projectDir
        )

        val entries = zipEntryNames(zipFile)
        assertTrue(entries.any  { it.endsWith("main.cpp") })
        assertFalse(entries.any { it.contains("cmake-build-debug") })
        assertFalse(entries.any { it.contains("cmake-build-release") })
    }

    fun testWildcardExcludedDirectoriesMixedExactAndWildcard() {
        createFile("main.cpp")
        createFile("build/output.o")
        createFile("cmake-build-debug/output.o")

        val zipFile = ZipFilesService(project).zipProject(
            zipFilename         = "test.zip",
            excludedExtensions  = emptySet(),
            excludedFilenames   = emptySet(),
            excludedDirectories = setOf("build", "cmake-build-*"),
            submissionDir       = projectDir
        )

        val entries = zipEntryNames(zipFile)
        assertTrue(entries.any  { it.endsWith("main.cpp") })
        assertFalse(entries.any { it.contains("build/") })
        assertFalse(entries.any { it.contains("cmake-build-debug/") })
    }

    // ── Zip file cleanup ─────────────────────────────────────────────────────

    fun testExistingZipFileIsOverwritten() {
        createFile("main.cpp")
        val zipFile = File(projectDir, "test.zip")
        zipFile.writeText("old content")

        val result = ZipFilesService(project).zipProject(
            zipFilename         = "test.zip",
            excludedExtensions  = emptySet(),
            excludedFilenames   = emptySet(),
            excludedDirectories = emptySet(),
            submissionDir       = projectDir
        )

        assertTrue(result.exists())
        assertTrue(result.length() > 0)
        assertFalse(result.readText().startsWith("old content"))
    }

    fun testExistingZipFileInSubdirectoryIsOverwritten() {
        val subDir = File(projectDir, "CS370_Assign01_Fa25")
        subDir.mkdirs()
        createFile("CS370_Assign01_Fa25/main.cpp")
        val zipFile = File(subDir, "assign01_submission.zip")
        zipFile.writeText("old content")

        val result = ZipFilesService(project).zipProject(
            zipFilename         = "assign01_submission.zip",
            excludedExtensions  = emptySet(),
            excludedFilenames   = emptySet(),
            excludedDirectories = emptySet(),
            submissionDir       = subDir
        )

        assertTrue(result.exists())
        assertEquals(subDir, result.parentFile)
        assertFalse(result.readText().startsWith("old content"))
    }
}