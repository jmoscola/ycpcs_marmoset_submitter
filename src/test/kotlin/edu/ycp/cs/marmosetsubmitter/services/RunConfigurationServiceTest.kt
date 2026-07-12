package edu.ycp.cs.marmosetsubmitter.services

import com.intellij.execution.RunManager
import com.intellij.execution.configurations.UnknownConfigurationType
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class RunConfigurationServiceTest : BasePlatformTestCase() {

    override fun tearDown() {
        val runManager = RunManager.getInstance(project)
        runManager.selectedConfiguration = null
        runManager.allSettings.forEach { runManager.removeConfiguration(it) }
        super.tearDown()
    }

    // ── No run configuration selected ────────────────────────────────────────

    fun testGetSelectedRunConfigurationNameReturnsNullWhenNoneSelected() {
        // by default in a fresh test project no run configuration is selected
        val result = RunConfigurationService(project).getSelectedRunConfigurationName()

        assertNull(result)
    }

    // ── Run configuration selected ────────────────────────────────────────────

    fun testGetSelectedRunConfigurationNameReturnsCorrectName() {
        val runManager = RunManager.getInstance(project)
        val configurationType = UnknownConfigurationType.getInstance()
        val settings = runManager.createConfiguration(
            "DonQuixote",
            configurationType.configurationFactories.first()
        )
        runManager.addConfiguration(settings)
        runManager.selectedConfiguration = settings

        val result = RunConfigurationService(project).getSelectedRunConfigurationName()

        assertEquals("DonQuixote", result)
    }

    fun testGetSelectedRunConfigurationNameReturnsUpdatedNameAfterChange() {
        val runManager = RunManager.getInstance(project)
        val configurationType = UnknownConfigurationType.getInstance()

        val settings1 = runManager.createConfiguration(
            "DonQuixote",
            configurationType.configurationFactories.first()
        )
        val settings2 = runManager.createConfiguration(
            "RollinTrain_MS1",
            configurationType.configurationFactories.first()
        )
        runManager.addConfiguration(settings1)
        runManager.addConfiguration(settings2)

        // select first config
        runManager.selectedConfiguration = settings1
        assertEquals("DonQuixote",
            RunConfigurationService(project).getSelectedRunConfigurationName())

        // switch to second config
        runManager.selectedConfiguration = settings2
        assertEquals("RollinTrain_MS1",
            RunConfigurationService(project).getSelectedRunConfigurationName())
    }

    fun testGetSelectedRunConfigurationNamePreservesCase() {
        val runManager = RunManager.getInstance(project)
        val configurationType = UnknownConfigurationType.getInstance()
        val settings = runManager.createConfiguration(
            "CS370_Assign01_Fa25",
            configurationType.configurationFactories.first()
        )
        runManager.addConfiguration(settings)
        runManager.selectedConfiguration = settings

        val result = RunConfigurationService(project).getSelectedRunConfigurationName()

        assertEquals("CS370_Assign01_Fa25", result)
    }

    fun testGetSelectedRunConfigurationNameHandlesNameWithSpaces() {
        val runManager = RunManager.getInstance(project)
        val configurationType = UnknownConfigurationType.getInstance()
        val settings = runManager.createConfiguration(
            "Don Quixote Assignment 01",
            configurationType.configurationFactories.first()
        )
        runManager.addConfiguration(settings)
        runManager.selectedConfiguration = settings

        val result = RunConfigurationService(project).getSelectedRunConfigurationName()

        assertEquals("Don Quixote Assignment 01", result)
    }
}