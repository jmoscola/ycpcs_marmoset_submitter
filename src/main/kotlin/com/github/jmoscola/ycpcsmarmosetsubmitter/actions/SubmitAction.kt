package com.github.jmoscola.ycpcsmarmosetsubmitter.actions

import com.github.jmoscola.ycpcsmarmosetsubmitter.SubmitterBundle
import com.github.jmoscola.ycpcsmarmosetsubmitter.dialog.LoginDialog
import com.github.jmoscola.ycpcsmarmosetsubmitter.services.LoginCredentialsService
import com.github.jmoscola.ycpcsmarmosetsubmitter.services.ZipFilesService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

class SubmitAction : AnAction(SubmitterBundle.message("submitAction.text")) {

    override fun actionPerformed(e: AnActionEvent) {

        val project: Project = e.project ?: return

        // step 1 - zip files
        val zipService = ZipFilesService(project)

        try {
            val zipFile = zipService.zipProject(
                zipFilename = "submission.zip",
                allowedExtensions = setOf("h", "cpp", "java"),
                excludedFilenames = setOf(
                    "Flags.h",
                    "tests.cpp"
                )
            )
            // remove this later
            Messages.showInfoMessage(
                project,
                "Created zip file:\n${zipFile.absolutePath}",
                "Zip Complete"
            )
        } catch (e: ProcessCanceledException) {
            Messages.showInfoMessage(project, "Submission canceled.", "Submit")
            throw e
        }

        // step 2 — prompt user for login info
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

        Messages.showInfoMessage(
            project,
            "Ready to submit as $username",
            "Submit"
        )

        // step 3 - save username and password securely in persistent settings
        val loginService = LoginCredentialsService(project)
        loginService.save(username, password)

        // Step 4 — upload (we will implement next)
        //
        //

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