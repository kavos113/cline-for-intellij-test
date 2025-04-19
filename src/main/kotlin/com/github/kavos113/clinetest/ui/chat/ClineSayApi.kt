package com.github.kavos113.clinetest.ui.chat

import com.github.kavos113.clinetest.shared.message.ClineAsk
import com.github.kavos113.clinetest.shared.message.ClineMessage
import com.github.kavos113.clinetest.shared.message.ClineSay
import com.intellij.icons.AllIcons
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.UIUtil
import javax.swing.JEditorPane

class ClineSayApi(say: ClineSay?) {

  private val normalColor = UIUtil.getLabelForeground()
  private val errorColor = JBColor.RED
  private val successColor = JBColor.GREEN

  private var isPendingApi = false

  private val title = when (say) {
    ClineSay.ApiReqStarted -> JBLabel("Making API Request...").apply {
      foreground = normalColor
    }

    ClineSay.ApiReqRetired -> JBLabel("Making API Request...").apply {
      foreground = normalColor
    }

    else -> JBLabel()
  }

  private val icon = when (say) {
    ClineSay.ApiReqStarted, ClineSay.ApiReqRetired -> JBLabel(AllIcons.Actions.Refresh).apply {
      foreground = normalColor
    }

    else -> JBLabel()
  }

  private lateinit var contentText: JEditorPane

  val content = panel {
    row {
      cell(icon)
      cell(title)
    }
    row {
      contentText = text("").applyToComponent {
        foreground = normalColor
      }.component

      contentText.isVisible = false
    }
  }

  fun updateApiRequest(message: ClineMessage) {
    if (message.say == ClineSay.ApiReqFinished) {
      icon.icon = AllIcons.General.InspectionsOK
      icon.foreground = successColor
      title.text = "API Request Complete"

      content.revalidate()
    } else if (message.ask == ClineAsk.ApiReqFailed) {
      icon.icon = AllIcons.General.Error
      icon.foreground = errorColor
      title.text = "API Request Failed"
      title.foreground = errorColor

      contentText.text = message.text ?: "API Request Failed"
      contentText.foreground = errorColor
      contentText.isVisible = true

      content.revalidate()
    }
  }
}