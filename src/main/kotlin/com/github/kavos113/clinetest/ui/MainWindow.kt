package com.github.kavos113.clinetest.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignY
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class MainWindow : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val welcomeToolWindow = WelcomeWindowContent(toolWindow)
        val content = ContentFactory.getInstance().createContent(welcomeToolWindow.getContent(), "Welcome", false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project): Boolean = true

    class WelcomeWindowContent(toolWindow: ToolWindow) {

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
                            val textArea = textArea()
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


                            button("") {

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

            return mainPanel
        }
    }
}