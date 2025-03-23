package com.github.kavos113.clinetest.settings

import com.intellij.ui.components.JBCheckBox
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
            .addComponent(JBCheckBox("Show API Key", false).apply {
                addActionListener {
                    if (isSelected) {
                        showPassword()
                    } else {
                        hidePassword()
                    }
                }
            })
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    fun getApiKey(): String {
        return String(apiKeyField.password)
    }

    fun setApiKey(apiKey: String) {
        apiKeyField.text = apiKey
    }

    private fun showPassword() {
        apiKeyField.echoChar = 0.toChar()
    }

    private fun hidePassword() {
        apiKeyField.echoChar = '*'
    }
}