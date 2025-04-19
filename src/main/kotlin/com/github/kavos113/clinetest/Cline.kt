package com.github.kavos113.clinetest

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.errors.AnthropicException
import com.anthropic.models.messages.ContentBlock
import com.anthropic.models.messages.ContentBlockParam
import com.anthropic.models.messages.Message
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.MessageParam
import com.anthropic.models.messages.Model
import com.anthropic.models.messages.TextBlockParam
import com.anthropic.models.messages.ToolChoice
import com.anthropic.models.messages.ToolChoiceAuto
import com.anthropic.models.messages.ToolResultBlockParam
import com.anthropic.models.messages.ToolUseBlock
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import com.github.difflib.patch.DeltaType
import com.github.difflib.patch.Patch
import com.github.kavos113.clinetest.analyze.ProjectAnalyzer
import com.github.kavos113.clinetest.shared.ApiRequestInfo
import com.github.kavos113.clinetest.shared.ApiTokenInfo
import com.github.kavos113.clinetest.shared.ClaudeRequestResult
import com.github.kavos113.clinetest.shared.anthropic.textMessage
import com.github.kavos113.clinetest.shared.anthropic.toContentBlockParam
import com.github.kavos113.clinetest.shared.message.ClineAsk
import com.github.kavos113.clinetest.shared.message.ClineAskOrSay
import com.github.kavos113.clinetest.shared.message.ClineAskResponse
import com.github.kavos113.clinetest.shared.message.ClineAskResponseListener
import com.github.kavos113.clinetest.shared.message.ClineMessage
import com.github.kavos113.clinetest.shared.message.ClineSay
import com.github.kavos113.clinetest.shared.message.ClineSayTool
import com.github.kavos113.clinetest.shared.message.ClineSayTools
import com.github.kavos113.clinetest.shared.tool.ToolInput
import com.github.kavos113.clinetest.shared.tool.ToolName
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import kotlin.io.path.Path

const val DEFAULT_MAX_REQUESTS_PER_TASK = 20

