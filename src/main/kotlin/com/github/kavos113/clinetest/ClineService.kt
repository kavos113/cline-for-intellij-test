package com.github.kavos113.clinetest

import com.anthropic.models.messages.MessageParam
import com.github.kavos113.clinetest.shared.message.ClineMessage
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class ClineService(project: Project) {
    private lateinit var cline: Cline

    fun addClineMessage(message: ClineMessage) {

    }

    fun getClineMessages(): List<ClineMessage> {
        return emptyList()
    }

    fun clearClineMessages() {

    }

    fun setClineMessages(messages: List<ClineMessage>) {

    }

    fun getApiConversationHistory(): List<MessageParam> {
        return emptyList()
    }

    fun addMessageToApiConversationHistory(message: MessageParam): List<MessageParam> {
        return emptyList()
    }
}