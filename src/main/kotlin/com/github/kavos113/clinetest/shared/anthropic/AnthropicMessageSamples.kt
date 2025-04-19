package com.github.kavos113.clinetest.shared.anthropic

import com.anthropic.core.JsonValue
import com.anthropic.models.messages.ContentBlock
import com.anthropic.models.messages.Message
import com.anthropic.models.messages.Model
import com.anthropic.models.messages.TextBlock
import com.anthropic.models.messages.ToolUseBlock
import com.anthropic.models.messages.Usage
import com.fasterxml.jackson.databind.ObjectMapper

val textMessage = Message.builder()
    .content(listOf(
        ContentBlock.ofText(
            TextBlock.builder()
                .text("""Hi, I am a software engineer. I can help you with your code.
## Overview
- I can help you with your code.
- I can help you with your code.
- I can help you with your code.

## Code
```python
def hello_world():
    print("Hello, world!")
hello_world()
```
```html
<body>
    <p>
        aaaa
    </p>
</body>
```
                """.trimIndent())
                .citations(listOf())
                .build()
        ),
    ))
    .id("")
    .model(Model.CLAUDE_3_7_SONNET_LATEST)
    .stopReason(Message.StopReason.END_TURN)
    .stopSequence("")
    .usage(
        Usage.builder()
            .inputTokens(20)
            .outputTokens(10)
            .cacheReadInputTokens(0)
            .cacheCreationInputTokens(0)
            .build()
    )
    .build()

val textWithToolUse = Message.builder()
    .content(listOf(
        ContentBlock.ofText(
            TextBlock.builder()
                .text("First, let's check the status of the git repository.")
                .citations(listOf())
                .build()
        ),
        ContentBlock.ofToolUse(
            ToolUseBlock.builder()
                .name("execute_command")
                .id("aaaa")
                .input(JsonValue.fromJsonNode(ObjectMapper().readTree("{\"command\": \"ls C:\\\\\"}")))
                .build()
        )
    ))
    .id("")
    .model(Model.CLAUDE_3_7_SONNET_LATEST)
    .stopReason(Message.StopReason.END_TURN)
    .stopSequence("")
    .usage(
        Usage.builder()
            .inputTokens(20)
            .outputTokens(10)
            .cacheReadInputTokens(0)
            .cacheCreationInputTokens(0)
            .build()
    )
    .build()
