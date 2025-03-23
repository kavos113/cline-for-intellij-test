package com.github.kavos113.clinetest.shared.anthropic

import com.anthropic.models.messages.ContentBlock
import com.anthropic.models.messages.ContentBlockParam
import com.anthropic.models.messages.RedactedThinkingBlockParam
import com.anthropic.models.messages.TextBlockParam
import com.anthropic.models.messages.ThinkingBlockParam
import com.anthropic.models.messages.ToolUseBlockParam

fun ContentBlock.toContentBlockParam(): ContentBlockParam {
    return when {
        text().isPresent -> {
            ContentBlockParam.ofText(
                TextBlockParam.builder()
                    .text(text().get().text())
                    .build()
            )
        }
        toolUse().isPresent -> {
            ContentBlockParam.ofToolUse(
                ToolUseBlockParam.builder()
                    .id(toolUse().get().id())
                    .input(toolUse().get()._input())
                    .name(toolUse().get().name())
                    .type(toolUse().get()._type())
                    .additionalProperties(toolUse().get()._additionalProperties())
                    .build()
            )
        }
        thinking().isPresent -> {
            ContentBlockParam.ofThinking(
                ThinkingBlockParam.builder()
                    .signature(thinking().get().signature())
                    .thinking(thinking().get().thinking())
                    .type(thinking().get()._type())
                    .additionalProperties(thinking().get()._additionalProperties())
                    .build()
            )
        }
        redactedThinking().isPresent -> {
            ContentBlockParam.ofRedactedThinking(
                RedactedThinkingBlockParam.builder()
                    .data(redactedThinking().get().data())
                    .type(redactedThinking().get()._type())
                    .additionalProperties(redactedThinking().get()._additionalProperties())
                    .build()
            )
        }
        else -> throw IllegalStateException("Unknown ContentBlock type")
    }
}