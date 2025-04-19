package com.github.kavos113.clinetest.settings

import com.github.kavos113.clinetest.DEFAULT_MAX_REQUESTS_PER_TASK
import com.intellij.openapi.Disposable
import com.intellij.openapi.options.Configurable
import javax.swing.JComponent


class ClineSettingsConfigurable : Configurable, Disposable {

  private var component: ClineSettingsComponent? = null

  override fun createComponent(): JComponent {
    component = ClineSettingsComponent()
    component?.setApiKey(ClineSecretSettings.getSecret(ClineSecretSettings.API_KEY))
    component?.maxRequestsPerTask = ClineSettings.getInstance().state.maxRequestsPerTask
    return component?.mainPanel ?: throw IllegalStateException("Component is not initialized")
  }

  override fun isModified(): Boolean {
    return (component?.getApiKey() ?: "") != ClineSecretSettings.getSecret(ClineSecretSettings.API_KEY) ||
        (component?.maxRequestsPerTask
          ?: DEFAULT_MAX_REQUESTS_PER_TASK) != ClineSettings.getInstance().state.maxRequestsPerTask
  }

  override fun apply() {
    ClineSecretSettings.storeSecret(ClineSecretSettings.API_KEY, component?.getApiKey() ?: "")
    ClineSettings.getInstance().state.maxRequestsPerTask =
      component?.maxRequestsPerTask ?: DEFAULT_MAX_REQUESTS_PER_TASK
    println("settings applied")
  }

  override fun getDisplayName(): String {
    return "Cline Settings"
  }

  override fun reset() {
    component?.setApiKey(ClineSecretSettings.getSecret(ClineSecretSettings.API_KEY))
    component?.maxRequestsPerTask = ClineSettings.getInstance().state.maxRequestsPerTask
  }

  override fun dispose() {
    component = null
  }
}