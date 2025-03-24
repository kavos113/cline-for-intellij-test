package com.github.kavos113.clinetest.ui

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.nio.file.Paths
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JEditorPane
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.text.html.HTMLEditorKit
import javax.swing.text.html.StyleSheet

class CodeBlock(
    val code: String?,
    diff: String?,
    path: String? = null,
    private var isExpanded: Boolean = true
) : JPanel(BorderLayout()) {

    private val codePanel: JComponent

    private val editorPane: JEditorPane

    private var content: String = ""

    init {
        border = BorderFactory.createLineBorder(Color(60, 60, 60))

        val content = code ?: diff ?: ""
        val language = getLanguageFromPath(path)
        editorPane = JEditorPane("text/html", "")
        codePanel = createCodeEditorPane(content, diff != null)

        if (path != null) {
            val headerPanel = createHeaderPanel(path)
            add(headerPanel, BorderLayout.NORTH)

            codePanel.isVisible = isExpanded
        }

        add(codePanel, BorderLayout.CENTER)
    }

    private fun createHeaderPanel(path: String): JPanel {
        val panel = JPanel(BorderLayout())
        panel.background = Color(50, 50, 50)
        panel.border = BorderFactory.createEmptyBorder(6, 10, 6, 10)

        val fileLabel = JLabel(path)
        fileLabel.foreground = Color(187, 187, 187)
        fileLabel.font = Font(Font.MONOSPACED, Font.PLAIN, 11)

        val expandIcon = JLabel(if (isExpanded) "▲" else "▼")
        expandIcon.foreground = Color(187, 187, 187)
        expandIcon.horizontalAlignment = SwingConstants.RIGHT

        panel.add(fileLabel, BorderLayout.CENTER)
        panel.add(expandIcon, BorderLayout.EAST)

        panel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                isExpanded = !isExpanded
                expandIcon.text = if (isExpanded) "▲" else "▼"
                codePanel.isVisible = isExpanded
                revalidate()
                repaint()
            }
        })

        return panel
    }

    private fun createCodeEditorPane(code: String, isDiff: Boolean): JComponent {
        val kit = HTMLEditorKit()
        editorPane.editorKit = kit

        val styleSheet = StyleSheet()
        styleSheet.addRule("body { font-family: monospace; font-size: 12px; background-color: #2d2d2d; color: #d4d4d4; padding: 6px 10px; margin: 0; }")
        styleSheet.addRule("pre { margin: 0; white-space: pre-wrap; font-family: Consolas, 'JetBrains Mono', monospace; font-size: 12px;}")

        styleSheet.addRule(".keyword { color: #569cd6; font-weight: bold; }")
        styleSheet.addRule(".string { color: #ce9178; }")
        styleSheet.addRule(".comment { color: #6a9955; }")
        styleSheet.addRule(".function { color: #dcdcaa; }")
        styleSheet.addRule(".number { color: #b5cea8; }")
        styleSheet.addRule(".operator { color: #d4d4d4; }")
        styleSheet.addRule(".variable { color: #9cdcfe; }")

        kit.styleSheet = styleSheet

        val formattedCode = if (isDiff) {
            formatDiffCode(code)
        } else {
            escapeHtml(code)
        }

        content = formattedCode

        val htmlContent = "<html><body><pre><code>$formattedCode</code></pre></body></html>"
        editorPane.text = htmlContent

        editorPane.isEditable = false
        editorPane.border = null

        val scrollablePanel = JPanel(BorderLayout())
        scrollablePanel.add(editorPane, BorderLayout.CENTER)

        return scrollablePanel
    }

    private fun formatDiffCode(diff: String): String {
        val lines = diff.split("\n")
        val formattedLines = lines.map { line ->
            when {
                line.startsWith("+") -> "<span style=\"background-color: #294436; color: #b5cea8;\">$line</span>"
                line.startsWith("-") -> "<span style=\"background-color: #40303b; color: #f88070;\">$line</span>"
                else -> line
            }
        }
        return formattedLines.joinToString("\n")
    }

    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }

    private fun getLanguageFromPath(path: String?): String {
        if (path == null) return "text"

        return when {
            path.endsWith(".java") -> "java"
            path.endsWith(".kt") -> "kotlin"
            path.endsWith(".js") -> "javascript"
            path.endsWith(".ts") -> "typescript"
            path.endsWith(".tsx") -> "typescript"
            path.endsWith(".jsx") -> "javascript"
            path.endsWith(".html") -> "html"
            path.endsWith(".css") -> "css"
            path.endsWith(".json") -> "json"
            path.endsWith(".py") -> "python"
            path.endsWith(".md") -> "markdown"
            path.endsWith(".xml") -> "xml"
            path.endsWith(".sh") -> "bash"
            else -> "text"
        }
    }

    fun setCode(code: String) {
        content = code
        val htmlContent = "<html><body><pre><code>$code</code></pre></body></html>"
        editorPane.text = htmlContent

        revalidate()
    }

    fun addCode(code: String) {
        content += "\n$code"
        val htmlContent = "<html><body><pre><code>$content</code></pre></body></html>"
        editorPane.text = htmlContent

        revalidate()
    }

    fun addPath(path: String) {
        add(createHeaderPanel(path), BorderLayout.NORTH)
    }
}