package edu.ycp.cs.marmosetsubmitter.services

import edu.ycp.cs.marmosetsubmitter.MarmosetSubmitterBundle
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.NonNls
import java.io.File
import java.net.HttpURLConnection
import java.net.URI

/**
 * Service that uploads a student's submission zip file to the Marmoset
 * submission server via an HTTP multipart/form-data POST request. The
 * request mimics the following curl command:
 * ```
 * curl -F 'submittedFiles=@solution.zip'
 *      -F 'campusUID=username'
 *      -F 'password=password'
 *      -F 'semester=Fall 2026'
 *      -F 'courseName=CS 350'
 *      -F 'projectNumber=assign01'
 *      https://cs.ycp.edu/marmoset/bluej/SubmitProjectViaBlueJSubmitter
 * ```
 *
 * @param project The current IntelliJ project.
 * @see UploadException
 */
class UploadService(private val project: Project) {

    companion object {
        @NonNls private const val BOUNDARY = "----UploaderBoundary"
        @NonNls private const val CRLF = "\r\n"
    }

    /**
     * Uploads the specified zip file to the Marmoset submission server using
     * an HTTP multipart/form-data POST request. The request includes the
     * campusUID, password, semester, course name, project number, and the
     * zip file itself as form fields.
     *
     * The zip file bytes are written directly to the underlying output stream
     * to avoid charset encoding of binary data.
     *
     * @param zipFile        The zip file to upload.
     * @param credentials    A [MarmosetCredentials] containing the student's
     *                       Marmoset campus UID and password.
     * @param assignmentInfo The parsed assignment info containing the course
     *                       name, project number, and semester.
     * @param submissionUrl  The URL of the Marmoset submission server,
     *                       sourced from the project configuration file.
     * @throws UploadException       if the server returns a non-2xx HTTP response code.
     * @throws MalformedURLException if the submission URL is malformed.
     * @throws IOException           if a network or IO error occurs during the upload.
     */
    fun upload(
        zipFile: File,
        credentials: MarmosetCredentials,
        assignmentInfo: AssignmentInfo,
        submissionUrl: String
    ) {
        val connection = (URI(submissionUrl).toURL().openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$BOUNDARY")
        }

        connection.outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->

            // helper to write a plain text form field
            fun writeField(name: String, value: String) {
                writer.write("--$BOUNDARY$CRLF")
                writer.write("Content-Disposition: form-data; name=\"$name\"$CRLF")
                writer.write(CRLF)
                writer.write(value)
                writer.write(CRLF)
            }

            // text fields — mimics the -F key=value curl arguments
            writeField("campusUID",     credentials.username)
            writeField("password",      credentials.password)
            writeField("semester",      assignmentInfo.semester)
            writeField("courseName",    assignmentInfo.courseName)
            writeField("projectNumber", assignmentInfo.projectNumber)

            // file field — mimics -F 'submittedFiles=@solution.zip'
            writer.write("--$BOUNDARY$CRLF")
            writer.write("Content-Disposition: form-data; name=\"submittedFiles\"; filename=\"${zipFile.name}\"$CRLF")
            writer.write("Content-Type: application/zip$CRLF")
            writer.write(CRLF)
            writer.flush()

            // write the zip file bytes directly to the underlying output stream
            zipFile.inputStream().use { fileInput ->
                fileInput.copyTo(connection.outputStream)
            }

            // final boundary
            writer.write(CRLF)
            writer.write("--$BOUNDARY--$CRLF")
            writer.flush()
        }

        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            val message = connection.errorStream?.bufferedReader()?.readText()
                ?: MarmosetSubmitterBundle.message("uploadService.error.noMessage")
            throw UploadException(
                responseCode,
                MarmosetSubmitterBundle.message("uploadService.error.uploadFailed", responseCode, message)
            )
        }
    }
}

/**
 * Exception thrown by [UploadService] when the Marmoset submission server
 * returns a non-2xx HTTP response code. Carries the HTTP response code so
 * that callers can provide specific error messages for known failure cases
 * such as invalid credentials (403) or invalid course or assignment name (404).
 *
 * @param responseCode The HTTP response code returned by the server.
 * @param message      A descriptive message describing the failure.
 */
class UploadException(val responseCode: Int, message: String) : Exception(message)