package com.github.jmoscola.ycpcsmarmosetsubmitter.actions

import com.github.jmoscola.ycpcsmarmosetsubmitter.SubmitterBundle
import com.github.jmoscola.ycpcsmarmosetsubmitter.dialog.LoginDialog
import com.github.jmoscola.ycpcsmarmosetsubmitter.services.AssignmentInfo
import com.github.jmoscola.ycpcsmarmosetsubmitter.services.CMakeAssignmentInfoService
import com.github.jmoscola.ycpcsmarmosetsubmitter.services.LoginCredentialsService
import com.github.jmoscola.ycpcsmarmosetsubmitter.services.MarmosetCredentials
import com.github.jmoscola.ycpcsmarmosetsubmitter.services.ProjectConfig
import com.github.jmoscola.ycpcsmarmosetsubmitter.services.SubmitterConfigService
import com.github.jmoscola.ycpcsmarmosetsubmitter.services.UploadException
import com.github.jmoscola.ycpcsmarmosetsubmitter.services.UploadService
import com.github.jmoscola.ycpcsmarmosetsubmitter.services.ZipFilesService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import java.io.File

/**
 * Action that handles the submission of a student's project to the YCPCS
 * Marmoset submission server. This action is accessible from the main toolbar
 * and the Tools menu.
 *
 * The submission process consists of the following steps:
 *   1. Load and validate the project configuration (ycpcs_marmoset_submitter.properties).
 *   2. Parse the CMake assignment info file to retrieve course and assignment details.
 *   3. Zip the project files according to the rules specified in the configuration.
 *   4. Prompt the user for their Marmoset credentials via a login dialog.
 *   5. Upload the zip file to the Marmoset server using the collected credentials
 *      and assignment information.
 *
 * Each step is implemented as a private helper method that returns null on
 * failure, allowing [actionPerformed] to terminate early using the Elvis
 * operator.
 *
 * @see SubmitterConfigService
 * @see CMakeAssignmentInfoService
 * @see LoginCredentialsService
 * @see ZipFilesService
 * @see UploadService
 */
class SubmitAction : AnAction() {

