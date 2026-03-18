package edu.ycp.cs.marmosetsubmitter.dialog

import edu.ycp.cs.marmosetsubmitter.MarmosetSubmitterBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import java.awt.Dimension
import javax.swing.*

/**
 * Dialog that prompts the user for their Marmoset login credentials.
 * Previously saved credentials are passed in by the caller and automatically
 * pre-populated into the username and password fields when the dialog is
 * displayed. The dialog is fixed in size at 1.5x the default width to
 * provide adequate space for the input fields.
 *
 * @param project   The current IntelliJ project.
 * @param username  The previously saved username to pre-populate in the
 *                  username field, or null if no credentials have been saved.
 * @param password  The previously saved password to pre-populate in the
 *                  password field, or null if no credentials have been saved.
 * @see LoginCredentialsService
 */
class LoginDialog(private val project: Project, username: String?, password: String?) : DialogWrapper(project) {

    private val usernameField = JTextField()
    private val passwordField = JPasswordField()

    init {
        title = MarmosetSubmitterBundle.message("loginDialog.title")

        // Load saved credentials from persistent storage
//        val loginService = LoginCredentialsService(project)
        usernameField.text = username ?: ""
        passwordField.text = password ?: ""

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

    /**
     * Returns the current text in the username input field.
     *
     * @return The username entered by the user.
     */
    val username: String
        get() = usernameField.text

    /**
     * Returns the current text in the password input field as a [String].
     * Converts the [CharArray] returned by [JPasswordField.getPassword] to
     * a [String] for use in the submission workflow.
     *
     * @return The password entered by the user.
     */
    val password: String
        get() = String(passwordField.password)

    /**
     * Creates and returns the center panel of the dialog containing the
     * username and password input fields. The panel uses a vertical
     * [BoxLayout] with each field preceded by a descriptive label.
     *
     * This method is required by [DialogWrapper] and is called automatically
     * during dialog initialization.
     *
     * @return A [JComponent] containing the username and password input fields.
     */
    override fun createCenterPanel(): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.add(JLabel(MarmosetSubmitterBundle.message("loginDialog.username")))
        panel.add(usernameField)
        panel.add(JLabel(MarmosetSubmitterBundle.message("loginDialog.password")))
        panel.add(passwordField)
        return panel
    }
}
