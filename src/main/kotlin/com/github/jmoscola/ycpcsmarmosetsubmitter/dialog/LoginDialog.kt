package com.github.jmoscola.ycpcsmarmosetsubmitter.dialog

import com.github.jmoscola.ycpcsmarmosetsubmitter.SubmitterBundle
import com.github.jmoscola.ycpcsmarmosetsubmitter.settings.LoginSettingsState
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import javax.swing.*

class LoginDialog(private val project: Project) : DialogWrapper(project) {

    private val usernameField = JTextField()
    private val passwordField = JPasswordField()

    init {
        title = SubmitterBundle.message("login.title")

        // Load saved credentials from persistent storage
        val state = LoginSettingsState.getInstance(project)
        usernameField.text = state.username
        passwordField.text = state.password

        init() // Required by DialogWrapper
    }

    val username: String
        get() = usernameField.text

    val password: String
        get() = String(passwordField.password)

    override fun createCenterPanel(): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.add(JLabel(SubmitterBundle.message("login.username")))
        panel.add(usernameField)
        panel.add(JLabel(SubmitterBundle.message("login.password")))
        panel.add(passwordField)
        return panel
    }
}
