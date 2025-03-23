package com.github.kavos113.clinetest.shared.message

import com.intellij.util.messages.Topic

interface ClineAskResponseListener {
    fun onResponse(response: ClineAskResponse, text: String? = null)

    companion object {
        val CLINE_ASK_RESPONSE_TOPIC = Topic.create("Cline Ask Response", ClineAskResponseListener::class.java)
    }
}