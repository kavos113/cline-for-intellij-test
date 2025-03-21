package com.github.kavos113.clinetest

import com.anthropic.models.messages.MessageParam
import com.github.kavos113.clinetest.shared.message.ClineMessage
import com.intellij.util.messages.Topic

interface ClineEventListener {
    fun onClineMessageAdded(message: ClineMessage)
    fun onClineMessageClear()
    fun onGetApiConversationHistory(): List<MessageParam>

    companion object {
        val CLINE_EVENT_TOPIC = Topic.create("Cline Event", ClineEventListener::class.java)
    }
}