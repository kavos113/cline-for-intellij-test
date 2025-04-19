package com.github.kavos113.clinetest.shared.tool

data class ToolInput(
  val path: String? = null,
  val content: String? = null,
  val command: String? = null,
  val question: String? = null,
  val result: String? = null,
)
