package com.github.jmoscola.ycpcsmarmosetsubmitter.actions

import com.github.jmoscola.ycpcsmarmosetsubmitter.SubmitterBundle
import com.github.jmoscola.ycpcsmarmosetsubmitter.dialog.LoginDialog
import com.github.jmoscola.ycpcsmarmosetsubmitter.services.AssignmentInfo
import com.github.jmoscola.ycpcsmarmosetsubmitter.services.CMakeAssignmentInfoService
import com.github.jmoscola.ycpcsmarmosetsubmitter.services.LoginCredentialsService
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

        val project: Project = e.project ?: return
        var assignmentInfo: AssignmentInfo? = null

        /** ************************************************************************
         * step 1 - extract assignment info from CMake file
         ************************************************************************* */
        val cmakeService = CMakeAssignmentInfoService(project)
        try {
            // TODO: get the CMakeLists file name from a new settings file
            assignmentInfo = cmakeService.parse("CMakeLists.assignment_info.txt")
//            Messages.showInfoMessage(
//                project,
//                "COURSE_NAME: ${assignmentInfo.courseName} \nTERM: ${assignmentInfo.term} \nSEMESTER: ${assignmentInfo.semester} \nPROJECT_NUM: ${assignmentInfo.projectNumber}",
//                SubmitterBundle.message("submitAction.submissionDialogTitle")
//            )
        } catch (e: IllegalStateException) {
            Messages.showErrorDialog(
                project,
                SubmitterBundle.message(
                    "submitAction.submissionFailed",
                    e.message ?: SubmitterBundle.message("submitAction.error.unknown")),
                SubmitterBundle.message("submitAction.submissionErrorDialogTitle")
            )
            throw e
        }


        /** ************************************************************************
         * step 2 - create zip file containing project files
         ************************************************************************* */
        val zipService = ZipFilesService(project)
        var zipFile: File? = null

        try {
            zipFile = zipService.zipProject(
                // TODO: put zip file suffix name in .settings file
                zipFilename = "${assignmentInfo.projectNumber}_submission.zip",
                // TODO: put allowedExtensions and Filenames in .settings file
                allowedExtensions = setOf("h", "cpp", "java"),
                excludedFilenames = setOf(
                    "Flags.h",
                    "tests.cpp"
                )
            )
        } catch (e: ProcessCanceledException) {
            Messages.showInfoMessage(
                project,
                SubmitterBundle.message("submitAction.submissionCanceled"),
                SubmitterBundle.message("submitAction.submissionDialogTitle")
            )
            throw e
        }


        /** ************************************************************************
         * step 3 — prompt user for Marmoset login info
         ************************************************************************* */
        val dialog = LoginDialog(project)

        if (!dialog.showAndGet()) {
            // user canceled login
            Messages.showInfoMessage(
                project,
                SubmitterBundle.message("submitAction.submissionCanceled"),
                SubmitterBundle.message("submitAction.submissionDialogTitle")
            )
            return
        }

        val username = dialog.username
        val password = dialog.password

//        Messages.showInfoMessage(
//            project,
//            "Ready to submit as $username",
//            SubmitterBundle.message("submitAction.submissionDialogTitle")
//        )


        /** ************************************************************************
         * step 4 - save username and password securely in persistent settings
         ************************************************************************* */
        val loginService = LoginCredentialsService(project)
        loginService.save(username, password)


        /** ************************************************************************
         * step 5 - upload zip file to Marmoset
         ************************************************************************* */
        val uploadService = UploadService(project)

        try {
            uploadService.upload(zipFile, username, password, assignmentInfo)
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
}