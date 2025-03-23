package com.github.kavos113.clinetest.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(
    name = "ClineSettings",
    storages = [Storage("cline.xml")]
)
@Service(Service.Level.APP)
class ClineSettings : PersistentStateComponent<ClineSettings.State> {

    data class State(
        var apiKey: String = "",
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