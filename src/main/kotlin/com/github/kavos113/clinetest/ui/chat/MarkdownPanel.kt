package com.github.kavos113.clinetest.ui.chat

import com.github.kavos113.clinetest.ui.css.GENERAL_CHAT_STYLE_SHEET
import com.intellij.ide.ui.UISettings
import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.util.ui.UIUtil
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import java.awt.Dimension
import javax.swing.BoxLayout
import javax.swing.JEditorPane
import javax.swing.JPanel
import javax.swing.text.html.HTMLEditorKit

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
    content: String,
    project: Project
): JPanel() {

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)

        val parser = Parser.builder().build()
        val renderer = HtmlRenderer.builder().build()

        val contents = content.split("```")

        contents.forEachIndexed { i, s ->
            if (s.trim().isEmpty()) {
                return@forEachIndexed
            }
            if (i % 2 == 0) {
                val htmlContent = renderer.render(parser.parse(s))
                val editorPane = createEditorPane(htmlContent)

                add(editorPane)
            } else {
                ApplicationManager.getApplication().invokeLater {
                    val lang = s.substringBefore("\n").trim()
                    val codeContent = s.substring(lang.length).trim()
                    val codeBlock = CodeBlock(
                        code = codeContent,
                        diff = null,
                        path = "code.${getExtFromLanguage(lang)}",
                        isExpanded = true,
                        project = project
                    )

                    codeBlock.size = codeBlock.preferredSize
                    add(codeBlock)

                    revalidate()
                    repaint()
                }
            }
        }

        border = UIUtil.getTextFieldBorder()
    }

    private fun createEditorPane(content: String): JEditorPane {
        val editorPane = JEditorPane("text/html", "")

        val kit = editorPane.editorKit as HTMLEditorKit
        val styleSheet = kit.styleSheet
        styleSheet.addRule(GENERAL_CHAT_STYLE_SHEET)

        editorPane.text = "<html><body>$content</body></html>"
        editorPane.isEditable = false
        editorPane.background = null
        editorPane.isOpaque = false
        editorPane.font = UIUtil.getLabelFont()

        return editorPane
    }

    private fun getExtFromLanguage(language: String): String {
        return ReadAction.compute<String, Throwable> {
            FileTypeManager.getInstance().findFileTypeByLanguage(
                Language.findLanguageByID(language) ?: Language.ANY
            )?.defaultExtension ?: "txt"
        }
    }
}
