package com.github.kavos113.clinetest

import com.github.kavos113.clinetest.shared.message.ClineMessage
import com.intellij.util.messages.Topic

interface ClineEventListener {
    fun onClineMessageAdded(message: ClineMessage)

    companion object {
        val CLINE_EVENT_TOPIC = Topic.create("Cline Event", ClineEventListener::class.java)
    }
}