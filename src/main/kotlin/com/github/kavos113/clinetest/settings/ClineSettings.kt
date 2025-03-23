package com.github.kavos113.clinetest.settings

import com.anthropic.models.messages.MessageParam
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.Converter

import com.intellij.util.xmlb.annotations.OptionTag

@State(
    name = "ClineSettings",
    storages = [Storage("cline.xml")]
)
@Service(Service.Level.APP)
class ClineSettings : PersistentStateComponent<ClineSettings.State> {

    data class State(
        val histories: Map<Long, ApiConversationHistory> = emptyMap(),
    )

    private var myState = State()

    override fun getState(): State {
        return myState
    }

    override fun loadState(state: State) {
        myState = state
    }

    companion object {
        fun getInstance(): ClineSettings = ApplicationManager.getApplication().getService(ClineSettings::class.java)
    }
}

data class ApiConversationHistory(
    val messages: List<MessageParam>
)

class ApiConversationHistoryConverter : Converter<ApiConversationHistory>() {
    override fun toString(history: ApiConversationHistory): String {
        return jacksonObjectMapper().writeValueAsString(history.messages)
    }

    override fun fromString(string: String): ApiConversationHistory {
        val messages = jacksonObjectMapper().readValue<List<MessageParam>>(string)
        return ApiConversationHistory(messages)
    }
}