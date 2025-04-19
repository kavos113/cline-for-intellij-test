package com.github.kavos113.clinetest.settings

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.application.ApplicationManager
import java.util.concurrent.CompletableFuture

object ClineSecretSettings {
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

  const val API_KEY = "apiKey"
}