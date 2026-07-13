package edu.ycp.cs.marmosetsubmitter.actions

import edu.ycp.cs.marmosetsubmitter.MarmosetSubmitterBundle
import edu.ycp.cs.marmosetsubmitter.dialog.LoginDialog
import edu.ycp.cs.marmosetsubmitter.services.AssignmentInfo
import edu.ycp.cs.marmosetsubmitter.services.CMakeAssignmentInfoService
import edu.ycp.cs.marmosetsubmitter.services.AssignmentMappingService
import edu.ycp.cs.marmosetsubmitter.services.RunConfigurationService
import edu.ycp.cs.marmosetsubmitter.services.LoginCredentialsService
import edu.ycp.cs.marmosetsubmitter.services.MarmosetCredentials
import edu.ycp.cs.marmosetsubmitter.services.ProjectConfig
import edu.ycp.cs.marmosetsubmitter.services.SubmitterConfigService
import edu.ycp.cs.marmosetsubmitter.services.UploadException
import edu.ycp.cs.marmosetsubmitter.services.UploadService
import edu.ycp.cs.marmosetsubmitter.services.ZipFilesService
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
 *   1. Load and validate the project configuration (marmoset_submitter.properties).
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
     *   1. [loadConfig]         - Load marmoset_submitter.properties from the project root.
     *   2. [loadAssignmentInfo] - Parse the .cmake assignment info (or mapping) file.
     *   3. [createZip]          - Zip the project files for submission.
     *   4. [getCredentials]     - Prompt the user for their Marmoset credentials.
     *   5. [uploadSubmission]   - Upload the zip file to the Marmoset server.
     *
     * @param e The action event providing context, including the current project.
     */
    override fun actionPerformed(e: AnActionEvent) {
        val project        = e.project ?: return
        val config         = loadConfig(project) ?: return                         // configure the plugin
        val assignmentInfo = loadAssignmentInfo(project, config) ?: return         // extract assignment info (or mapping) from .cmake file
        val zipFile        = createZip(project, config, assignmentInfo) ?: return  // create zip file
        val credentials    = getCredentials(project) ?: return                     // prompt user for credentials
        uploadSubmission(project, zipFile, credentials, assignmentInfo, config)    // upload zip file
    }

    /**
     * Loads and parses the plugin configuration file (marmoset_submitter.properties)
     * from the project root directory using [SubmitterConfigService].
     *
     * @param project The current project.
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
                MarmosetSubmitterBundle.message(
                    "submitAction.submissionFailed",
                    e.message ?: MarmosetSubmitterBundle.message("submitAction.error.unknown")
                ),
                MarmosetSubmitterBundle.message("submitAction.submissionErrorDialogTitle")
            )
            null
        }
    }

    /**
     * Resolves the assignment info filename and submission directory for the
     * current submission. The resolution strategy depends on the value of
     * [ProjectConfig.useRunConfigurationBasedSubmissions]:
     *
     * In Mode 1 ([ProjectConfig.useRunConfigurationBasedSubmissions] = false):
     * Returns [ProjectConfig.assignmentInfoFilename] directly as the assignment
     * info filename, with the project root as the submission directory. The
     * run configuration is ignored entirely.
     *
     * In Mode 2 ([ProjectConfig.useRunConfigurationBasedSubmissions] = true):
     * Reads the currently selected run configuration name, looks it up in the
     * mapping file specified by [ProjectConfig.assignmentInfoFilename], and
     * returns the resolved assignment info file path. The submission directory
     * is derived from the resolved path. If the path includes a subdirectory
     * component (e.g. "CS370_Assign01_Fa25/assignment_info.cmake"), the
     * submission directory is that subdirectory. If the path is a filename
     * with no subdirectory component (e.g. "assign01_info.cmake"), the
     * submission directory is the project root.
     *
     * @param project The current project.
     * @param config  The project configuration.
     * @return A [Pair] of the resolved assignment info filename and the
     *         resolved submission [File] directory, or null if resolution
     *         failed (e.g. no run configuration selected in Mode 2, or
     *         the run configuration name was not found in the mapping file).
     */
    private fun resolveAssignmentInfoFilename(
        project: Project,
        config: ProjectConfig
    ): Pair<String, File>? {

        val projectDir = File(project.basePath!!)

        if (!config.useRunConfigurationBasedSubmissions) {
            // Mode 1 -- use assignmentInfoFilename directly; submit from project root
            return Pair(config.assignmentInfoFilename, projectDir)
        }

        // Mode 2 -- resolve via mapping file and selected run configuration
        val runConfigName = RunConfigurationService(project)
            .getSelectedRunConfigurationName()

        if (runConfigName == null) {
            Messages.showErrorDialog(
                project,
                MarmosetSubmitterBundle.message("submitAction.error.noRunConfigSelected"),
                MarmosetSubmitterBundle.message("submitAction.submissionErrorDialogTitle")
            )
            return null
        }

        return try {
            val assignmentInfoFilename = AssignmentMappingService(project).resolve(
                config.assignmentInfoFilename,
                runConfigName
            )
            // Derive the submission directory from the resolved path.
            // If the path includes a subdirectory component, use that subdirectory.
            // If the path is a filename with no subdirectory component (no '/'),
            // fall back to the project root directory.
            val subDirectory = File(
                projectDir,
                assignmentInfoFilename
                    .substringBeforeLast("/", missingDelimiterValue = "")
                    .ifEmpty { "." }
            )
            Pair(assignmentInfoFilename, subDirectory)
        } catch (e: ProcessCanceledException) {
            throw e
        } catch (e: Exception) {
            Messages.showErrorDialog(
                project,
                MarmosetSubmitterBundle.message(
                    "submitAction.submissionFailed",
                    e.message ?: MarmosetSubmitterBundle.message("submitAction.error.unknown")
                ),
                MarmosetSubmitterBundle.message("submitAction.submissionErrorDialogTitle")
            )
            null
        }
    }

    /**
     * Resolves and parses the assignment info file for the current submission.
     * Delegates to [resolveAssignmentInfoFilename] to determine the correct
     * assignment info file path and submission directory based on the current
     * mode, then delegates to [CMakeAssignmentInfoService] to parse the file.
     *
     * In Mode 1, parses the file specified directly by
     * [ProjectConfig.assignmentInfoFilename] in the project root directory.
     *
     * In Mode 2, resolves the assignment info file path via the mapping file
     * and the currently selected run configuration name, then parses the
     * resolved file. The resolved file may be located in a subdirectory of
     * the project root or in the project root itself, depending on how the
     * mapping file is structured.
     *
     * @param project The current project.
     * @param config  The project configuration containing the assignment info
     *                filename (or mapping filename in Mode 2), the
     *                [ProjectConfig.useRunConfigurationBasedSubmissions] flag,
     *                and the [ProjectConfig.useAssignmentInfoYear] flag.
     * @return An [AssignmentInfo] containing the parsed course name, term,
     *         project number, semester, and submission directory, or null if
     *         the file could not be resolved or parsed.
     */
    private fun loadAssignmentInfo(
        project: Project,
        config: ProjectConfig
    ): AssignmentInfo? {
        val (assignmentInfoFilename, submissionDir) =
            resolveAssignmentInfoFilename(project, config) ?: return null

        return try {
            CMakeAssignmentInfoService(project).parse(
                filename              = assignmentInfoFilename,
                useAssignmentInfoYear = config.useAssignmentInfoYear,
                submissionDir         = submissionDir
            )
        } catch (e: ProcessCanceledException) {
            throw e
        } catch (e: Exception) {
            Messages.showErrorDialog(
                project,
                MarmosetSubmitterBundle.message(
                    "submitAction.submissionFailed",
                    e.message ?: MarmosetSubmitterBundle.message("submitAction.error.unknown")
                ),
                MarmosetSubmitterBundle.message("submitAction.submissionErrorDialogTitle")
            )
            null
        }
    }

    /**
     * Zips the project files into a single zip file for submission using
     * [ZipFilesService]. The zip filename is constructed from the project
     * number and the zip filename suffix specified in the project configuration.
     * Allowed filenames, allowed extensions, excluded filenames, excluded
     * directories, and excluded extensions are all sourced from the project
     * configuration.
     *
     * @param project        The current project.
     * @param config         The project configuration containing zip options and exclusion rules.
     * @param assignmentInfo The parsed assignment info used to construct the zip filename.
     * @return A [File] reference to the created zip file, or null if the operation
     *         failed or was canceled by the user.
     */
    private fun createZip(
        project: Project,
        config: ProjectConfig,
        assignmentInfo: AssignmentInfo
    ): File? {
        return try {
            ZipFilesService(project).zipProject(
                zipFilename         = "${assignmentInfo.projectNumber}${config.zipFilenameSuffix}.zip",
                allowedFilenames    = config.allowedFilenames,
                allowedExtensions   = config.allowedExtensions,
                excludedExtensions  = config.excludedExtensions,
                excludedFilenames   = config.excludedFilenames,
                excludedDirectories = config.excludedDirectories,
                submissionDir       = assignmentInfo.submissionDir
            )
        } catch (e: ProcessCanceledException) {
            // ProcessCanceledException must be rethrown so the IntelliJ platform
            // can handle the cancellation correctly. A cancellation is not an error,
            // so an informational dialog is shown before rethrowing.
            Messages.showInfoMessage(
                project,
                MarmosetSubmitterBundle.message("submitAction.submissionCanceled"),
                MarmosetSubmitterBundle.message("submitAction.submissionDialogTitle")
            )
            throw e
        } catch (e: Exception) {
            // Catch any remaining exceptions from ZipFilesService, such as
            // IOException caused by disk write failures, and display the
            // error message to the user.
            Messages.showErrorDialog(
                project,
                e.message,
                MarmosetSubmitterBundle.message("submitAction.submissionErrorDialogTitle")
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
     * @param project The current project.
     * @return A [MarmosetCredentials] containing the username and password if
     *         the user confirmed the dialog, or null if the user canceled.
     */
    private fun getCredentials(project: Project): MarmosetCredentials? {
        val loginService = LoginCredentialsService()
        val dialog = LoginDialog(project, loginService.getUsername(), loginService.getPassword())
        if (!dialog.showAndGet()) {
            Messages.showInfoMessage(
                project,
                MarmosetSubmitterBundle.message("submitAction.submissionCanceled"),
                MarmosetSubmitterBundle.message("submitAction.submissionDialogTitle")
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
     * @param project        The current project.
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
            UploadService().upload(zipFile, credentials, assignmentInfo, config.submissionUrl)
            Messages.showInfoMessage(
                project,
                MarmosetSubmitterBundle.message("submitAction.submissionSuccessful", assignmentInfo.projectNumber),
                MarmosetSubmitterBundle.message("submitAction.submissionDialogTitle")
            )
        } catch (e: UploadException) {
            val message = when (e.responseCode) {
                403 -> MarmosetSubmitterBundle.message("submitAction.error.403", credentials.username)
                404 -> MarmosetSubmitterBundle.message(
                    "submitAction.error.404",
                    assignmentInfo.semester,
                    assignmentInfo.courseName,
                    assignmentInfo.projectNumber
                )

                else -> e.message ?: MarmosetSubmitterBundle.message("submitAction.error.unknown")
            }
            Messages.showErrorDialog(
                project,
                MarmosetSubmitterBundle.message("submitAction.submissionFailed", message),
                MarmosetSubmitterBundle.message("submitAction.submissionErrorDialogTitle")
            )
        } catch (e: Exception) {
            Messages.showErrorDialog(
                project,
                MarmosetSubmitterBundle.message(
                    "submitAction.submissionFailed",
                    MarmosetSubmitterBundle.message(
                        "submitAction.error.network",
                        e.message ?: MarmosetSubmitterBundle.message("submitAction.error.unknown")
                    )
                ),
                MarmosetSubmitterBundle.message("submitAction.submissionErrorDialogTitle")
            )
        }
    }
}