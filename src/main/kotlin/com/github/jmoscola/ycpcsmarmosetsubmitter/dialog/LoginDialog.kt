package com.github.jmoscola.ycpcsmarmosetsubmitter.dialog

import com.github.jmoscola.ycpcsmarmosetsubmitter.SubmitterBundle
import com.github.jmoscola.ycpcsmarmosetsubmitter.services.LoginCredentialsService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import java.awt.Dimension
import javax.swing.*

class LoginDialog(private val project: Project) : DialogWrapper(project) {

    private val usernameField = JTextField()
    private val passwordField = JPasswordField()

    init {
        title = SubmitterBundle.message("loginDialog.title")

        // Load saved credentials from persistent storage
        val loginService = LoginCredentialsService(project)
        usernameField.text = loginService.getUsername()
        passwordField.text = loginService.getPassword() ?: ""

        init() // Required by DialogWrapper

        // Make dialog 1.5x wider than the default width and fixed size
        val defaultSize = contentPanel.preferredSize
        val fixedWidth = (defaultSize.width * 1.5).toInt()
        val fixedHeight = defaultSize.height

        (window as? JDialog)?.let { dialog ->
            dialog.setSize(fixedWidth, fixedHeight)
            dialog.minimumSize = Dimension(fixedWidth, fixedHeight)
            dialog.maximumSize = Dimension(fixedWidth, fixedHeight)
            dialog.isResizable = false
        }
    }

    val username: String
        get() = usernameField.text

    val password: String
        get() = String(passwordField.password)

    override fun createCenterPanel(): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.add(JLabel(SubmitterBundle.message("loginDialog.username")))
        panel.add(usernameField)
        panel.add(JLabel(SubmitterBundle.message("loginDialog.password")))
        panel.add(passwordField)
        return panel
    }
}
