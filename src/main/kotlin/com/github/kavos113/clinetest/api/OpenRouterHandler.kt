package com.github.kavos113.clinetest.api

import com.anthropic.models.messages.ContentBlockParam
import com.anthropic.models.messages.ImageBlockParam
import com.anthropic.models.messages.Message
import com.anthropic.models.messages.MessageParam
import com.anthropic.models.messages.ToolResultBlockParam
import com.anthropic.models.messages.ToolUnion
import com.github.kavos113.clinetest.shared.api.ApiHandlerOptions
import com.github.kavos113.clinetest.shared.api.ApiRequestInfo
import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.core.JsonString
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam
import com.openai.models.chat.completions.ChatCompletionContentPart
import com.openai.models.chat.completions.ChatCompletionContentPartImage
import com.openai.models.chat.completions.ChatCompletionContentPartText
import com.openai.models.chat.completions.ChatCompletionMessageParam
import com.openai.models.chat.completions.ChatCompletionUserMessageParam

class OpenRouterHandler(options: ApiHandlerOptions): ApiHandler {
  private val client: OpenAIClient = OpenAIOkHttpClient.builder()
    .apiKey(options.openRouterApiKey ?: "")
    .baseUrl("https://openrouter.ai/api/v1")
    .build()

  override fun createMessage(
    systemPrompt: String,
    messages: List<MessageParam>,
    tools: List<ToolUnion>
  ): Message {

  }

  override fun createUserReadableRequest(userContent: List<ContentBlockParam>): ApiRequestInfo {

  }

  fun convertToOpenAiMessages(anthropicMessages: List<MessageParam>): List<ChatCompletionMessageParam> {
    val openAiMessages = mutableListOf<ChatCompletionMessageParam>()
    anthropicMessages.forEach { message ->
      if (message.content().string().isPresent) {
        val msg = when (message.role().known()) {
          MessageParam.Role.Known.USER -> {
            ChatCompletionMessageParam.ofUser(
              ChatCompletionUserMessageParam.builder()
                .content(message.content().string().get())
                .build()
            )
          }
          MessageParam.Role.Known.ASSISTANT -> {
            ChatCompletionMessageParam.ofAssistant(
              ChatCompletionAssistantMessageParam.builder()
                .content(message.content().string().get())
                .build()
            )
          }
        }
        openAiMessages.add(msg)
      } else {
        when (message.role().known()) {
          MessageParam.Role.Known.USER -> {
            val nonToolMessages = mutableListOf<ContentBlockParam>()
            val toolMessages = mutableListOf<ToolResultBlockParam>()

            message.content().blockParams().get().forEach { message ->
              when {
                message.isToolResult() -> {
                  toolMessages.add(message.toolResult().get())
                }
                message.isText() || message.isImage() -> {
                  nonToolMessages.add(message)
                }
              }
            }

            if (nonToolMessages.isNotEmpty()) {
              openAiMessages.add(
                ChatCompletionMessageParam.ofUser(
                  ChatCompletionUserMessageParam.builder()
                    .content(
                      ChatCompletionUserMessageParam.Content.ofArrayOfContentParts(
                        nonToolMessages.map { message ->
                          val c = when {
                            message.isImage() -> ChatCompletionContentPart.ofImageUrl(
                              ChatCompletionContentPartImage.builder()
                                .type(JsonString.of("image_url"))
                                .imageUrl(
                                  ChatCompletionContentPartImage.ImageUrl.builder()
                                    .url("data:${
                                      message.asImage().source().asBase64Image().mediaType().asString()
                                    };base64,${
                                      message.asImage().source().asBase64Image().data()
                                    }")
                                    .build()
                                )
                                .build()
                            )
                            message.isText() -> ChatCompletionContentPart.ofText(
                              ChatCompletionContentPartText.builder()
                                .text(message.asText().text())
                                .build()
                            )
                            else -> throw IllegalArgumentException("Unknown message type")
                          }

                          return@map c
                        }
                      )
                    )
                    .build()
                )
              )
            }

            val toolResultImages = mutableListOf<ImageBlockParam>()
            toolMessages.forEach { message ->
              val content = when {
                message.content().get().isString() -> message.content().get().asString()
                else -> {

                }
              }
            }
          }
        }
      }
    }
  }
}