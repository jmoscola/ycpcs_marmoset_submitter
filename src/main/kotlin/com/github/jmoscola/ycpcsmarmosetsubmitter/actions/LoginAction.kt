package com.github.jmoscola.ycpcsmarmosetsubmitter.actions

import com.github.jmoscola.ycpcsmarmosetsubmitter.dialog.LoginDialog
import com.github.jmoscola.ycpcsmarmosetsubmitter.settings.LoginSettingsState
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.github.jmoscola.ycpcsmarmosetsubmitter.SubmitterBundle

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

            // Save credentials persistently for the project
            val state = LoginSettingsState.getInstance(project)
            state.username = username
            state.password = password
        }
    }
}
