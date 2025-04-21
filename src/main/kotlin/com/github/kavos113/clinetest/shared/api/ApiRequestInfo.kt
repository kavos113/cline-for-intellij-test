package com.github.kavos113.clinetest.shared.api

import com.anthropic.models.messages.MessageParam
import com.anthropic.models.messages.Model
import com.anthropic.models.messages.ToolChoice

data class ApiRequestInfo(
  val model: Model,
  val maxTokens: Long,
  val system: String = "(see SYSTEM_PROMPT in ...)",
  val messages: Pair<String, MessageParam>,
  val tools: String = "(see TOOLS in ...)",
  val toolChoice: ToolChoice
)
