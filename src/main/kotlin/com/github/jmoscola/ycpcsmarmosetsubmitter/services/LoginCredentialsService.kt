package com.github.jmoscola.ycpcsmarmosetsubmitter.services

import com.github.jmoscola.ycpcsmarmosetsubmitter.SubmitterBundle
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.project.Project

class LoginCredentialsService(private val project: Project) {

    // Create a unique key for this plugin’s storage
    private val credentialAttributes =
        CredentialAttributes(SubmitterBundle.message("credentialAttributes.serviceName"))

    fun save(username: String, password: String) {
        val creds = Credentials(username, password.toCharArray())
        PasswordSafe.instance.set(credentialAttributes, creds)
    }

    fun getUsername(): String? =
        PasswordSafe.instance.get(credentialAttributes)?.userName

    fun getPassword(): String? =
        PasswordSafe.instance.get(credentialAttributes)?.getPasswordAsString()
}