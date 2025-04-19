package com.github.kavos113.clinetest.ui.chat

import com.github.kavos113.clinetest.ui.css.CODE_BLOCK_STYLE_SHEET
import com.github.kavos113.clinetest.ui.css.GENERAL_CHAT_STYLE_SHEET
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.ui.JBFont
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import org.intellij.plugins.markdown.ui.preview.html.MarkdownUtil
import java.awt.GridLayout
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
): JPanel(GridLayout(1, 1)) {
    private val editorPane: JEditorPane

    init {
        val parser = Parser.builder().build()
        val renderer = HtmlRenderer.builder().build()

        val htmlContent = renderer.render(parser.parse(content))

        println(htmlContent)

        editorPane = JEditorPane("text/html", "")

        val kit = editorPane.editorKit as HTMLEditorKit
        println("${kit.contentType}, ${kit.styleSheet}")

        kit.styleSheet.addRule(GENERAL_CHAT_STYLE_SHEET)
        kit.styleSheet.addRule(CODE_BLOCK_STYLE_SHEET)

        editorPane.text = "<html><body>$htmlContent</body></html>"
        editorPane.isEditable = false
        editorPane.background = null
        editorPane.isOpaque = false

        add(editorPane)
    }
}
