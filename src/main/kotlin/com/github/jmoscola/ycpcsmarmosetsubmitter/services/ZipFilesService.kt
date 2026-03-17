package com.github.jmoscola.ycpcsmarmosetsubmitter.services

import com.github.jmoscola.ycpcsmarmosetsubmitter.SubmitterBundle
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
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
        val basePath = project.basePath ?: error(SubmitterBundle.message("zipFilesService.error.projectPathNotFound"))
        val baseDir = File(basePath)
        val baseDirPath: Path = baseDir.toPath()
        val zipFile = File(baseDir, zipFilename)

        // remove previous zip file if one exists
        if (zipFile.exists()) {
            zipFile.delete()
        }

        // variable to shuttle general exceptions out of the lambda
        // exceptions might include disk writing issues, etc.
        var zipException: Exception? = null

        val zippedSuccessfully = ProgressManager.getInstance().runProcessWithProgressSynchronously(
            {
                val indicator: ProgressIndicator = ProgressManager.getInstance().progressIndicator

                indicator.text = SubmitterBundle.message("zipFilesService.scanningProjectFiles")
                indicator.isIndeterminate = true

                // create list of files to zip
                val filesToZip = baseDir.walkTopDown()
                    .onEnter { dir ->
                        ProgressManager.checkCanceled()
                        !defaultExcludedDirs.contains(dir.name)
                    }
                    .filter { it.isFile }
                    .filter { !defaultExcludedFiles.contains(it.name) }
                    .filter { !excludedFilenames.contains(it.name) }
                    .filter { !defaultExcludedExtensions.contains(it.extension.lowercase()) }
                    .filter { allowedExtensions == null || allowedExtensions.contains(it.extension.lowercase()) }
                    .toList()

                indicator.isIndeterminate = false

                try {
                    ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
                        filesToZip.forEachIndexed { index, file ->
                            ProgressManager.checkCanceled()
                            indicator.text = SubmitterBundle.message("zipFilesService.zippingFile", file.relativeTo(baseDir))
                            indicator.fraction = (index + 1).toDouble() / filesToZip.size
                            addFileToZip(file, baseDirPath, zipOut)
                        }
                    }
                } catch (e: Exception) {
                    // Don't catch ProcessCanceledException which is generated when using clicks
                    // the cancel button — let it propagate normally so runProcessWithProgressSynchronously
                    // can handle cancellation and return false. ProcessCanceledException will get rethrown
                    // outside of lambda
                    if (e is ProcessCanceledException) throw e
                    zipException = e  // shuttle other non-cancellation exceptions out
                }
            },
            SubmitterBundle.message("zipFilesService.creatingArchive"),
            true,
            project
        )

        // rethrow any non-cancellation exception that occurred inside the lambda
        zipException?.let {
            if (zipFile.exists()) {
                zipFile.delete()
            }
            throw it
        }

        // user clicked cancel button, propagate ProcessCanceledException
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

//        val topLevelFolder = "submission" // or assignment name
//        val zipEntryPath = "$topLevelFolder/$relativeZipPath" // top-level folder in zip
        zipOut.putNextEntry(ZipEntry(relativeZipPath))
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
