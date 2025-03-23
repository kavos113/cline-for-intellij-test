package com.github.kavos113.clinetest.ui

import com.github.kavos113.clinetest.ClineEventListener
import com.github.kavos113.clinetest.ClineService
import com.github.kavos113.clinetest.shared.message.ClineAsk
import com.github.kavos113.clinetest.shared.message.ClineAskOrSay
import com.github.kavos113.clinetest.shared.message.ClineAskResponse
import com.github.kavos113.clinetest.shared.message.ClineAskResponseListener
import com.github.kavos113.clinetest.shared.message.ClineMessage
import com.github.kavos113.clinetest.shared.message.ExtensionMessage
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.AlignY
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.GridLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class MainWindow : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val welcomeToolWindow = MainWindowContent(toolWindow, project)
        val content = ContentFactory.getInstance().createContent(welcomeToolWindow.getContent(), "Welcome", false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project): Boolean = true

    class MainWindowContent(toolWindow: ToolWindow, private val project: Project) {

        private var messages: List<ClineMessage> = emptyList()
        private var clineAsk: ClineAsk? = null

        private var textArea: JBTextArea? = null
        private var primaryButton: JButton? = null
        private var secondaryButton: JButton? = null

        private fun getClineService() = project.getService(ClineService::class.java)

        fun getContent(): JComponent {
            val contentPanel = panel {
                row {
                    text("What can I do for you?")
                        .bold()
                }
            }

            val mainPanel = JPanel(BorderLayout())

            val inputPanel = panel {
                row {
                    panel {
                        row {
                            cell(JPanel(GridLayout(1, 2, 5, 0)))
                                .align(AlignX.FILL)
                                .applyToComponent {
                                    primaryButton = JButton().apply {
                                        addActionListener {
                                            when (clineAsk) {
                                                ClineAsk.RequestLimitReached, ClineAsk.ApiReqFailed, ClineAsk.Command, ClineAsk.Tool -> {
                                                    project.messageBus.syncPublisher(ClineAskResponseListener.CLINE_ASK_RESPONSE_TOPIC)
                                                        .onResponse(ClineAskResponse.YesButtonTapped)
                                                }
                                                ClineAsk.CompletionResult -> {
                                                    getClineService().clearTask()
                                                }
                                                else -> {}
                                            }

                                            textArea?.isEnabled = false
                                            clineAsk = null
                                            setEnableButton(false)
                                        }
                                    }
                                    secondaryButton = JButton().apply {
                                        addActionListener {
                                            when (clineAsk) {
                                                ClineAsk.RequestLimitReached, ClineAsk.ApiReqFailed -> {
                                                    getClineService().clearTask()
                                                }
                                                ClineAsk.Command, ClineAsk.Tool -> {
                                                    project.messageBus.syncPublisher(ClineAskResponseListener.CLINE_ASK_RESPONSE_TOPIC)
                                                        .onResponse(ClineAskResponse.NoButtonTapped)
                                                }
                                                else -> {}
                                            }

                                            textArea?.isEnabled = false
                                            clineAsk = null
                                            setEnableButton(false)
                                        }
                                    }

                                    add(primaryButton)
                                    add(secondaryButton)
                                }
                        }
                        row {
                            textArea = textArea()
                                .resizableColumn()
                                .align(Align.FILL)
                                .applyToComponent {
                                    lineWrap = true
                                    wrapStyleWord = true
                                    border = JBUI.Borders.empty(8)
                                    emptyText.text = "Type a message..."

                                    document.addDocumentListener(object : DocumentListener {
                                        override fun insertUpdate(e: DocumentEvent?) = updateSize()
                                        override fun removeUpdate(e: DocumentEvent?) = updateSize()
                                        override fun changedUpdate(e: DocumentEvent?) = updateSize()

                                        private fun updateSize() {
                                            size.height = preferredSize.height
                                            mainPanel.revalidate()
                                        }
                                    })
                                }
                                .component

                            button("") {
                                handleSendMessage()
                            }.applyToComponent {
                                icon = AllIcons.Actions.ArrowExpand
                                toolTipText = "Send Message"
                                isFocusable = true
                                putClientProperty("JButton.buttonType", "toolBarButton")
                                border = JBUI.Borders.empty(4)
                            }.align(AlignY.CENTER)
                        }
                    }
                }.resizableRow()
            }

            mainPanel.add(contentPanel, BorderLayout.NORTH)
            mainPanel.add(inputPanel, BorderLayout.SOUTH)

            setEnableButton(true)
            setupMessageHandler()

            return mainPanel
        }

        private fun setupMessageHandler() {
            project.messageBus.connect().subscribe(
                ClineEventListener.CLINE_EVENT_TOPIC,
                object : ClineEventListener {
                    override fun onPostMessageToWindow(message: ExtensionMessage) {
                        val lastMessage = message.state?.clineMessages?.lastOrNull()
                        if (lastMessage != null) {
                            when (lastMessage.type) {
                                ClineAskOrSay.Ask -> {
                                    val ask = lastMessage.ask!!
                                    when (ask) {
                                        ClineAsk.RequestLimitReached -> {
                                            textArea?.isEnabled = false
                                            clineAsk = ClineAsk.RequestLimitReached
                                            setEnableButton(true)
                                            primaryButton?.text = "Proceed"
                                            secondaryButton?.text = "Start New Task"
                                        }
                                        ClineAsk.Followup -> {
                                            textArea?.isEnabled = true
                                            clineAsk = ClineAsk.Followup
                                            setEnableButton(false)
                                        }
                                        ClineAsk.Command -> {
                                            textArea?.isEnabled = true
                                            clineAsk = ClineAsk.Command
                                            setEnableButton(true)
                                            primaryButton?.text = "Run Command"
                                            secondaryButton?.text = "Reject"
                                        }
                                        ClineAsk.CompletionResult -> {
                                            textArea?.isEnabled = true
                                            clineAsk = ClineAsk.CompletionResult
                                            setEnableButton(true)
                                            primaryButton?.text = "Start New Task"
                                            secondaryButton?.isVisible = false
                                        }
                                        ClineAsk.Tool -> {
                                            textArea?.isEnabled = true
                                            clineAsk = ClineAsk.Tool
                                            setEnableButton(true)
                                            primaryButton?.text = "Approve"
                                            secondaryButton?.text = "Reject"
                                        }
                                        ClineAsk.ApiReqFailed -> {
                                            textArea?.isEnabled = false
                                            clineAsk = ClineAsk.ApiReqFailed
                                            setEnableButton(true)
                                            primaryButton?.text = "Retry"
                                            secondaryButton?.text = "Start New Task"
                                        }
                                    }
                                }
                                ClineAskOrSay.Say -> {}
                            }
                        }
                    }
                }
            )
        }

        private fun handleSendMessage() {
            val message = textArea?.text

            if (message?.isNotEmpty() == true) {
                if (messages.isEmpty()) {
                    getClineService().tryToInitClineWithTask(message)
                } else if (clineAsk != null) {
                    when (clineAsk) {
                        ClineAsk.Followup, ClineAsk.Tool, ClineAsk.Command, ClineAsk.CompletionResult -> {
                            project.messageBus.syncPublisher(ClineAskResponseListener.CLINE_ASK_RESPONSE_TOPIC)
                                .onResponse(
                                    response = ClineAskResponse.TextResponse,
                                    text = message
                                )
                        }
                        else -> {}
                    }
                }

                textArea?.text = ""
                textArea?.isEnabled = false
                clineAsk = null
                setEnableButton(false)
            }
        }

        private fun setEnableButton(enable: Boolean) {
            primaryButton?.isVisible = enable
            secondaryButton?.isVisible = enable
        }
    }
}