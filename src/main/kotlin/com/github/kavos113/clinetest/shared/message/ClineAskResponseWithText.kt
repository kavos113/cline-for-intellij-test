package com.github.kavos113.clinetest.shared.message

data class ClineAskResponseWithText(
    val type: ClineAskResponse,
    val text: String? = null,
)