package com.github.jmoscola.ycpcsmarmosetsubmitter.actions

import com.github.jmoscola.ycpcsmarmosetsubmitter.dialog.LoginDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.github.jmoscola.ycpcsmarmosetsubmitter.SubmitterBundle
import com.github.jmoscola.ycpcsmarmosetsubmitter.services.LoginCredentialsService

class LoginAction : AnAction(SubmitterBundle.message("plugin.actionText")) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val dialog = LoginDialog(project)

        if (dialog.showAndGet()) {
            val username = dialog.username
            val password = dialog.password

            Messages.showInfoMessage(
                "Username: $username\nPassword length: ${password.length}",
                "Login Received"
            )

            // Save username and password securely in persistent settings
            val loginService = LoginCredentialsService(project)
            loginService.save(username, password)
        }
    }
}
