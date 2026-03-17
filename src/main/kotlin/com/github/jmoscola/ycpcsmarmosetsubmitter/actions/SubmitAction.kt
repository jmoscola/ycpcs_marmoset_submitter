package com.github.jmoscola.ycpcsmarmosetsubmitter.actions

import com.github.jmoscola.ycpcsmarmosetsubmitter.SubmitterBundle
import com.github.jmoscola.ycpcsmarmosetsubmitter.dialog.LoginDialog
import com.github.jmoscola.ycpcsmarmosetsubmitter.services.AssignmentInfo
import com.github.jmoscola.ycpcsmarmosetsubmitter.services.CMakeAssignmentInfoService
import com.github.jmoscola.ycpcsmarmosetsubmitter.services.LoginCredentialsService
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


class SubmitAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project        = e.project ?: return
        val config         = loadConfig(project)         ?: return
        val assignmentInfo = loadAssignmentInfo(project, config) ?: return
        val zipFile        = createZip(project, config, assignmentInfo) ?: return
        val credentials    = getCredentials(project)     ?: return
        uploadSubmission(project, zipFile, credentials, assignmentInfo, config)
    }

}


/** ************************************************************************
 * step 1 - configure the plugin settings by reading the .properties file
 ************************************************************************* */
private fun loadConfig(project: Project): ProjectConfig? {
    return try {
        SubmitterConfigService(project).load()
    } catch (e: IllegalStateException) {
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


/** ************************************************************************
 * step 2 - extract assignment info from CMake file
 ************************************************************************* */
private fun loadAssignmentInfo(project: Project, config: ProjectConfig): AssignmentInfo? {
    return try {
        CMakeAssignmentInfoService(project).parse(config.assignmentInfoFilename)
    } catch (e: IllegalStateException) {
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


/** ************************************************************************
 * step 3 - create zip file containing project files
 ************************************************************************* */
private fun createZip(project: Project, config: ProjectConfig, assignmentInfo: AssignmentInfo): File? {
    return try {
        ZipFilesService(project).zipProject(
            zipFilename       = "${assignmentInfo.projectNumber}${config.zipFilenameSuffix}.zip",
            allowedExtensions = config.allowedExtensions,
            excludedFilenames = config.excludedFilenames,
            excludedDirectories = config.excludedDirectories,
            excludedExtensions = config.excludedExtensions
        )
    } catch (e: ProcessCanceledException) {
        Messages.showInfoMessage(
            project,
            SubmitterBundle.message("submitAction.submissionCanceled"),
            SubmitterBundle.message("submitAction.submissionDialogTitle")
        )
        throw e
    } catch (e: Exception) {
        Messages.showErrorDialog(
            project,
            e.message,
            SubmitterBundle.message("submitAction.submissionErrorDialogTitle")
        )
        null
    }
}


/** ************************************************************************
 * step 4 — prompt user for Marmoset login info
 ************************************************************************* */
private fun getCredentials(project: Project): Pair<String, String>? {
    val dialog = LoginDialog(project)
    if (!dialog.showAndGet()) {
        Messages.showInfoMessage(
            project,
            SubmitterBundle.message("submitAction.submissionCanceled"),
            SubmitterBundle.message("submitAction.submissionDialogTitle")
        )
        return null
    }
    // save username and password securely in persistent settings
    LoginCredentialsService(project).save(dialog.username, dialog.password)
    return Pair(dialog.username, dialog.password)
}


/** ************************************************************************
 * step 5 - upload zip file to Marmoset
 ************************************************************************* */
private fun uploadSubmission(project: Project, zipFile: File, credentials: Pair<String, String>, assignmentInfo: AssignmentInfo, config: ProjectConfig) {
    val (username, password) = credentials
    try {
        UploadService(project).upload(zipFile, username, password, assignmentInfo, config.submissionUrl)
        Messages.showInfoMessage(
            project,
            SubmitterBundle.message("submitAction.submissionSuccessful"),
            SubmitterBundle.message("submitAction.submissionDialogTitle")
        )
    } catch (e: UploadException) {
        val message = when (e.responseCode) {
            403  -> SubmitterBundle.message("submitAction.error.403", username)
            404  -> SubmitterBundle.message("submitAction.error.404",
                assignmentInfo.semester,
                assignmentInfo.courseName,
                assignmentInfo.projectNumber)
            else -> e.message ?: SubmitterBundle.message("submitAction.error.unknown")
        }
        Messages.showErrorDialog(
            project,
            SubmitterBundle.message("submitAction.submissionFailed", message),
            SubmitterBundle.message("submitAction.submissionErrorDialogTitle"))
    } catch (e: Exception) {
        Messages.showErrorDialog(
            project,
            SubmitterBundle.message("submitAction.submissionFailed",
                SubmitterBundle.message(
                    "submitAction.error.network",
                    e.message ?: SubmitterBundle.message("submitAction.error.unknown"))
            ),
            SubmitterBundle.message("submitAction.submissionErrorDialogTitle")
        )
    }
}
