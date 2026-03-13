package com.github.jmoscola.ycpcsmarmosetsubmitter.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
@State(
    name = "MarmosetSubmissionSettings",
    storages = [Storage("marmoset_submission.xml")]
)
class SubmissionSettings : PersistentStateComponent<SubmissionSettings.State> {

    data class State(
        var username: String = ""
    )

    private var state = State()

    override fun getState(): State {
        return state
    }

    override fun loadState(state: State) {
        this.state = state
    }

    companion object {
        fun getInstance(project: Project): SubmissionSettings {
            return project.getService(SubmissionSettings::class.java)
        }
    }
}