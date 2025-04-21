package com.github.kavos113.clinetest.api

import com.anthropic.models.messages.ContentBlockParam
import com.anthropic.models.messages.Message
import com.anthropic.models.messages.MessageParam
import com.anthropic.models.messages.ToolUnion
import com.github.kavos113.clinetest.shared.api.ApiRequestInfo

interface ApiHandler {
  fun createMessage(
    systemPrompt: String,
    messages: List<MessageParam>,
    tools: List<ToolUnion>
  ): Message

  fun createUserReadableRequest(userContent: List<ContentBlockParam>): ApiRequestInfo
}