package com.github.kavos113.clinetest

import com.github.kavos113.clinetest.shared.message.ClineMessage
import com.github.kavos113.clinetest.shared.message.ExtensionMessage
import com.intellij.util.messages.Topic

interface ClineEventListener {
    fun onPostMessageToWindow(message: ExtensionMessage)
    fun onAddClineMessage(message: ClineMessage)
    fun onClearClineMessages()

    companion object {
        val CLINE_EVENT_TOPIC = Topic.create("Cline Event", ClineEventListener::class.java)
    }
}