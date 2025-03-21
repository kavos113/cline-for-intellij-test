package com.github.kavos113.clinetest

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.github.kavos113.clinetest.shared.message.ClineAsk
import com.github.kavos113.clinetest.shared.message.ClineAskOrSay
import com.github.kavos113.clinetest.shared.message.ClineAskResponse
import com.github.kavos113.clinetest.shared.message.ClineAskResponseListener
import com.github.kavos113.clinetest.shared.message.ClineMessage
import com.intellij.openapi.project.Project
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

    private fun startTask(task: String) {

    }
}