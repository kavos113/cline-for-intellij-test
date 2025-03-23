package com.github.kavos113.clinetest

import com.anthropic.models.messages.MessageParam
import com.github.kavos113.clinetest.settings.ClineSecretSettings
import com.github.kavos113.clinetest.settings.ClineSettings
import com.github.kavos113.clinetest.shared.message.ClineMessage
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class ClineService(private val project: Project) {
    private var cline: Cline? = null
    private val clineInstanceIdentifier: Long = System.currentTimeMillis()

    // "newTask"
    fun tryToInitClineWithTask(task: String) {
        clearTask()
        val apiKey = ClineSecretSettings.getSecret(ClineSecretSettings.API_KEY)
        val maxRequestsPerTask = ClineSettings.getInstance().state.maxRequestsPerTask

        if (apiKey.isNotEmpty()) {
            cline = Cline(
                task = task,
                apiKey = apiKey,
                maxRequestsPerTask = maxRequestsPerTask,
                project = project
            )
        }
    }

    private fun clearTask() {
        if (cline != null) {
            cline!!.abort = true
            cline = null
        }

        setApiConversationHistory(emptyList())
        setClineMessages(emptyList())
    }

    fun addClineMessage(message: ClineMessage): List<ClineMessage> {
        val messages = getClineMessages().toMutableList()
        messages.add(message)
        setClineMessages(messages)
        return messages
    }

    fun getClineMessages(): List<ClineMessage> {
        return ClineSettings.getInstance().getClineMessages(clineInstanceIdentifier) ?: emptyList()
    }

    fun clearClineMessages() {
        ClineSettings.getInstance().setClineMessages(clineInstanceIdentifier, emptyList())
    }

    fun setClineMessages(messages: List<ClineMessage>) {
        ClineSettings.getInstance().setClineMessages(clineInstanceIdentifier, messages)
    }

    fun getApiConversationHistory(): List<MessageParam> {
        return ClineSettings.getInstance().getApiConversationHistory(clineInstanceIdentifier) ?: emptyList()
    }

    fun addMessageToApiConversationHistory(message: MessageParam): List<MessageParam> {
        val history = getApiConversationHistory().toMutableList()
        history.add(message)
        setApiConversationHistory(history)
        return history
    }

    fun setApiConversationHistory(history: List<MessageParam>) {
        ClineSettings.getInstance().setApiConversationHistory(clineInstanceIdentifier, history)
    }
}