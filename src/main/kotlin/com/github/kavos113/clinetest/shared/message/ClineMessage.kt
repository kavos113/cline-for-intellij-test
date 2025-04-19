package com.github.kavos113.clinetest.shared.message

data class ClineMessage(
  val ts: Long,
  val type: ClineAskOrSay,
  val ask: ClineAsk? = null,
  val say: ClineSay? = null,
  val text: String? = null,
)