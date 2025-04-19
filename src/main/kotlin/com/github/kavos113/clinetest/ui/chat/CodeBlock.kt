package com.github.kavos113.clinetest.ui.chat

import com.intellij.diff.editor.DiffFileType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.openapi.project.Project
import com.intellij.ui.EditorTextField
import com.intellij.ui.JBColor
import java.awt.BorderLayout
import java.awt.Color
import javax.swing.JPanel

class CodeBlock(
    code: String?,
    diff: String? = null,
    path: String? = null,
    private var isExpanded: Boolean = true,
    project: Project? = null
) : JPanel(BorderLayout()) {

    private val field: EditorTextField

    init {
        val document = EditorFactory.getInstance().createDocument(code ?: "")
        field = EditorTextField(document, project, FileTypes.PLAIN_TEXT, true, false).apply {
            addSettingsProvider { editorEx ->
                editorEx.colorsScheme = EditorColorsManager.getInstance().globalScheme
                fileType = getFileTypes(path?.substringAfterLast(".") ?: "txt")

                if (diff != null) {
                    text = diff
                    setDiff(diff, editorEx)
                }
            }
        }

        add(field)
    }

    private fun getFileTypes(ext: String): FileType {
        return FileTypeManager.getInstance().getFileTypeByExtension(ext)
    }

    private fun setDiff(diff: String, editor: Editor) {
        val lines = diff.split("\n")

        var offset = 0

        for (line in lines) {
            if (line.startsWith("+")) {
                addHighlight(editor, offset, offset + line.length, JBColor(Color(220, 255, 220, 100), Color(40, 65, 40, 100)))
            } else if (line.startsWith("-")) {
                addHighlight(editor, offset, offset + line.length, JBColor(Color(255, 220, 220, 100), Color(65, 40, 40, 100)))
            }

            offset += line.length + 1
        }
    }

    private fun addHighlight(editor: Editor, startOffset: Int, endOffset: Int, color: JBColor) {
        val attributes = TextAttributes()
        attributes.backgroundColor = color
        editor.markupModel.addRangeHighlighter(
            startOffset,
            endOffset,
            HighlighterLayer.ADDITIONAL_SYNTAX,
            attributes,
            HighlighterTargetArea.LINES_IN_RANGE
        )
    }

    fun setCode(code: String?) {
        field.text = code ?: ""
    }

    fun addCode(code: String?) {
        field.text += "\n$code"
        field.revalidate()
    }
}