    /**
     * Invoked when the user triggers the 'Submit to Marmoset' action from either
     * the main toolbar or the Tools menu. Orchestrates the full submission
     * workflow by delegating each step to a private helper method.
     *
     * Each helper method returns null if the step fails or is canceled by the
     * user, and the Elvis operator (?:) is used to terminate the workflow early
     * when necessary. Error and cancellation messages are displayed to the user
     * by each helper method before returning null, so no additional error handling
     * is required here.
     *
     * The submission workflow is as follows:
     *   1. [loadConfig]         — Load ycpcs_marmoset_submitter.properties from the project root.
     *   2. [loadAssignmentInfo] — Parse the CMake assignment info file.
     *   3. [createZip]          — Zip the project files for submission.
     *   4. [getCredentials]     — Prompt the user for their Marmoset credentials.
     *   5. [uploadSubmission]   — Upload the zip file to the Marmoset server.
     *
     * @param e The action event providing context, including the current project.
     */
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val config = loadConfig(project) ?: return                  // configure the plugin
        val assignmentInfo =
            loadAssignmentInfo(project, config) ?: return          // extract assignment info from CMake file
        val zipFile = createZip(project, config, assignmentInfo) ?: return   // create zip file
        val credentials = getCredentials(project) ?: return                  // prompt user for credentials
        uploadSubmission(project, zipFile, credentials, assignmentInfo, config)     // upload zip file
    }

    /**
     * Loads and parses the plugin configuration file (ycpcs_marmoset_submitter.properties)
     * from the project root directory using [SubmitterConfigService].
     *
     * @param project The current IntelliJ project.
     * @return A [ProjectConfig] containing the parsed configuration values, or
     *         null if the configuration file is missing or a required property
     *         is absent.
     */
    private fun loadConfig(project: Project): ProjectConfig? {
        return try {
            SubmitterConfigService(project).load()
        } catch (e: Exception) {
            Messages.showErrorDialog(
                project,
                SubmitterBundle.message(
                    "submitAction.submissionFailed",
                    e.message ?: SubmitterBundle.message("submitAction.error.unknown")
                ),
                SubmitterBundle.message("submitAction.submissionErrorDialogTitle")
            )
            null
        }
    }

    /**
     * Locates and parses the CMake assignment info file specified in the
     * project configuration using [CMakeAssignmentInfoService]. The assignment
     * info file contains the course name, term, and project number required
     * for submission.
     *
     * @param project The current IntelliJ project.
     * @param config  The project configuration containing the assignment info filename.
     * @return An [AssignmentInfo] containing the parsed course name, term, project
     *         number, and semester, or null if the file is missing or a required
     *         field is absent.
     */
    private fun loadAssignmentInfo(project: Project, config: ProjectConfig): AssignmentInfo? {
        return try {
            CMakeAssignmentInfoService(project).parse(config.assignmentInfoFilename)
        } catch (e: Exception) {
            Messages.showErrorDialog(
                project,
                SubmitterBundle.message(
                    "submitAction.submissionFailed",
                    e.message ?: SubmitterBundle.message("submitAction.error.unknown")
                ),
                SubmitterBundle.message("submitAction.submissionErrorDialogTitle")
            )
            null
        }
    }

    /**
     * Zips the project files into a single zip file for submission using
     * [ZipFilesService]. The zip filename is constructed from the project
     * number and the zip filename suffix specified in the project configuration.
     * Allowed extensions, excluded filenames, excluded directories, and excluded
     * extensions are all sourced from the project configuration.
     *
     * @param project        The current IntelliJ project.
     * @param config         The project configuration containing zip options and exclusion rules.
     * @param assignmentInfo The parsed assignment info used to construct the zip filename.
     * @return A [File] reference to the created zip file, or null if the operation
     *         failed or was canceled by the user.
     */
    private fun createZip(project: Project, config: ProjectConfig, assignmentInfo: AssignmentInfo): File? {
        return try {
            ZipFilesService(project).zipProject(
                zipFilename = "${assignmentInfo.projectNumber}${config.zipFilenameSuffix}.zip",
                allowedExtensions = config.allowedExtensions,
                excludedExtensions = config.excludedExtensions,
                excludedFilenames = config.excludedFilenames,
                excludedDirectories = config.excludedDirectories
            )
        } catch (e: ProcessCanceledException) {
            // ProcessCanceledException must be rethrown so the IntelliJ platform
            // can handle the cancellation correctly. A cancellation is not an error,
            // so an informational dialog is shown before rethrowing.
            Messages.showInfoMessage(
                project,
                SubmitterBundle.message("submitAction.submissionCanceled"),
                SubmitterBundle.message("submitAction.submissionDialogTitle")
            )
            throw e
        } catch (e: Exception) {
            // Catch any remaining exceptions from ZipFilesService, such as
            // IOException caused by disk write failures, and display the
            // error message to the user.
            Messages.showErrorDialog(
                project,
                e.message,
                SubmitterBundle.message("submitAction.submissionErrorDialogTitle")
            )
            null
        }
    }

    /**
     * Displays the login dialog to collect the user's Marmoset credentials,
     * saves them securely using [LoginCredentialsService], and returns them
     * as a [MarmosetCredentials] instance. Previously saved credentials are
     * pre-populated in the login dialog via [LoginCredentialsService].
     *
     * @param project The current IntelliJ project.
     * @return A [MarmosetCredentials] containing the username and password if
     *         the user confirmed the dialog, or null if the user canceled.
     */
    private fun getCredentials(project: Project): MarmosetCredentials? {
        val loginService = LoginCredentialsService(project)
        val dialog = LoginDialog(project, loginService.getUsername(), loginService.getPassword())
        if (!dialog.showAndGet()) {
            Messages.showInfoMessage(
                project,
                SubmitterBundle.message("submitAction.submissionCanceled"),
                SubmitterBundle.message("submitAction.submissionDialogTitle")
            )
            return null
        }
        val credentials = MarmosetCredentials(dialog.username, dialog.password)
        // save username and password securely in persistent settings
        loginService.save(credentials)
        return credentials
    }

    /**
     * Uploads the submission zip file to the Marmoset server using the provided
     * credentials and assignment information. Displays a success dialog if the
     * upload completes successfully, or an error dialog if the upload fails.
     *
     * HTTP response codes are handled as follows:
     *   - 403: Displays an invalid username or password message.
     *   - 404: Displays an invalid course or assignment name message.
     *   - Other non-2xx codes: Displays a generic upload failure message.
     *
     * @param project        The current IntelliJ project.
     * @param zipFile        The zip file to upload.
     * @param credentials    A [MarmosetCredentials] containing the username and password.
     * @param assignmentInfo The parsed assignment info (course name, project number, semester).
     * @param config         The project configuration containing the submission URL.
     */
    private fun uploadSubmission(
        project: Project,
        zipFile: File,
        credentials: MarmosetCredentials,
        assignmentInfo: AssignmentInfo,
        config: ProjectConfig
    ) {
        try {
            UploadService(project).upload(zipFile, credentials, assignmentInfo, config.submissionUrl)
            Messages.showInfoMessage(
                project,
                SubmitterBundle.message("submitAction.submissionSuccessful"),
                SubmitterBundle.message("submitAction.submissionDialogTitle")
            )
        } catch (e: UploadException) {
            val message = when (e.responseCode) {
                403 -> SubmitterBundle.message("submitAction.error.403", credentials.username)
                404 -> SubmitterBundle.message(
                    "submitAction.error.404",
                    assignmentInfo.semester,
                    assignmentInfo.courseName,
                    assignmentInfo.projectNumber
                )

                else -> e.message ?: SubmitterBundle.message("submitAction.error.unknown")
            }
            Messages.showErrorDialog(
                project,
                SubmitterBundle.message("submitAction.submissionFailed", message),
                SubmitterBundle.message("submitAction.submissionErrorDialogTitle")
            )
        } catch (e: Exception) {
            Messages.showErrorDialog(
                project,
                SubmitterBundle.message(
                    "submitAction.submissionFailed",
                    SubmitterBundle.message(
                        "submitAction.error.network",
                        e.message ?: SubmitterBundle.message("submitAction.error.unknown")
                    )
                ),
                SubmitterBundle.message("submitAction.submissionErrorDialogTitle")
            )
        }
    }
}