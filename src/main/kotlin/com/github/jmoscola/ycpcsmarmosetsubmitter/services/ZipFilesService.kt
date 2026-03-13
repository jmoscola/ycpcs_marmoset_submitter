package com.github.jmoscola.ycpcsmarmosetsubmitter.services

import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipFilesService(private val project: Project) {

    private val defaultExcludedDirs = setOf(
        ".git",
        ".idea",
        ".vs",
        ".gradle",
        "build",
        "out",
        "target",
        "node_modules",
        "cmake-build-debug"
    )

    private val defaultExcludedFiles = setOf(
        ".DS_Store",
        "submission.zip"
    )

    private val defaultExcludedExtensions = setOf(
        "o",
        "d",
        "a",
        "iml",
        "log",
        "stackdump",
        "exe"
    )

    /**
     * Zips project files into a single zip file.
     * @param zipFilename Name of the zip file to create in project base path.
     * @param allowedExtensions Optional whitelist of file extensions (without '.')
     * @param excludedFilenames Optional blacklist of filenames to skip
     */
    fun zipProject(
        zipFilename: String,
        allowedExtensions: Set<String>? = null,
        excludedFilenames: Set<String> = emptySet()
    ): File {
        val basePath = project.basePath ?: error("Project base path not found")
        val baseDir = File(basePath)
        val baseDirPath: Path = baseDir.toPath()
        val zipFile = File(baseDir, zipFilename)

        // remove previous zip file if one exists
        if (zipFile.exists()) {
            zipFile.delete()
        }

        val zippedSuccessfully = ProgressManager.getInstance().runProcessWithProgressSynchronously(
            {
                ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
                    baseDir.walkTopDown()
                        .onEnter { dir -> !defaultExcludedDirs.contains(dir.name) }
                        .filter { file -> file.isFile }
                        .filter { file -> !defaultExcludedFiles.contains(file.name) }
                        .filter { file -> !excludedFilenames.contains(file.name) }
                        .filter { file -> !defaultExcludedExtensions.contains(file.extension.lowercase()) }
                        .filter { file -> allowedExtensions == null || allowedExtensions.contains(file.extension.lowercase()) }
                        .forEach { file ->
                            ProgressManager.getInstance().progressIndicator?.text = "Zipping ${file.relativeTo(baseDir)}"
                            addFileToZip(file, baseDirPath, zipOut)
                        }
                }
            },"Creating Submission Archive",true, project
        )

        if (!zippedSuccessfully) {
            if (zipFile.exists()) {
                zipFile.delete()
            }
            throw ProcessCanceledException()
        }
        return zipFile
    }

    /**
     * Zips project files into a single zip file.
     * @param file Name of the file to add to the zip file
     * @param baseDirPath Path to file
     * @param zipOut The Zip output stream for the zip archive
     */
    private fun addFileToZip(file: File, baseDirPath: Path, zipOut: ZipOutputStream) {
        val relativeZipPath = baseDirPath
            .relativize(file.toPath())
            .toString()
            .replace("\\", "/")

        val topLevelFolder = "submission" // or assignment name
        val zipEntryPath = "$topLevelFolder/$relativeZipPath" // top-level folder in zip
        zipOut.putNextEntry(ZipEntry(zipEntryPath))
        file.inputStream().use { input -> copyStreamWithCancelCheck(input, zipOut) }
        zipOut.closeEntry()
    }

    /**
     * Zips project files into a single zip file.
     * @param input File stream to add to zip archive
     * @param zipOut The Zip output stream for the zip archive
     */
    private fun copyStreamWithCancelCheck(input: InputStream, zipOut: ZipOutputStream) {
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (input.read(buffer).also { bytesRead = it } != -1) {
            ProgressManager.checkCanceled()
            zipOut.write(buffer, 0, bytesRead)
        }
    }
}
