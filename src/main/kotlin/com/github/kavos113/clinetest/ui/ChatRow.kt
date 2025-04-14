package com.github.kavos113.clinetest.ui

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kavos113.clinetest.shared.message.ClineAsk
import com.github.kavos113.clinetest.shared.message.ClineAskOrSay
import com.github.kavos113.clinetest.shared.message.ClineMessage
import com.github.kavos113.clinetest.shared.message.ClineSay
import com.github.kavos113.clinetest.shared.message.ClineSayTool
import com.github.kavos113.clinetest.shared.message.ClineSayTools
import com.intellij.icons.AllIcons
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.UIUtil
import java.awt.Font
import javax.swing.JEditorPane
import javax.swing.JPanel

class ChatRow(
    private val message: ClineMessage,
) {
    private val title: JBLabel
    private val icon: JBLabel
    private val content: JPanel

    private var apiTextLabel: JEditorPane? = null

    private var isPendingApi: Boolean? = null
    private var commandCodeBlock: CodeBlock? = null
    private var isStartCommandOutput: Boolean? = null

    private val normalColor = UIUtil.getLabelForeground()
    private val errorColor = JBColor.RED
    private val successColor = JBColor.GREEN

    init {

        title = when (message.type) {
            ClineAskOrSay.Ask -> {
                when (message.ask) {
                    ClineAsk.RequestLimitReached -> JBLabel("Max Requests Reached").apply {
                        foreground = errorColor
                    }
                    ClineAsk.Command -> JBLabel("Claude wants to execute this command:").apply {
                        foreground = normalColor
                    }
                    ClineAsk.CompletionResult -> JBLabel("Task Completed").apply {
                        foreground = successColor
                    }
                    else -> JBLabel()
                }
            }
            ClineAskOrSay.Say-> {
                when (message.say) {
                    ClineSay.Error -> JBLabel("Error").apply {
                        foreground = errorColor
                    }
                    ClineSay.CompletionResult -> JBLabel("Task Completed").apply {
                        foreground = successColor
                    }
                    ClineSay.ApiReqStarted -> JBLabel("Making API Request...").apply {
                        foreground = normalColor
                    }
                    else -> JBLabel()
                }
            }
        }

        title.apply {
            font = font.deriveFont(Font.BOLD)
        }

        icon = when (message.type) {
            ClineAskOrSay.Ask -> {
                when (message.ask) {
                    ClineAsk.RequestLimitReached -> JBLabel(AllIcons.General.Error).apply {
                        foreground = errorColor
                    }
                    ClineAsk.Command -> JBLabel(AllIcons.Nodes.Console).apply {
                        foreground = normalColor
                    }
                    ClineAsk.CompletionResult -> JBLabel(AllIcons.General.InspectionsOK).apply {
                        foreground = successColor
                    }
                    else -> JBLabel()
                }
            }
            ClineAskOrSay.Say-> {
                when (message.say) {
                    ClineSay.Error -> JBLabel(AllIcons.General.Error).apply {
                        foreground = errorColor
                    }
                    ClineSay.CompletionResult -> JBLabel(AllIcons.General.InspectionsOK).apply {
                        foreground = successColor
                    }
                    ClineSay.ApiReqStarted, ClineSay.ApiReqRetired -> JBLabel(AllIcons.Actions.Refresh).apply {
                        foreground = normalColor
                    }
                    else -> JBLabel()
                }
            }
        }

        content = when(message.type) {
            ClineAskOrSay.Say -> when(message.say) {
                ClineSay.ApiReqStarted, ClineSay.ApiReqRetired -> {
                    isPendingApi = true
                    panel {
                        row {
                            cell(icon)
                            cell(title)
                        }
                        row {
                            apiTextLabel = text("").applyToComponent {
                                foreground = normalColor
                            }.component
                        }
                    }
                }
                ClineSay.ApiReqFinished -> panel {  }
                ClineSay.Text, ClineSay.UserFeedback -> panel {
                    row {
                        cell(MarkdownPanel(message.text ?: ""))
                            .align(AlignX.FILL)
                    }
                }
                ClineSay.Error -> panel {
                    row {
                        cell(icon)
                        cell(title)
                    }
                    row {
                        text(message.text ?: "").applyToComponent {
                            foreground = errorColor
                        }
                    }
                }
                ClineSay.CompletionResult -> panel {
                    row {
                        cell(icon)
                        cell(title)
                    }
                    row {
                        cell(MarkdownPanel(message.text ?: ""))
                            .align(AlignX.FILL)
                    }
                }
                else -> panel {
                    row {
                        if (icon.text.isNotEmpty()) {
                            cell(icon)
                        }
                        if (title.text.isNotEmpty()) {
                            cell(title)
                        }
                    }
                    row {
                        cell(MarkdownPanel(message.text ?: ""))
                            .align(AlignX.FILL)
                    }
                }
            }
            ClineAskOrSay.Ask -> when(message.ask) {
                ClineAsk.Tool -> {
                    val tool = jacksonObjectMapper().readValue<ClineSayTool>(message.text!!)

                    when(tool.tool) {
                        ClineSayTools.EditedExistingFile -> panel {
                            row {
                                cell(icon.apply { icon = AllIcons.Actions.Edit })
                                cell(title.apply { text = "Claude wants to edit this file:" })
                            }
                            row {
                                cell(CodeBlock(null, tool.diff, tool.path, true))
                                    .align(AlignX.FILL)
                            }
                        }
                        ClineSayTools.NewFileCreated -> panel {
                            row {
                                cell(icon.apply { icon = AllIcons.Actions.AddFile })
                                cell(title.apply { text = "Claude wants to create a new file:" })
                            }
                            row {
                                text("Path: ${tool.path}")
                            }
                            row {
                                cell(CodeBlock(tool.content, null, tool.path, true))
                                    .align(AlignX.FILL)
                            }
                        }
                        ClineSayTools.ReadFile -> panel {
                            row {
                                cell(icon.apply { icon = AllIcons.FileTypes.Any_type })
                                cell(title.apply { text = "Claude wants to read this file:" })
                            }
                            row {
                                cell(CodeBlock(tool.content, null, tool.path, true))
                                    .align(AlignX.FILL)
                            }
                        }
                        ClineSayTools.ListFiles -> panel {
                            row {
                                cell(icon.apply { icon = AllIcons.FileTypes.Any_type })
                                cell(title.apply { text = "Claude wants view files in this directory:" })
                            }
                            row {
                                text("Path: ${tool.path}")
                            }
                            row {
                                cell(CodeBlock(tool.content, null, tool.path, true))
                                    .align(AlignX.FILL)
                            }
                        }
                        ClineSayTools.AnalyzeProject -> panel {
                            row {
                                cell(icon.apply { icon = AllIcons.Actions.DependencyAnalyzer })
                                cell(title.apply { text = "Claude wants to analyze this project:" })
                            }
                            row {
                                text("Path: ${tool.path}")
                            }
                            row {
                                cell(CodeBlock(tool.content, null, tool.path, true))
                                    .align(AlignX.FILL)
                            }
                        }
                    }
                }
                ClineAsk.RequestLimitReached -> panel {
                    row {
                        cell(icon)
                        cell(title)
                    }
                    row {
                        text(message.text ?: "").applyToComponent {
                            foreground = errorColor
                        }
                    }
                }
                ClineAsk.Command -> panel {
                    row {
                        cell(icon)
                        cell(title)
                    }
                    row {
                        commandCodeBlock = CodeBlock(
                            code = message.text,
                            diff = null,
                            path = null,
                            isExpanded = true
                        )
                        isStartCommandOutput = false
                        cell(commandCodeBlock!!)
                            .align(AlignX.FILL)
                    }
                }
                ClineAsk.CompletionResult -> panel {
                    row {
                        cell(icon)
                        cell(title)
                    }
                    row {
                        cell(MarkdownPanel(message.text ?: ""))
                            .align(AlignX.FILL)
                    }
                }
                ClineAsk.Followup -> panel {
                    row {
                        if (icon.text.isNotEmpty()) {
                            cell(icon)
                        }
                        if (title.text.isNotEmpty()) {
                            cell(title)
                        }
                    }
                    row {
                        cell(MarkdownPanel(message.text ?: ""))
                            .align(AlignX.FILL)
                    }
                }
                else -> panel {
                    row {
                        if (icon.text.isNotEmpty()) {
                            cell(icon)
                        }
                        if (title.text.isNotEmpty()) {
                            cell(title)
                        }
                    }
                    row {
                        cell(MarkdownPanel(message.text ?: ""))
                            .align(AlignX.FILL)
                    }
                }
            }
        }
    }

    fun getContent(): JPanel {
//        content.border = UIUtil.getTextFieldBorder()
        return content
    }

    fun isPendingApiRequest(): Boolean {
        return isPendingApi == true
    }

    fun updateApiRequest(message: ClineMessage) {
        if (isPendingApi != true || (message.say != ClineSay.ApiReqFinished && message.ask != ClineAsk.ApiReqFailed)) {
            return
        }

        if (message.say == ClineSay.ApiReqFinished) {
            icon.icon = AllIcons.General.InspectionsOK
            icon.foreground = successColor
            title.text = "API Request Complete"
        } else {
            icon.icon = AllIcons.General.Error
            icon.foreground = errorColor
            title.text = "API Request Failed"
            title.foreground = errorColor

            apiTextLabel?.text = message.text ?: "API Request Failed"
            apiTextLabel?.foreground = errorColor
            apiTextLabel?.revalidate()
        }

        content.revalidate()
    }

    fun isCommand(): Boolean {
        return commandCodeBlock != null
    }

    fun addCommandOutput(message: ClineMessage) {
        if (isStartCommandOutput == false) {
            commandCodeBlock?.addPath(commandCodeBlock?.code?: "")
            commandCodeBlock?.setCode(message.text?: "")
            isStartCommandOutput = true
            return
        }
        commandCodeBlock?.addCode(message.text?: "")
    }
}
