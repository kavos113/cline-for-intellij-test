package com.github.kavos113.clinetest.shared.api

data class ApiHandlerOptions(
  val anthropicApiKey: String? = null,
  val openRouterApiKey: String? = null,
  val awsAccessKey: String? = null,
  val awsSecretKey: String? = null,
  val awsRegion: String? = null,
)
