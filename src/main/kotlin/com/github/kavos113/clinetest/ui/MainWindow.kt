package com.github.kavos113.clinetest.ui

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kavos113.clinetest.ClineEventListener
import com.github.kavos113.clinetest.ClineService
import com.github.kavos113.clinetest.shared.ApiTokenInfo
import com.github.kavos113.clinetest.shared.message.ClineAsk
import com.github.kavos113.clinetest.shared.message.ClineAskOrSay
import com.github.kavos113.clinetest.shared.message.ClineAskResponse
import com.github.kavos113.clinetest.shared.message.ClineAskResponseListener
import com.github.kavos113.clinetest.shared.message.ClineMessage
import com.github.kavos113.clinetest.shared.message.ClineSay
import com.github.kavos113.clinetest.shared.message.ExtensionMessage
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.AlignY
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
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

        private var messageCount = 0
        private var clineAsk: ClineAsk? = null

        private var textArea: JBTextArea? = null
        private var primaryButton: JButton? = null
        private var secondaryButton: JButton? = null
        private var chatPanel: JPanel? = null
        private val mainPanel = JPanel(BorderLayout())

        private var lastChat: ChatRow? = null
        private var taskHeader: TaskHeader? = null
        private val welcomeHeader = panel {
            row {
                text("What can I do for you?")
                    .bold()
            }
        }

        private fun getClineService() = project.getService(ClineService::class.java)

        fun getContent(): JComponent {
            val inputPanel = panel {
                row {
                    panel {
                        row {
                            button("add message") {
                                if (messageCount < sampleMessages.size) {
                                    project.messageBus.syncPublisher(ClineEventListener.CLINE_EVENT_TOPIC)
                                        .onAddClineMessage(sampleMessages[messageCount])
                                    messageCount++
                                }
                            }
                        }
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

            chatPanel = JPanel().apply {
                layout = GridBagLayout()

                add(JPanel(), GridBagConstraints().apply {
                    gridy = 999
                    weighty = 1.0
                })
            }

            mainPanel.add(welcomeHeader, BorderLayout.NORTH)
            mainPanel.add(inputPanel, BorderLayout.SOUTH)
            mainPanel.add(JBScrollPane(chatPanel).apply {
                horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
                verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
            }, BorderLayout.CENTER)

            setEnableButton(false)
            setupMessageHandler()

            return mainPanel
        }

        private fun setupMessageHandler() {
            project.messageBus.connect().subscribe(
                ClineEventListener.CLINE_EVENT_TOPIC,
                object : ClineEventListener {

                    override fun onAddClineMessage(message: ClineMessage) {
                        setEnableButton(false)
                        when (message.type) {
                            ClineAskOrSay.Ask -> {
                                val ask = message.ask!!
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
                            ClineAskOrSay.Say -> {
                                when(message.say) {
                                    ClineSay.ApiReqFinished -> {
                                        val info = jacksonObjectMapper().readValue<ApiTokenInfo>(message.text!!)
                                        taskHeader?.addApiInfo(info)
                                    }
                                    else -> {}
                                }
                            }
                        }

                        addMessageToChatPanel(message)
                    }

                    override fun onPostMessageToWindow(message: ExtensionMessage) {

                    }

                    override fun onClearClineMessages() {
                        messageCount = 0
                        textArea?.isEnabled = true
                        clineAsk = null
                        setEnableButton(false)
                        chatPanel?.removeAll()
                        chatPanel?.add(JPanel(), GridBagConstraints().apply {
                            gridy = 999
                            weighty = 1.0
                        })
                        chatPanel?.revalidate()
                        chatPanel?.repaint()
                    }
                }
            )
        }

        private fun handleSendMessage() {
            val message = textArea?.text

            if (message?.isNotEmpty() == true) {
                if (messageCount == 0) {
                    taskHeader = TaskHeader(message)
                    mainPanel.remove(welcomeHeader)
                    mainPanel.add(taskHeader!!.content, BorderLayout.NORTH)
                    mainPanel.revalidate()
                    mainPanel.repaint()

//                    getClineService().tryToInitClineWithTask(message)
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

        private fun addMessageToChatPanel(message: ClineMessage) {
            if (lastChat?.isPendingApiRequest() == true && (message.say == ClineSay.ApiReqFinished || message.ask == ClineAsk.ApiReqFailed)) {
                lastChat?.updateApiRequest(message)
                return
            }

            if (lastChat?.isCommand() == true && message.say == ClineSay.CommandOutput) {
                lastChat?.addCommandOutput(message)
                return
            }

            chatPanel?.remove(chatPanel?.componentCount?.minus(1) ?: 0)

            lastChat = ChatRow(message)
            chatPanel?.add(lastChat!!.getContent(), GridBagConstraints().apply {
                gridx = 0
                gridy = chatPanel?.componentCount ?: 0
                weightx = 1.0
                fill = GridBagConstraints.HORIZONTAL
                anchor = GridBagConstraints.NORTH
            })

            chatPanel?.add(JPanel(), GridBagConstraints().apply {
                gridy = chatPanel?.componentCount ?: 0
                weighty = 1.0
            })

            chatPanel?.revalidate()
            chatPanel?.repaint()
        }
    }
}