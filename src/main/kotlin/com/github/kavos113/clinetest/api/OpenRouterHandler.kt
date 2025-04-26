package com.github.kavos113.clinetest.api

import com.anthropic.core.JsonValue
import com.anthropic.models.messages.ContentBlockParam
import com.anthropic.models.messages.ImageBlockParam
import com.anthropic.models.messages.Message
import com.anthropic.models.messages.MessageParam
import com.anthropic.models.messages.Model
import com.anthropic.models.messages.ToolChoice
import com.anthropic.models.messages.ToolChoiceAuto
import com.anthropic.models.messages.ToolResultBlockParam
import com.anthropic.models.messages.ToolUnion
import com.anthropic.models.messages.ToolUseBlockParam
import com.github.kavos113.clinetest.shared.api.ApiHandlerOptions
import com.github.kavos113.clinetest.shared.api.ApiRequestInfo
import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.core.JsonString
import com.openai.models.FunctionDefinition
import com.openai.models.FunctionParameters
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam
import com.openai.models.chat.completions.ChatCompletionContentPart
import com.openai.models.chat.completions.ChatCompletionContentPartImage
import com.openai.models.chat.completions.ChatCompletionContentPartText
import com.openai.models.chat.completions.ChatCompletionMessageParam
import com.openai.models.chat.completions.ChatCompletionMessageToolCall
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam
import com.openai.models.chat.completions.ChatCompletionTool
import com.openai.models.chat.completions.ChatCompletionToolMessageParam
import com.openai.models.chat.completions.ChatCompletionUserMessageParam
import kotlin.text.get

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
    val openAiMessages = convertToOpenAiMessages(messages).toMutableList()
    openAiMessages.add(0,
      ChatCompletionMessageParam.ofSystem(
        ChatCompletionSystemMessageParam.builder()
          .content(systemPrompt)
          .build()
      )
    )

    val openAiTools = tools.map { tool ->
      convertTool(tool)
    }
  }

  private fun convertTool(tool: ToolUnion): ChatCompletionTool {
    val anthropicTool = tool.asTool()
    val schema = anthropicTool.inputSchema()

    val parameters = FunctionParameters.builder()
      .putAdditionalProperty("type", JsonString.of(schema._type().asString().get()))

    if (schema.properties().isPresent) {
      val propertiesMap = mutableMapOf<String, JsonValue>()
      schema.properties().get().members().forEach { (key, value) ->
        propertiesMap[key] = value
      }
      parameters.properties(propertiesMap)
    }

    if (schema.required().isPresent) {
      parameters.required(schema.required().get())
    }

    return ChatCompletionTool.builder()
      .type(JsonString.of("function"))
      .function(
        FunctionDefinition.builder()
          .name(anthropicTool.name())
          .description(anthropicTool.description().orElse(""))
          .parameters(parameters.build())
          .build()
      )
      .build()
  }

  override fun createUserReadableRequest(userContent: List<ContentBlockParam>): ApiRequestInfo {
    return ApiRequestInfo(
      model = Model.CLAUDE_3_5_SONNET_LATEST,
      maxTokens = 4096L,
      messages = Pair(
        "...", MessageParam.builder()
          .content(MessageParam.Content.ofBlockParams(userContent))
          .role(MessageParam.Role.USER)
          .build()
      ),
      tools = "(see tools in src/main/kotlin/com/github/kavos113/clinetest/Prompt.kt)",
      toolChoice = ToolChoice.ofAuto(ToolChoiceAuto.builder().type(JsonValue.from("auto")).build())
    )
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
                  if (message.content().get().blocks().isPresent) {
                    message.content().get().asBlocks().map {
                      if (it.isImageBlockParam()) {
                        toolResultImages.add(it.asImageBlockParam())
                        return@map "(see following user message for image)"
                      }
                      return@map it.asTextBlockParam().text()
                    }.joinToString("\n")
                  } else {
                    ""
                  }
                }
              }

              openAiMessages.add(
                ChatCompletionMessageParam.ofTool(
                  ChatCompletionToolMessageParam.builder()
                    .role(JsonString.of("tool"))
                    .toolCallId(message.toolUseId())
                    .content(content)
                    .build()
                )
              )
            }
          }
          MessageParam.Role.Known.ASSISTANT -> {
            val nonToolMessages = mutableListOf<ContentBlockParam>()
            val toolMessages = mutableListOf<ToolUseBlockParam>()

            message.content().blockParams().get().forEach { message ->
              when {
                message.isToolUse() -> {
                  toolMessages.add(message.toolUse().get())
                }
                message.isText() || message.isImage() -> {
                  nonToolMessages.add(message)
                }
              }
            }

            val content = if (nonToolMessages.isNotEmpty()) {
              nonToolMessages.map {
                if (it.isImage()) {
                  return@map ""
                }
                return@map it.asText().text()
              }.joinToString("\n")
            } else null

            val toolCalls = toolMessages.map { message ->
              ChatCompletionMessageToolCall.builder()
                .id(message.id())
                .type(JsonString.of("function"))
                .function(
                  ChatCompletionMessageToolCall.Function.builder()
                    .name(message.name())
                    .arguments(message._input().toString())
                    .build()
                )
                .build()
            }

            openAiMessages.add(
              ChatCompletionMessageParam.ofAssistant(
                if (content != null) {
                  ChatCompletionAssistantMessageParam.builder()
                    .role(JsonString.of("assistant"))
                    .content(content)
                    .toolCalls(toolCalls)
                    .build()
                } else {
                  ChatCompletionAssistantMessageParam.builder()
                    .role(JsonString.of("assistant"))
                    .toolCalls(toolCalls)
                    .build()
                }
              )
            )
          }
        }
      }
    }

    return openAiMessages
  }
}