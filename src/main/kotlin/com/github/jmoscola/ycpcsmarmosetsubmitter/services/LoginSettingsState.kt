package com.github.jmoscola.ycpcsmarmosetsubmitter.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.NotNull

@State(
    name = "LoginSettingsState",
    storages = [Storage("login_settings.xml")]
)
@Service(Service.Level.PROJECT)
class LoginSettingsState : PersistentStateComponent<LoginSettingsState.State> {

    data class State(
        var username: String = "",
        var password: String = ""
    )

    private var state = State()

    var username: String
        get() = state.username
        set(value) { state.username = value }

    var password: String
        get() = state.password
        set(value) { state.password = value }

    override fun getState(): State = state

    override fun loadState(@NotNull state: State) {
        this.state = state
    }

    companion object {
        fun getInstance(project: Project): LoginSettingsState =
            project.getService(LoginSettingsState::class.java)
    }
}