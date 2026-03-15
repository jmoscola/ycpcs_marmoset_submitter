package com.github.jmoscola.ycpcsmarmosetsubmitter.services

import com.intellij.openapi.project.Project
import java.io.File
import java.net.HttpURLConnection
import java.net.URL


class UploadService(private val project: Project) {

    companion object {
        private const val SUBMISSION_URL = "https://cs.ycp.edu/marmoset/bluej/SubmitProjectViaBlueJSubmitter"
        private const val BOUNDARY = "----UploaderBoundary"
        private const val CRLF = "\r\n"
    }

    /**
     * Uploads a zip file to the Marmoset submission server.
     * Mimics the following curl command:
     *   curl -F 'submittedFiles=@solution.zip' -F 'campusUID=username' -F 'password=password' -F 'semester=Fall 2026' -F 'courseName=CS 350' -F 'projectNumber=assign01' https://cs.ycp.edu/marmoset/bluej/SubmitProjectViaBlueJSubmitter
     * @param zipFile       The zip file to upload.
     * @param username      The campus UID.
     * @param password      The user's password.
     * @param assignmentInfo The parsed assignment info (courseName, projectNumber, semester).
     * @throws UploadException if the server returns a non-success response.
     * @throws Exception for network or IO errors.
     */
    fun upload(
        zipFile: File,
        username: String,
        password: String,
        assignmentInfo: AssignmentInfo
    ) {
        val connection = (URL(SUBMISSION_URL).openConnection() as HttpURLConnection).apply {
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
            writeField("campusUID",     username)
            writeField("password",      password)
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
                ?: "No error message returned"
            throw UploadException(responseCode, "Upload failed (HTTP $responseCode): $message")
        }
    }
}

class UploadException(val responseCode: Int, message: String) : Exception(message)