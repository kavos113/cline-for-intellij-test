package com.github.kavos113.clinetest.shared

data class ClaudeRequestResult(
    val didEndLoop: Boolean,
    val inputTokens: Long,
    val outputTokens: Long,
)
