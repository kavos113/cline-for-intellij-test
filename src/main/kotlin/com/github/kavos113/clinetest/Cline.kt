package com.github.kavos113.clinetest

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import com.github.difflib.patch.DeltaType
import com.github.difflib.patch.Patch
import com.github.kavos113.clinetest.shared.message.ClineAsk
import com.github.kavos113.clinetest.shared.message.ClineAskOrSay
import com.github.kavos113.clinetest.shared.message.ClineAskResponse
import com.github.kavos113.clinetest.shared.message.ClineAskResponseListener
import com.github.kavos113.clinetest.shared.message.ClineMessage
import com.github.kavos113.clinetest.shared.message.ClineSay
import com.github.kavos113.clinetest.shared.message.ClineSayTool
import com.github.kavos113.clinetest.shared.message.ClineSayTools
import com.github.kavos113.clinetest.shared.tool.ToolName
import com.intellij.openapi.project.Project
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File
import java.util.concurrent.CountDownLatch

const val DEFAULT_MAX_REQUESTS_PER_TASK = 20

class Cline(
    task: String,
    apiKey: String,
    var maxRequestsPerTask: Int = DEFAULT_MAX_REQUESTS_PER_TASK,
    project: Project
) {
    private var anthropicClient: AnthropicClient = AnthropicOkHttpClient.builder()
        .apiKey(apiKey)
        .build()

    private var requestCount = 0
    private var askResponse: ClineAskResponse? = null
    private var askResponseText: String? = null
    var abort = false

    private val messageBus = project.messageBus

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

        messageBus.syncPublisher(ClineEventListener.CLINE_EVENT_TOPIC).onClineMessageAdded(
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

        messageBus.syncPublisher(ClineEventListener.CLINE_EVENT_TOPIC).onClineMessageAdded(
            ClineMessage(
                ts = System.currentTimeMillis(),
                type = ClineAskOrSay.Say,
                say = type,
                text = text
            )
        )
    }

    private fun startTask(task: String) {
        messageBus.syncPublisher(ClineEventListener.CLINE_EVENT_TOPIC).onClineMessageClear()

        var userPrompt = "Task: \"$task\""

        this.say(ClineSay.Text, task)

        var totalInputTokens = 0
        var totalOutputTokens = 0

        while (requestCount < maxRequestsPerTask) {

        }
    }

    fun calculateApiCost(inputTokens: Int, outputTokens: Int): Int {
        val INPUT_COST_PER_MILLION = 3.0
        val OUTPUT_COST_PER_MILLION = 15.0
        val inputCost = (inputTokens / 1_000_000.0) * INPUT_COST_PER_MILLION
        val outputCost = (outputTokens / 1_000_000.0) * OUTPUT_COST_PER_MILLION
        return (inputCost + outputCost).toInt()
    }

    fun executeTool(toolName: ToolName, toolInput: Any): String {
        when (toolName) {
            ToolName.WriteToFile -> TODO()
            ToolName.ReadFile -> TODO()
            ToolName.AnalyzeProject -> TODO()
            ToolName.ListFiles -> TODO()
            ToolName.ExecuteCommand -> TODO()
            ToolName.AskFollowupQuestion -> TODO()
            ToolName.AttemptCompletion -> TODO()
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
                Json.encodeToString(ClineSayTool(
                    tool = ClineSayTools.EditedExistingFile,
                    path = filePath,
                    diff = diffRepresentation
                ))
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
                Json.encodeToString(ClineSayTool(
                    tool = ClineSayTools.NewFileCreated,
                    path = filePath,
                    content = newContent
                ))
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
}