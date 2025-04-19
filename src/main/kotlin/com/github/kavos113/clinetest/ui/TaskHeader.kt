package com.github.kavos113.clinetest.ui

import com.github.kavos113.clinetest.shared.ApiTokenInfo
import com.intellij.icons.AllIcons
import com.intellij.ui.dsl.builder.panel
import java.awt.Font
import javax.swing.JLabel
import javax.swing.JPanel

class TaskHeader(
  private val task: String
) {
  private var tokensIn: Long = 0
  private var tokensOut: Long = 0
  private var apiCost: Double = 0.0

  private val tokensInLabel: JLabel
  private val tokensOutLabel: JLabel
  private val apiCostLabel: JLabel

  val content: JPanel

  init {
    tokensInLabel = JLabel(tokensIn.toString()).apply {
      font = font.deriveFont(11f).deriveFont(Font.BOLD)
    }
    tokensOutLabel = JLabel(tokensOut.toString()).apply {
      font = font.deriveFont(11f).deriveFont(Font.BOLD)
    }
    apiCostLabel = JLabel("${'$'}$apiCost").apply {
      font = font.deriveFont(11f).deriveFont(Font.BOLD)
    }
    content = panel {
      row {
        label("Task")
          .bold()
          .applyToComponent {
            font = font.deriveFont(14f)
          }
      }
      row {
        label(task)
          .applyToComponent {
            font = font.deriveFont(11f)
          }
      }
      row {
        label("Tokens:")
          .bold()
          .applyToComponent {
            font = font.deriveFont(11f)
          }
        icon(AllIcons.General.ArrowUp)
        cell(tokensInLabel)
        icon(AllIcons.General.ArrowDown)
        cell(tokensOutLabel)
      }
      row {
        label("API Cost:")
          .bold()
          .applyToComponent {
            font = font.deriveFont(11f)
          }
        cell(apiCostLabel)
      }
    }
  }

  fun addApiInfo(info: ApiTokenInfo) {
    tokensIn += info.tokensIn
    tokensOut += info.tokensOut
    apiCost += info.cost

    tokensInLabel.text = tokensIn.toString()
    tokensOutLabel.text = tokensOut.toString()
    apiCostLabel.text = "${'$'}$apiCost"
  }
}