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


class SubmitAction : AnAction(SubmitterBundle.message("submitAction.text")) {

    override fun actionPerformed(e: AnActionEvent) {

        val project: Project = e.project ?: return
        var assignmentInfo: AssignmentInfo? = null

        /** ************************************************************************
         * step 1 - extract assignment info from CMake file
         ************************************************************************* */
        val cmakeService = CMakeAssignmentInfoService(project)
        try {
            assignmentInfo = cmakeService.parse("CMakeLists.assignment_info.txt")
//            Messages.showInfoMessage(
//                project,
//                "COURSE_NAME: ${assignmentInfo.courseName} \nTERM: ${assignmentInfo.term} \nSEMESTER: ${assignmentInfo.semester} \nPROJECT_NUM: ${assignmentInfo.projectNumber}",
//                "Assignment Info"
//            )
        } catch (e: IllegalStateException) {
            Messages.showErrorDialog(project, "${e.message}", "Submission Failed")
            throw e
        }


        /** ************************************************************************
         * step 2 - create zip file containing project files
         ************************************************************************* */
        val zipService = ZipFilesService(project)
        var zipFile: File? = null

        try {
            zipFile = zipService.zipProject(
                zipFilename = "submission.zip",
                allowedExtensions = setOf("h", "cpp", "java"),
                excludedFilenames = setOf(
                    "Flags.h",
                    "tests.cpp"
                )
            )
        } catch (e: ProcessCanceledException) {
            Messages.showInfoMessage(project, "Submission canceled.", "Submit")
            throw e
        }

//        // remove this later
//        Messages.showInfoMessage(
//            project,
//            "Created zip file:\n${zipFile.absolutePath}",
//            "Zip Complete"
//        )


        /** ************************************************************************
         * step 3 — prompt user for Marmoset login info
         ************************************************************************* */
        val dialog = LoginDialog(project)

        if (!dialog.showAndGet()) {
            // user canceled login
            Messages.showInfoMessage(
                project,
                "Submission cancelled.",
                "Submit"
            )
            return
        }

        val username = dialog.username
        val password = dialog.password

//        Messages.showInfoMessage(
//            project,
//            "Ready to submit as $username",
//            "Submit"
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
            Messages.showInfoMessage(project, "Submission successful!", "Marmoset Submission")
        } catch (e: UploadException) {
            val message = when (e.responseCode) {
                403  -> "<html>" +
                            "Invalid username or password.<br>" +
                            "&nbsp;&nbsp;&nbsp;&nbsp;&#8226;&nbsp;Username: $username" +
                        "</html>"
                404  -> "<html>" +
                            "Invalid semester, course, or assignment name.<br>" +
                            "Please see your instructor.<br>" +
                            "&nbsp;&nbsp;&nbsp;&nbsp;&#8226;&nbsp;Semester: ${assignmentInfo.semester}<br>" +
                            "&nbsp;&nbsp;&nbsp;&nbsp;&#8226;&nbsp;Course: ${assignmentInfo.courseName}<br>" +
                            "&nbsp;&nbsp;&nbsp;&nbsp;&#8226;&nbsp;Assignment: ${assignmentInfo.projectNumber}" +
                        "</html>"
                else -> e.message ?: "Unknown error."
            }
            Messages.showErrorDialog(project, message, "Submission Failed")
        } catch (e: Exception) {
            Messages.showErrorDialog(project, "Network error: ${e.message}", "Submission Failed")
        }


        // TODO: move all strings into bundle

        // TODO: change submission.zip filename to my new format that includes the assignment number
        // TODO: maybe change the Makefiles again so they call the submission file "submission" and not "solution"
        // TODO:   the above will require changing the markdown files (assignment writeups
        // TODO:   the above will require updating CS420 Makefiles and markdown files too
        // TODO:   the above won't require, but I should probably change the ECE260 Makefiles too

//        Messages.showErrorDialog(
//            project,
//            "Authentication failed.",
//            "Submit Error"
//        )
//        } catch (e: ProcessCanceledException) {
//            // user canceled the zipping, terminate the action
//            Messages.showInfoMessage(project, "Submission canceled.", "Submit")
//            throw e
//        } catch (e: Exception) {
//            Messages.showErrorDialog(project, "Submission failed: ${e.message}", "Submit")
//        }
    }
}