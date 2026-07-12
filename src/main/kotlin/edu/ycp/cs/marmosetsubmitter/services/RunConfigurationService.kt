package edu.ycp.cs.marmosetsubmitter.services

import com.intellij.execution.RunManager
import com.intellij.openapi.project.Project

/**
 * Service that queries the currently selected run configuration
 * from the JetBrains IDE's [RunManager]. Used in run
 * configuration based submission mode to determine which
 * assignment's properties to load.
 *
 * @param project The current project.
 * @see RunManager
 */
class RunConfigurationService(private val project: Project) {

    /**
     * Returns the name of the currently selected run configuration,
     * or null if no run configuration is selected or the IDE does
     * not support run configurations.
     *
     * @return The name of the selected run configuration, or null.
     */
    fun getSelectedRunConfigurationName(): String? {
        return try {
            RunManager.getInstance(project).selectedConfiguration?.name
        } catch (e: Exception) {
            null
        }
    }
}