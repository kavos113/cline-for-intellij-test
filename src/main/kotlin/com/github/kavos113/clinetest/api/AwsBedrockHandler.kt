package com.github.kavos113.clinetest.api

import com.anthropic.bedrock.backends.BedrockBackend
import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.models.messages.ContentBlockParam
import com.anthropic.models.messages.Message
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.MessageParam
import com.anthropic.models.messages.Model
import com.anthropic.models.messages.ToolChoice
import com.anthropic.models.messages.ToolChoiceAuto
import com.anthropic.models.messages.ToolUnion
import com.github.kavos113.clinetest.shared.api.ApiHandlerOptions
import com.github.kavos113.clinetest.shared.api.ApiRequestInfo
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.regions.Region

class AwsBedrockHandler(options: ApiHandlerOptions): ApiHandler {
  private val client: AnthropicClient

  init {
    val awsCredentials = AwsBasicCredentials.create(
      options.awsAccessKey,
      options.awsSecretKey
    )

    client = AnthropicOkHttpClient.builder()
      .backend(
        BedrockBackend.builder()
          .awsCredentials(awsCredentials)
          .region(Region.of(options.awsRegion))
          .build()
      )
      .build()
  }

  override fun createMessage(
    systemPrompt: String,
    messages: List<MessageParam>,
    tools: List<ToolUnion>
  ): Message {
    val params = MessageCreateParams.builder()
      .model(Model.CLAUDE_3_HAIKU_20240307)
      .maxTokens(4096L) // for HAIKU
      .system(systemPrompt)
      .messages(messages)
      .tools(tools)
      .build()

    return client.messages().create(params)
  }

  override fun createUserReadableRequest(userContent: List<ContentBlockParam>): ApiRequestInfo {
    return ApiRequestInfo(
      model = Model.CLAUDE_3_HAIKU_20240307,
      maxTokens = 8192L,
      messages = Pair(
        "...", MessageParam.builder()
          .content(MessageParam.Content.ofBlockParams(userContent))
          .role(MessageParam.Role.USER)
          .build()
      ),
      toolChoice = ToolChoice.ofAuto(ToolChoiceAuto.builder().type(JsonValue.from("auto")).build())
    )
  }
}