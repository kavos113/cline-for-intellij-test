package com.github.kavos113.clinetest

import com.anthropic.models.messages.MessageParam
import com.github.kavos113.clinetest.settings.ClineSettings
import com.github.kavos113.clinetest.shared.message.ClineMessage
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import java.util.concurrent.CompletableFuture

@Service(Service.Level.PROJECT)
class ClineService(project: Project) {
    private var cline: Cline? = null
    private val clineInstanceIdentifier: Long = System.currentTimeMillis()

    fun clearTask() {
        if (cline != null) {
            cline!!.abort = true
            cline = null
        }

        setApiConversationHistory(emptyList())
        setClineMessages(emptyList())
    }

    fun addClineMessage(message: ClineMessage): List<ClineMessage> {
        val messages = getClineMessages().toMutableList()
        messages.add(message)
        setClineMessages(messages)
        return messages
    }

    fun getClineMessages(): List<ClineMessage> {
        return ClineSettings.getInstance().getClineMessages(clineInstanceIdentifier) ?: emptyList()
    }

    fun clearClineMessages() {
        ClineSettings.getInstance().setClineMessages(clineInstanceIdentifier, emptyList())
    }

    fun setClineMessages(messages: List<ClineMessage>) {
        ClineSettings.getInstance().setClineMessages(clineInstanceIdentifier, messages)
    }

    fun getApiConversationHistory(): List<MessageParam> {
        return ClineSettings.getInstance().getApiConversationHistory(clineInstanceIdentifier) ?: emptyList()
    }

    fun addMessageToApiConversationHistory(message: MessageParam): List<MessageParam> {
        val history = getApiConversationHistory().toMutableList()
        history.add(message)
        setApiConversationHistory(history)
        return history
    }

    fun setApiConversationHistory(history: List<MessageParam>) {
        ClineSettings.getInstance().setApiConversationHistory(clineInstanceIdentifier, history)
    }

    companion object {
        fun getSecret(key: String): String {
            val future = CompletableFuture<String>()
            ApplicationManager.getApplication().executeOnPooledThread {
                val attributes = createCredentialAttributes(key)
                val credentials = PasswordSafe.instance.get(attributes)
                future.complete(credentials?.getPasswordAsString() ?: "")
            }
            return future.get()
        }

        fun storeSecret(key: String, value: String) {
            ApplicationManager.getApplication().executeOnPooledThread {
                val attributes = createCredentialAttributes(key)
                val credentials = Credentials(key, value)
                PasswordSafe.instance.set(attributes, credentials)
            }
        }

        private fun createCredentialAttributes(key: String): CredentialAttributes {
            return CredentialAttributes("Cline", key)
        }
    }

}