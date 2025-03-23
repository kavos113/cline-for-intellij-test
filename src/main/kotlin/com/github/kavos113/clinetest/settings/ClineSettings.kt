package com.github.kavos113.clinetest.settings

import com.anthropic.models.messages.MessageParam
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kavos113.clinetest.shared.message.ClineMessage
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
        @OptionTag(converter = ApiHistoryConverter::class)
        var histories: Map<Long, List<MessageParam>> = emptyMap(),

        @OptionTag(converter = ClineMessageMapConverter::class)
        var messages: Map<Long, List<ClineMessage>> = emptyMap(),

        var maxRequestsPerTask: Int = 20,
    )

    private var myState = State()

    override fun getState(): State {
        return myState
    }

    override fun loadState(state: State) {
        myState = state
    }

    fun getApiConversationHistory(id: Long): List<MessageParam>? {
        return myState.histories[id]
    }

    fun setApiConversationHistory(id: Long, history: List<MessageParam>) {
        myState = myState.copy(histories = myState.histories + (id to history))
        state.histories = myState.histories
    }

    fun getClineMessages(id: Long): List<ClineMessage>? {
        return myState.messages[id]
    }

    fun setClineMessages(id: Long, messages: List<ClineMessage>) {
        myState = myState.copy(messages = myState.messages + (id to messages))
        state.messages = myState.messages
    }

    companion object {
        fun getInstance(): ClineSettings = ApplicationManager.getApplication().getService(ClineSettings::class.java)
    }
}

class ApiHistoryConverter : Converter<Map<Long, List<MessageParam>>>() {
    override fun toString(map: Map<Long, List<MessageParam>>): String? {
        return jacksonObjectMapper().writeValueAsString(map)
    }

    override fun fromString(json: String): Map<Long, List<MessageParam>> {
        return jacksonObjectMapper().readValue<Map<Long, List<MessageParam>>>(json)
    }
}

class ClineMessageMapConverter : Converter<Map<Long, List<ClineMessage>>>() {
    override fun toString(map: Map<Long, List<ClineMessage>>): String? {
        return jacksonObjectMapper().writeValueAsString(map)
    }

    override fun fromString(json: String): Map<Long, List<ClineMessage>> {
        return jacksonObjectMapper().readValue<Map<Long, List<ClineMessage>>>(json)
    }
}