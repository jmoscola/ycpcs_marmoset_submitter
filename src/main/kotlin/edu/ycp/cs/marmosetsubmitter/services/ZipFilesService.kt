package edu.ycp.cs.marmosetsubmitter.services

import edu.ycp.cs.marmosetsubmitter.MarmosetSubmitterBundle
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

/**
 * Service that creates a zip file containing the student's project files
 * for submission to the Marmoset server. The zipping process runs in a
 * background thread with a progress dialog, allowing the user to monitor
 * progress and cancel the operation if needed.
 *
 * Files are included or excluded based on the following rules, all of
 * which are sourced from the project configuration file:
 *   - Only files with names in [allowedFilenames] are included
 *     (if null, all filenames are allowed — more restrictive than [allowedExtensions])
 *   - Only files with extensions in [allowedExtensions] are included
 *     (if null, all extensions are allowed)
 *   - Files with names in [excludedFilenames] are excluded
 *   - Files with extensions in [excludedExtensions] are excluded
 *   - Directories with names in [excludedDirectories] are not recursed
 *     and their contents are excluded entirely
 *
 * @param project The current IntelliJ project.
 */
class ZipFilesService(private val project: Project) {

    /**
     * Creates a zip file containing the project files that satisfy the
     * specified inclusion and exclusion rules. The operation runs in a
     * background thread managed by [ProgressManager], displaying a progress
     * dialog with a cancel button. The progress indicator transitions from
     * an indeterminate spinner during the file scanning phase to a
     * determinate progress bar during the zipping phase.
     *
     * If the user cancels the operation or an exception occurs during
     * zipping, any partially created zip file is deleted before the
     * exception is propagated to the caller.
     *
     * @param zipFilename         The name of the zip file to create in the project
     *                            root directory. Any existing file with the same
     *                            name will be overwritten.
     * @param allowedFilenames    A whitelist of exact filenames to include in the
     *                            zip file. A value of null indicates that all
     *                            filenames are allowed. When set, only files whose
     *                            names appear in this set will be included,
     *                            regardless of extension. This is a more restrictive
     *                            filter than [allowedExtensions].
     * @param allowedExtensions   A whitelist of file extensions to include in the
     *                            zip file. A value of null indicates that all
     *                            extensions are allowed. An empty set indicates
     *                            that no files will be included.
     * @param excludedExtensions  A set of file extensions to exclude from the zip file.
     * @param excludedFilenames   A set of filenames to exclude from the zip file.
     * @param excludedDirectories A set of directory names to exclude from the zip
     *                            file. Neither the directory nor any of its contents
     *                            will be included.
     * @return A [File] reference to the created zip file.
     * @throws ProcessCanceledException if the user clicks the cancel button
     *                                  in the progress dialog.
     * @throws IllegalStateException    if the project base path cannot be determined.
     * @throws Exception                if an IO error occurs while creating the zip file.
     */
    fun zipProject(
        zipFilename: String,
        allowedFilenames: Set<String>? = null,   // null = no restriction
        allowedExtensions: Set<String>? = null,  // null = allow all
        excludedExtensions: Set<String>,
        excludedFilenames: Set<String>,
        excludedDirectories: Set<String>
    ): File {
        val basePath = project.basePath ?: error(MarmosetSubmitterBundle.message("zipFilesService.error.projectPathNotFound"))
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

                indicator.text = MarmosetSubmitterBundle.message("zipFilesService.scanningProjectFiles")
                indicator.isIndeterminate = true

                // create list of files to zip
                val filesToZip = baseDir.walkTopDown()
                    .onEnter { dir ->
                        ProgressManager.checkCanceled()
                        !excludedDirectories.contains(dir.name)
                    }
                    .filter { it.isFile }
                    .filter { !excludedFilenames.contains(it.name) }
                    .filter { !excludedExtensions.contains(it.extension.lowercase()) }
                    .filter { allowedExtensions == null || allowedExtensions.contains(it.extension.lowercase()) }
                    .filter { allowedFilenames == null || allowedFilenames.contains(it.name) }
                    .toList()

                indicator.isIndeterminate = false

                try {
                    ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
                        filesToZip.forEachIndexed { index, file ->
                            ProgressManager.checkCanceled()
                            indicator.text = MarmosetSubmitterBundle.message("zipFilesService.zippingFile", file.relativeTo(baseDir))
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
            MarmosetSubmitterBundle.message("zipFilesService.creatingArchive"),
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
     * Adds a single file to the zip archive as a [ZipEntry]. The entry path
     * is the file's path relative to the project base directory, with
     * backslashes replaced by forward slashes for cross-platform compatibility.
     * File bytes are written using [copyStreamWithCancelCheck] to support
     * cancellation during writing.
     *
     * @param file        The file to add to the zip archive.
     * @param baseDirPath The project base directory path, used to compute
     *                    the relative entry path within the zip file.
     * @param zipOut      The [ZipOutputStream] to write the file entry to.
     * @throws ProcessCanceledException if the user cancels the operation
     *                                  during the file write.
     */
    private fun addFileToZip(file: File, baseDirPath: Path, zipOut: ZipOutputStream) {
        val relativeZipPath = baseDirPath
            .relativize(file.toPath())
            .toString()
            .replace("\\", "/")

        zipOut.putNextEntry(ZipEntry(relativeZipPath))
        file.inputStream().use { input -> copyStreamWithCancelCheck(input, zipOut) }
        zipOut.closeEntry()
    }

    /**
     * Copies bytes from the input stream to the zip output stream in 8KB
     * chunks, calling [ProgressManager.checkCanceled] between each chunk
     * to support cancellation of large file writes.
     *
     * @param input  The input stream to read file bytes from.
     * @param zipOut The [ZipOutputStream] to write the bytes to.
     * @throws ProcessCanceledException if the user cancels the operation
     *                                  between chunks.
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
