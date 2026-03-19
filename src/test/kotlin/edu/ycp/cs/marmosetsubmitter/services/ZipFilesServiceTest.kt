package edu.ycp.cs.marmosetsubmitter.services

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File
import java.util.zip.ZipFile

class ZipFilesServiceTest : BasePlatformTestCase() {

    private lateinit var projectDir: File

    override fun setUp() {
        super.setUp()
        projectDir = File(project.basePath!!)
        projectDir.mkdirs() // ensure the directory exists on disk
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

    // ── Basic zipping ────────────────────────────────────────────────────────

    fun testZipProjectCreatesZipFile() {
        createFile("main.cpp")
        createFile("main.h")

        val zipFile = ZipFilesService(project).zipProject(
            zipFilename         = "test.zip",
            excludedExtensions  = emptySet(),
            excludedFilenames   = emptySet(),
            excludedDirectories = emptySet()
        )

        assertTrue(zipFile.exists())
        zipFile.delete()
    }

    fun testZipProjectIncludesAllFilesWhenNoFiltersSet() {
        createFile("main.cpp")
        createFile("main.h")
        createFile("README.md")

        val zipFile = ZipFilesService(project).zipProject(
            zipFilename         = "test.zip",
            excludedExtensions  = emptySet(),
            excludedFilenames   = emptySet(),
            excludedDirectories = emptySet()
        )

        val entries = zipEntryNames(zipFile)
        assertTrue(entries.any { it.endsWith("main.cpp") })
        assertTrue(entries.any { it.endsWith("main.h") })
        assertTrue(entries.any { it.endsWith("README.md") })
        zipFile.delete()
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
            excludedDirectories = emptySet()
        )

        val entries = zipEntryNames(zipFile)
        assertTrue(entries.any  { it.endsWith("main.cpp") })
        assertTrue(entries.any  { it.endsWith("main.h") })
        assertFalse(entries.any { it.endsWith("notes.txt") })
        zipFile.delete()
    }

    fun testEmptyAllowedExtensionsProducesEmptyZip() {
        createFile("main.cpp")
        createFile("main.h")

        val zipFile = ZipFilesService(project).zipProject(
            zipFilename         = "test.zip",
            allowedExtensions   = emptySet(),
            excludedExtensions  = emptySet(),
            excludedFilenames   = emptySet(),
            excludedDirectories = emptySet()
        )

        assertTrue(zipEntryNames(zipFile).isEmpty())
        zipFile.delete()
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
            excludedDirectories = emptySet()
        )

        val entries = zipEntryNames(zipFile)
        assertTrue(entries.any  { it.endsWith("main.cpp") })
        assertTrue(entries.any  { it.endsWith("main.h") })
        assertFalse(entries.any { it.endsWith("Flags.h") })
        zipFile.delete()
    }

    fun testEmptyAllowedFilenamesProducesEmptyZip() {
        createFile("main.cpp")
        createFile("main.h")

        val zipFile = ZipFilesService(project).zipProject(
            zipFilename         = "test.zip",
            allowedFilenames    = emptySet(),
            excludedExtensions  = emptySet(),
            excludedFilenames   = emptySet(),
            excludedDirectories = emptySet()
        )

        assertTrue(zipEntryNames(zipFile).isEmpty())
        zipFile.delete()
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
            excludedDirectories = emptySet()
        )

        val entries = zipEntryNames(zipFile)
        assertTrue(entries.any  { it.endsWith("main.cpp") })   // passes both filters
        assertFalse(entries.any { it.endsWith("main.h") })     // fails allowedExtensions
        assertFalse(entries.any { it.endsWith("helper.cpp") }) // fails allowedFilenames
        zipFile.delete()
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
            excludedDirectories = emptySet()
        )

        val entries = zipEntryNames(zipFile)
        assertTrue(entries.any  { it.endsWith("main.cpp") })
        assertFalse(entries.any { it.endsWith("Flags.h") })
        assertFalse(entries.any { it.endsWith("tests.cpp") })
        zipFile.delete()
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
            excludedDirectories = emptySet()
        )

        val entries = zipEntryNames(zipFile)
        assertTrue(entries.any  { it.endsWith("main.cpp") })
        assertFalse(entries.any { it.endsWith("main.o") })
        assertFalse(entries.any { it.endsWith("main.d") })
        zipFile.delete()
    }

    fun testExcludedExtensionsCaseInsensitive() {
        createFile("main.CPP")
        createFile("main.h")

        val zipFile = ZipFilesService(project).zipProject(
            zipFilename         = "test.zip",
            excludedExtensions  = setOf("cpp"),
            excludedFilenames   = emptySet(),
            excludedDirectories = emptySet()
        )

        val entries = zipEntryNames(zipFile)
        assertFalse(entries.any { it.endsWith("main.CPP") })
        assertTrue(entries.any  { it.endsWith("main.h") })
        zipFile.delete()
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
            excludedDirectories = setOf("build")
        )

        val entries = zipEntryNames(zipFile)
        assertTrue(entries.any  { it.endsWith("main.cpp") })
        assertFalse(entries.any { it.contains("build/") })
        zipFile.delete()
    }

    fun testNestedExcludedDirectoriesAreNotRecursed() {
        createFile("main.cpp")
        createFile("src/build/output.o")

        val zipFile = ZipFilesService(project).zipProject(
            zipFilename         = "test.zip",
            excludedExtensions  = emptySet(),
            excludedFilenames   = emptySet(),
            excludedDirectories = setOf("build")
        )

        val entries = zipEntryNames(zipFile)
        assertTrue(entries.any  { it.endsWith("main.cpp") })
        assertFalse(entries.any { it.contains("build/") })
        zipFile.delete()
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
            excludedDirectories = emptySet()
        )

        assertTrue(result.exists())
        assertTrue(result.length() > 0)
        assertFalse(result.readText().startsWith("old content"))
        result.delete()
    }
}