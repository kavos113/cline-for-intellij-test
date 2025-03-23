package com.github.kavos113.clinetest.settings

import com.github.kavos113.clinetest.DEFAULT_MAX_REQUESTS_PER_TASK
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import javax.swing.JPanel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class ClineSettingsComponent {
    val mainPanel: JPanel
    private val apiKeyField: JBPasswordField = JBPasswordField()
    private lateinit var maxRequestsField: JBTextField
    var maxRequestsPerTask: Int = DEFAULT_MAX_REQUESTS_PER_TASK
        set(value) {
            field = value
            maxRequestsField.text = value.toString()
        }

    init {
        mainPanel = panel {
            row("API key:") {
                cell(apiKeyField)
                    .align(AlignX.FILL)
            }
            row {
                checkBox("Show API key")
                    .applyToComponent {
                        addActionListener {
                            if (isSelected) {
                                showPassword()
                            } else {
                                hidePassword()
                            }
                        }
                    }
            }
            row("Max requests per task:") {
                maxRequestsField = intTextField()
                    .component
                    .also {
                        it.text = maxRequestsPerTask.toString()
                        it.document.addDocumentListener(object : DocumentListener {
                            override fun insertUpdate(e: DocumentEvent?) {
                                updateValue()
                            }

                            override fun removeUpdate(e: DocumentEvent?) {
                                updateValue()
                            }

                            override fun changedUpdate(e: DocumentEvent?) {
                                updateValue()
                            }

                            private fun updateValue() {
                                try {
                                    maxRequestsPerTask = it.text.toInt()
                                } catch (_: NumberFormatException) {

                                }
                            }
                        })
                    }
            }
        }
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