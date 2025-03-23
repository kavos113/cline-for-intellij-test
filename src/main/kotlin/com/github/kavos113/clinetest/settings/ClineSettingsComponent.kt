package com.github.kavos113.clinetest.settings

import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.util.ui.FormBuilder
import javax.swing.JPanel

class ClineSettingsComponent {
    val mainPanel: JPanel
    private val apiKeyField: JBPasswordField = JBPasswordField()

    init {
        mainPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("API Key:"), apiKeyField, 1, false)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    fun getApiKey(): String {
        return apiKeyField.password.toString()
    }

    fun setApiKey(apiKey: String) {
        apiKeyField.text = apiKey
    }
}