class Cline(
  task: String,
  apiKey: String,
  var maxRequestsPerTask: Int = DEFAULT_MAX_REQUESTS_PER_TASK,
  private val project: Project
) {
  private var anthropicClient: AnthropicClient = AnthropicOkHttpClient.builder()
    .apiKey(apiKey)
    .build()

  private var requestCount = 0
  private var askResponse: ClineAskResponse? = null
  private var askResponseText: String? = null
  var abort = false

  private val messageBus = project.messageBus
  private fun getClineService() = project.getService(ClineService::class.java)

  init {
    startTask(task)
  }

  fun updateApiKey(apiKey: String) {
    anthropicClient = AnthropicOkHttpClient.builder()
      .apiKey(apiKey)
      .build()
  }

  fun updateMaxRequestsPerTask(maxRequestsPerTask: Int) {
    this.maxRequestsPerTask = maxRequestsPerTask
  }

  fun ask(type: ClineAsk, question: String): Pair<ClineAskResponse, String?> {
    if (abort) {
      throw IllegalStateException("Task has been aborted")
    }

    askResponse = null
    askResponseText = null

    getClineService().addClineMessage(
      ClineMessage(
        ts = System.currentTimeMillis(),
        type = ClineAskOrSay.Ask,
        ask = type,
        text = question
      )
    )

    val latch = CountDownLatch(1)
    val connection = messageBus.connect()
    connection.subscribe(ClineAskResponseListener.CLINE_ASK_RESPONSE_TOPIC, object : ClineAskResponseListener {
      override fun onResponse(response: ClineAskResponse, text: String?) {
        askResponse = response
        askResponseText = text
        latch.countDown()
      }
    })

    try {
      latch.await()

      val response = askResponse ?: throw IllegalStateException("No response received")

      return Pair(response, askResponseText)
    } finally {
      connection.dispose()
    }
  }

  fun say(type: ClineSay, text: String? = null) {
    if (abort) {
      throw IllegalStateException("Task has been aborted")
    }

    getClineService().addClineMessage(
      ClineMessage(
        ts = System.currentTimeMillis(),
        type = ClineAskOrSay.Say,
        say = type,
        text = text
      )
    )
  }

  private fun startTask(task: String) {
    getClineService().clearClineMessages()

    var userPrompt = "Task: \"$task\""

    this.say(ClineSay.Text, task)

    var totalInputTokens = 0L
    var totalOutputTokens = 0L

    while (requestCount < maxRequestsPerTask) {
      val result = recursivelyMakeClaudeRequests(
        listOf(
          ContentBlockParam.ofText(
            TextBlockParam.builder()
              .text(userPrompt)
              .build()
          )
        )
      )

      totalInputTokens += result.inputTokens
      totalOutputTokens += result.outputTokens

      if (result.didEndLoop) {
        break
      } else {
        userPrompt =
          "Ask yourself if you have completed the user's task. If you have, use the attempt_completion tool, otherwise proceed to the next step. (This is an automated message, so do not respond to it conversationally. Just proceed with the task.)"
      }
    }
  }

  fun calculateApiCost(inputTokens: Long, outputTokens: Long): Double {
    val INPUT_COST_PER_MILLION = 3.0
    val OUTPUT_COST_PER_MILLION = 15.0
    val inputCost = (inputTokens / 1_000_000.0) * INPUT_COST_PER_MILLION
    val outputCost = (outputTokens / 1_000_000.0) * OUTPUT_COST_PER_MILLION
    return (inputCost + outputCost)
  }

  fun executeTool(toolName: ToolName, toolInput: ToolInput): String {
    return when (toolName) {
      ToolName.WriteToFile -> writeFile(toolInput.path!!, toolInput.content!!)
      ToolName.ReadFile -> readFile(toolInput.path!!)
      ToolName.AnalyzeProject -> analyzedProject(toolInput.path!!)
      ToolName.ListFiles -> listFiles(toolInput.path!!)
      ToolName.ExecuteCommand -> executeCommand(toolInput.command!!)
      ToolName.AskFollowupQuestion -> askFollowUpQuestion(toolInput.question!!)
      ToolName.AttemptCompletion -> attemptCompletion(toolInput.result!!, toolInput.command)
    }
  }

  fun writeFile(filePath: String, newContent: String): String {
    val file = File(filePath)
    if (file.exists()) {
      val originalContent = file.readText()

      val originalLines = originalContent.lines()
      val newLines = newContent.lines()
      val patch: Patch<String> = DiffUtils.diff(originalLines, newLines)

      val diffResult = UnifiedDiffUtils.generateUnifiedDiff(
        filePath,
        filePath,
        originalLines,
        patch,
        3
      ).joinToString("\n")

      val diffRepresentation = createFullDiffRepresentation(originalLines, patch)

      val (response, text) = ask(
        ClineAsk.Tool,
        jacksonObjectMapper().writeValueAsString(
          ClineSayTool(
            tool = ClineSayTools.EditedExistingFile,
            path = filePath,
            diff = diffRepresentation
          )
        )
      )

      if (response != ClineAskResponse.YesButtonTapped) {
        if (response == ClineAskResponse.TextResponse && text != null) {
          say(ClineSay.UserFeedback, text)
          return "The user denied this operation and provided the following feedback:\n\"${text}\""
        }
        return "The user denied this operation"
      }

      file.writeText(newContent)
      return "Changes applied to $filePath:\n$diffResult"
    } else {
      val (response, text) = ask(
        ClineAsk.Tool,
        jacksonObjectMapper().writeValueAsString(
          ClineSayTool(
            tool = ClineSayTools.NewFileCreated,
            path = filePath,
            content = newContent
          )
        )
      )

      if (response != ClineAskResponse.YesButtonTapped) {
        if (response == ClineAskResponse.TextResponse && text != null) {
          say(ClineSay.UserFeedback, text)
          return "The user denied this operation and provided the following feedback:\n\"${text}\""
        }
        return "The user denied this operation"
      }

      if (!file.parentFile.exists()) {
        file.parentFile.mkdirs()
      }
      file.writeText(newContent)

      return "New file created and content written to $filePath"
    }
  }

  private fun createFullDiffRepresentation(
    oldLines: List<String>,
    patch: Patch<String>
  ): String {
    val sb = StringBuilder()

    val sortedDeltas = patch.deltas.sortedBy { it.source.position }

    var currentPosition = 0
    sortedDeltas.forEach { d ->
      for (i in currentPosition until d.source.position) {
        sb.appendLine(" ${oldLines[i]}")
      }

      when (d.type) {
        DeltaType.CHANGE -> {
          d.source.lines.forEach { sb.appendLine("-$it") }
          d.target.lines.forEach { sb.appendLine("+$it") }
        }

        DeltaType.DELETE -> {
          d.source.lines.forEach { sb.appendLine("-$it") }
        }

        DeltaType.INSERT -> {
          d.target.lines.forEach { sb.appendLine("+$it") }
        }

        DeltaType.EQUAL -> {
          d.source.lines.forEach { sb.appendLine(" $it") }
        }
      }

      currentPosition = d.source.position + d.source.lines.size
    }

    for (i in currentPosition until oldLines.size) {
      sb.appendLine(" ${oldLines[i]}")
    }

    return sb.toString()
  }

  fun readFile(filePath: String): String {
    val content = File(filePath).readText()
    val (response, text) = ask(
      ClineAsk.Tool,
      jacksonObjectMapper().writeValueAsString(
        ClineSayTool(
          tool = ClineSayTools.ReadFile,
          path = filePath,
          content = content
        )
      )
    )

    if (response != ClineAskResponse.YesButtonTapped) {
      if (response == ClineAskResponse.TextResponse && text != null) {
        say(ClineSay.UserFeedback, text)
        return "The user denied this operation and provided the following feedback:\n\"${text}\""
      }
      return "The user denied this operation"
    }

    return content
  }

  fun analyzedProject(dirPath: String): String {
    try {
      val analysis = ProjectAnalyzer.analyzeProject(Path(dirPath))

      val (response, text) = ask(
        ClineAsk.Tool,
        jacksonObjectMapper().writeValueAsString(
          ClineSayTool(
            tool = ClineSayTools.AnalyzeProject,
            path = dirPath,
            content = analysis
          )
        )
      )

      if (response != ClineAskResponse.YesButtonTapped) {
        if (response == ClineAskResponse.TextResponse && text != null) {
          say(ClineSay.UserFeedback, text)
          return "The user denied this operation and provided the following feedback:\n\"${text}\""
        }
        return "The user denied this operation"
      }

      return analysis
    } catch (e: Exception) {
      val errorString = "Error analyzing project:\n${e.message}"
      say(ClineSay.Error, "Error analyzing project: ${e.message}")
      return errorString
    }
  }

  fun listFiles(dirPath: String): String {
    val path = File(dirPath).absolutePath
    val root = System.getProperty("os.name").lowercase().let {
      if (it.contains("win")) {
        path.substring(0, 3)
      } else {
        "/"
      }
    }
    val isRoot = path == root

    if (isRoot) {
      val (response, text) = ask(
        ClineAsk.Tool,
        jacksonObjectMapper().writeValueAsString(
          ClineSayTool(
            tool = ClineSayTools.ListFiles,
            path = dirPath,
            content = root
          )
        )
      )

      if (response != ClineAskResponse.YesButtonTapped) {
        if (response == ClineAskResponse.TextResponse && text != null) {
          say(ClineSay.UserFeedback, text)
          return "The user denied this operation and provided the following feedback:\n\"${text}\""
        }
        return "The user denied this operation"
      }

      return root
    }

    val files =
      File(dirPath).listFiles()?.map { if (it.isDirectory) "${it.name}/" else it.name }?.sorted() ?: emptyList()
    val result = files.joinToString("\n")
    val (response, text) = ask(
      ClineAsk.Tool,
      jacksonObjectMapper().writeValueAsString(
        ClineSayTool(
          tool = ClineSayTools.ListFiles,
          path = dirPath,
          content = result
        )
      )
    )

    if (response != ClineAskResponse.YesButtonTapped) {
      if (response == ClineAskResponse.TextResponse && text != null) {
        say(ClineSay.UserFeedback, text)
        return "The user denied this operation and provided the following feedback:\n\"${text}\""
      }
      return "The user denied this operation"
    }

    return result
  }

  fun executeCommand(command: String, returnEmptyStringOnSuccess: Boolean = false): String {
    val (response, text) = ask(
      ClineAsk.Command,
      command
    )
    if (response != ClineAskResponse.YesButtonTapped) {
      if (response == ClineAskResponse.TextResponse && text != null) {
        say(ClineSay.UserFeedback, text)
        return "The user denied this operation and provided the following feedback:\n\"${text}\""
      }
      return "The user denied this operation"
    }

    try {
      val stringBuilder = StringBuilder()

      val commandParts = if (System.getProperty("os.name").lowercase().contains("win")) {
        "cmd /c $command".replace("\\", "\\\\")
      } else {
        command
      }.split("\\s".toRegex())

      val commandLine = GeneralCommandLine(commandParts)
      commandLine.charset = StandardCharsets.UTF_8
      commandLine.setWorkDirectory(project.basePath)

      val latch = CountDownLatch(1)

      val processHandler = OSProcessHandler(commandLine)
      processHandler.addProcessListener(object : ProcessAdapter() {
        override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
          stringBuilder.append(event.text)
          println(event.text)
          say(ClineSay.CommandOutput, event.text)
        }

        override fun processTerminated(event: ProcessEvent) {
          latch.countDown()
        }
      })

      processHandler.startNotify()
      latch.await()

      if (returnEmptyStringOnSuccess) {
        return ""
      }

      return "Command executed successfully. Output:\n$stringBuilder"
    } catch (e: ExecutionException) {
      val errorString = "Error executing command:\n${e.message}"
      say(ClineSay.Error, "Error executing command: ${e.message}")
      return errorString
    }
  }

  fun askFollowUpQuestion(question: String): String {
    val (_, text) = ask(
      ClineAsk.Followup,
      question
    )
    say(ClineSay.UserFeedback, text)
    return "User's response:\n\"$text\""
  }

  fun attemptCompletion(result: String, command: String? = null): String {
    var resultToSend = result
    if (command != null) {
      say(ClineSay.CompletionResult, resultToSend)

      val commandResult = executeCommand(command, true)
      if (commandResult.isNotEmpty()) {
        return commandResult
      }
      resultToSend = ""
    }

    val (response, text) = ask(
      ClineAsk.CompletionResult,
      resultToSend
    )
    if (response == ClineAskResponse.YesButtonTapped) {
      return ""
    }

    say(ClineSay.UserFeedback, text ?: "")
    return "The user is not pleased with the results. Use the feedback they provided to successfully complete the task, and then attempt completion again.\nUser's feedback:\n\"${text}\""
  }

  fun attemptApiRequest(): Message {
    try {
      val params = MessageCreateParams.builder()
        .model(Model.CLAUDE_3_HAIKU_20240307) // most cheap model
        .maxTokens(4096L) // for HAIKU
        .system(Prompt.SYSTEM_PROMPT)
        .messages(getClineService().getApiConversationHistory())
        .tools(Prompt.TOOLS)
        .toolChoice(ToolChoiceAuto.builder().type(JsonValue.from("auto")).build())
        .build()

      Thread.sleep(2000)
//            return anthropicClient.messages().create(params)
      return textMessage
    } catch (e: AnthropicException) {
      val (response, _) = ask(
        ClineAsk.ApiReqFailed,
        "Error occurred while making an API request: ${e.message}"
      )
      if (response != ClineAskResponse.YesButtonTapped) {
        throw IllegalStateException("API request failed")
      }

      say(ClineSay.ApiReqRetired)
      return attemptApiRequest()
    }
  }

  fun recursivelyMakeClaudeRequests(userContent: List<ContentBlockParam>): ClaudeRequestResult {
    if (abort) {
      throw IllegalStateException("Task has been aborted")
    }

    getClineService().addMessageToApiConversationHistory(
      MessageParam.builder()
        .content(MessageParam.Content.ofBlockParams(userContent))
        .role(MessageParam.Role.USER)
        .build()
    )

    if (finishByRequestCount()) {
      return ClaudeRequestResult(
        didEndLoop = true,
        inputTokens = 0,
        outputTokens = 0
      )
    }

    say(
      ClineSay.ApiReqStarted,
      jacksonObjectMapper().writeValueAsString(
        ApiRequestInfo(
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
      )
    )

    try {
      val response = attemptApiRequest()
      requestCount++

      val assistantResponses: MutableList<ContentBlock> = mutableListOf()
      var inputTokens = response.usage().inputTokens()
      var outputTokens = response.usage().outputTokens()
      say(
        ClineSay.ApiReqFinished,
        jacksonObjectMapper().writeValueAsString(
          ApiTokenInfo(
            tokensIn = inputTokens,
            tokensOut = outputTokens,
            cost = calculateApiCost(inputTokens, outputTokens)
          )
        )
      )

      for (contentBlock in response.content()) {
        contentBlock.text().ifPresent {
          assistantResponses.add(contentBlock)
          say(
            ClineSay.Text,
            contentBlock.text().get().text()
          )
        }
      }

      val toolResults: MutableList<ToolResultBlockParam> = mutableListOf()
      var attemptCompletionBlock: ToolUseBlock? = null
      for (contentBlock in response.content()) {
        contentBlock.toolUse().ifPresent {
          assistantResponses.add(contentBlock)

          val toolName = ToolName.fromString(contentBlock.toolUse().get().name())
          val toolInput = contentBlock.toolUse().get()._input().convert(ToolInput::class.java)
          val toolUseId = contentBlock.toolUse().get().id()

          if (toolName == ToolName.AttemptCompletion) {
            attemptCompletionBlock = contentBlock.toolUse().get()
          } else {
            val result = executeTool(toolName, toolInput!!)
            toolResults.add(
              ToolResultBlockParam.builder()
                .type(JsonValue.from("tool_result"))
                .toolUseId(toolUseId)
                .content(result)
                .build()
            )
          }
        }
      }

      if (assistantResponses.isNotEmpty()) {
        getClineService().addMessageToApiConversationHistory(
          MessageParam.builder()
            .role(MessageParam.Role.ASSISTANT)
            .content(MessageParam.Content.ofBlockParams(assistantResponses.toList().map { it.toContentBlockParam() }))
            .build()
        )
      } else {
        say(
          ClineSay.Error,
          "Unexpected Error: No assistant messages were found in the API response"
        )
        getClineService().addMessageToApiConversationHistory(
          MessageParam.builder()
            .role(MessageParam.Role.ASSISTANT)
            .content(
              MessageParam.Content.ofBlockParams(
                listOf(
                  ContentBlockParam.ofText(
                    TextBlockParam.builder()
                      .text("Failure: I did not have a response to provide.")
                      .build()
                  )
                )
              )
            )
            .build()
        )
      }

      var didEndLoop = false

      if (attemptCompletionBlock != null) {
        var result = executeTool(
          ToolName.fromString(attemptCompletionBlock.name()),
          attemptCompletionBlock._input().convert(ToolInput::class.java)!!
        )

        if (result == "") {
          didEndLoop = true
          result = "The user is satisfied with the result."
        }
        toolResults.add(
          ToolResultBlockParam.builder()
            .type(JsonValue.from("tool_result"))
            .toolUseId(attemptCompletionBlock.id())
            .content(result)
            .build()
        )
      }

      if (toolResults.isNotEmpty()) {
        if (didEndLoop) {
          getClineService().addMessageToApiConversationHistory(
            MessageParam.builder()
              .role(MessageParam.Role.USER)
              .content(
                MessageParam.Content.ofBlockParams(
                  toolResults.toList().map { ContentBlockParam.ofToolResult(it) })
              )
              .build()
          )

          getClineService().addMessageToApiConversationHistory(
            MessageParam.builder()
              .role(MessageParam.Role.ASSISTANT)
              .content(
                MessageParam.Content.ofBlockParams(
                  listOf(
                    ContentBlockParam.ofText(
                      TextBlockParam.builder()
                        .text("I am pleased you are satisfied with the result. Do you have a new task for me?")
                        .build()
                    )
                  )
                )
              )
              .build()
          )
        } else {
          val claudeResult =
            recursivelyMakeClaudeRequests(toolResults.toList().map { ContentBlockParam.ofToolResult(it) })
          didEndLoop = claudeResult.didEndLoop
          inputTokens += claudeResult.inputTokens
          outputTokens += claudeResult.outputTokens
        }
      }

      return ClaudeRequestResult(
        didEndLoop = didEndLoop,
        inputTokens = inputTokens,
        outputTokens = outputTokens
      )
    } catch (e: Exception) {
      return ClaudeRequestResult(
        didEndLoop = true,
        inputTokens = 0,
        outputTokens = 0
      )
    }
  }

  internal fun finishByRequestCount(): Boolean {
    if (requestCount < maxRequestsPerTask) return false

    val (response, _) = ask(
      ClineAsk.RequestLimitReached,
      "Claude Dev has reached the maximum number of requests for this task. Would you like to reset the count and allow him to proceed?"
    )

    if (response == ClineAskResponse.YesButtonTapped) {
      requestCount = 0
    } else {
      getClineService().addMessageToApiConversationHistory(
        MessageParam.builder()
          .role(MessageParam.Role.ASSISTANT)
          .content(
            MessageParam.Content.ofBlockParams(
              listOf(
                ContentBlockParam.ofText(
                  TextBlockParam.builder()
                    .text("Failure: I have reached the request limit for this task. Do you have a new task for me?")
                    .build()
                )
              )
            )
          )
          .build()
      )

      return true
    }

    return false
  }
}