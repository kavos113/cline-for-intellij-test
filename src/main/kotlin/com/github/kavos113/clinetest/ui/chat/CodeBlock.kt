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
            }
            fileType = getFileTypes(path?.substringAfterLast(".") ?: "txt")

            editor?.markupModel?.addLineHighlighter(1, HighlighterLayer.ADDITIONAL_SYNTAX, TextAttributes().apply { backgroundColor = JBColor.RED })
        }

        if (diff != null) {
            field.text = diff
            setDiff(diff)
        }

        add(field)
    }

    private fun getFileTypes(ext: String): FileType {
        return FileTypeManager.getInstance().getFileTypeByExtension(ext)
    }

    private fun setDiff(diff: String) {
        val editor = field.editor
        if (editor != null) {
            val lines = diff.split("\n")

            var offset = 0

            for (line in lines) {
                if (line.startsWith("+")) {
                    addHighlight(editor, offset, offset + line.length, Color.GREEN)
                } else if (line.startsWith("-")) {
                    addHighlight(editor, offset, offset + line.length, Color.RED)
                }

                offset += line.length + 1
            }
        }
    }

    private fun addHighlight(editor: Editor, startOffset: Int, endOffset: Int, color: Color) {
        val attributes = TextAttributes()
        attributes.backgroundColor = color
        editor.markupModel.addRangeHighlighter(
            startOffset,
            endOffset,
            HighlighterLayer.ADDITIONAL_SYNTAX,
            attributes,
            HighlighterTargetArea.EXACT_RANGE
        )
    }

    fun setCode(code: String?) {
        field.text = code ?: ""
    }

    fun addCode(code: String?) {
        field.text += "\n$code"
    }
}