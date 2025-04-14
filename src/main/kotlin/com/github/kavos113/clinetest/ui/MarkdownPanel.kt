package com.github.kavos113.clinetest.ui

import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.ui.JBFont
import org.intellij.plugins.markdown.ui.preview.html.MarkdownUtil
import java.awt.BorderLayout
import javax.swing.JEditorPane
import javax.swing.JPanel
import javax.swing.text.html.HTMLEditorKit
import javax.swing.text.html.StyleSheet

const val HTML_SAMPLE = """<html>
    <body>
        <h1>Hi, I am a software engineer. I can help you with your code.</h1>
        <h2>Overview</h2>
        <ul>
            <li>I can help you with your code.</li>
            <li>I can help you with your code.</li>
            <li>I can help you with your code.</li>
        </ul>
        <h2>Code</h2>
        <pre><code>def hello_world():
    print("Hello, world!")
hello_world()
</code></pre>
    </body>
"""

class MarkdownPanel(
    content: String
): JPanel() {
    private val htmlContent: String
    private val editorPane: JEditorPane

    init {
        val file = LightVirtualFile("content.md", content)
        htmlContent = MarkdownUtil.generateMarkdownHtml(file, content, null)

        editorPane = JEditorPane("text/html", "<html>$htmlContent</html>")
        editorPane.isEditable = false
        editorPane.background = null
        editorPane.isOpaque = false

        println(editorPane.preferredSize)

        val kit = HTMLEditorKit()
        editorPane.editorKit = kit

        val styleSheet = StyleSheet()
        styleSheet.addRule("body { font-family: '${JBFont.regular().family ?: "Segoe UI"}'; font-size: 12pt; }")

        add(editorPane)
    }
}
