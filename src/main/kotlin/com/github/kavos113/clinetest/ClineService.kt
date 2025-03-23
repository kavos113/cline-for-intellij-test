package com.github.kavos113.clinetest

import com.anthropic.models.messages.MessageParam
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
    private lateinit var cline: Cline

    fun addClineMessage(message: ClineMessage) {

    }

    fun getClineMessages(): List<ClineMessage> {
        return emptyList()
    }

    fun clearClineMessages() {

    }

    fun setClineMessages(messages: List<ClineMessage>) {

    }

    fun getApiConversationHistory(): List<MessageParam> {
        return emptyList()
    }

    fun addMessageToApiConversationHistory(message: MessageParam): List<MessageParam> {
        return emptyList()
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

        fun getGlobalVariable(key: String): String {
            return ""
        }

        fun storeGlobalVariable(key: String, value: String) {

        }

        private fun createCredentialAttributes(key: String): CredentialAttributes {
            return CredentialAttributes("Cline", key)
        }
    }

}