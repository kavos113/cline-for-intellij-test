package com.github.kavos113.clinetest.settings

import com.github.kavos113.clinetest.ClineService
import com.intellij.openapi.options.Configurable
import javax.swing.JComponent


class ClineSettingsConfigurable : Configurable {

    private lateinit var settingsComponent: ClineSettingsComponent

    override fun createComponent(): JComponent {
        settingsComponent = ClineSettingsComponent()
        settingsComponent.setApiKey(ClineService.getSecret("apiKey"))
        return settingsComponent.mainPanel
    }

    override fun isModified(): Boolean {
        return settingsComponent.getApiKey() != ClineService.getSecret("apiKey")
    }

    override fun apply() {
        ClineService.storeSecret("apiKey", settingsComponent.getApiKey())
    }

    override fun getDisplayName(): String {
        return "Cline Settings"
    }

    override fun reset() {
        settingsComponent.setApiKey(ClineService.getSecret("apiKey"))
    }
}