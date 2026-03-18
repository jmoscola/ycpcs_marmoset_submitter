package com.github.jmoscola.ycpcsmarmosetsubmitter.services

import com.github.jmoscola.ycpcsmarmosetsubmitter.SubmitterBundle
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.project.Project

/**
 * Service that securely stores and retrieves Marmoset login credentials
 * using the IntelliJ Platform's [PasswordSafe] API. Credentials are stored
 * in the platform's secure credential store, which uses the operating
 * system's native keychain on macOS and Windows, and a KeePass-based
 * store on Linux.
 *
 * Credentials are associated with a unique service name defined in the
 * plugin's resource bundle, ensuring they do not conflict with credentials
 * stored by other plugins or applications.
 *
 * @param project The current IntelliJ project.
 * @see PasswordSafe
 * @see CredentialAttributes
 */
class LoginCredentialsService(private val project: Project) {

    // Create a unique key for this plugin’s storage
    private val credentialAttributes =
        CredentialAttributes(SubmitterBundle.message("credentialAttributes.serviceName"))

    /**
     * Securely saves the provided credentials to the IntelliJ Platform's
     * [PasswordSafe] credential store. If credentials have previously been
     * saved, they are overwritten.
     *
     * @param credentials A [MarmosetCredentials] containing the username and
     *                    password to save.
     */
    fun save(credentials: MarmosetCredentials) {
        val creds = Credentials(
            credentials.username,
            credentials.password.toCharArray()
        )
        PasswordSafe.instance.set(credentialAttributes, creds)
    }

    /**
     * Retrieves the saved Marmoset username from the IntelliJ Platform's
     * [PasswordSafe] credential store.
     *
     * @return The saved username, or null if no credentials have been saved.
     */
    fun getUsername(): String? =
        PasswordSafe.instance.get(credentialAttributes)?.userName

    /**
     * Retrieves the saved Marmoset password from the IntelliJ Platform's
     * [PasswordSafe] credential store.
     *
     * @return The saved password, or null if no credentials have been saved.
     */
    fun getPassword(): String? =
        PasswordSafe.instance.get(credentialAttributes)?.getPasswordAsString()
}