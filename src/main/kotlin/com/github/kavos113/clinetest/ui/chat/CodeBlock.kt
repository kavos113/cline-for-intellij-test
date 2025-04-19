package com.github.kavos113.clinetest.ui.chat

import com.intellij.diff.editor.DiffFileType
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.application.ApplicationManager
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
    project: Project
) : JPanel(BorderLayout()) {

    private val field: EditorTextField

    init {
        val document = EditorFactory.getInstance().createDocument(code ?: diff ?: "")
        val fileType = ApplicationManager.getApplication().runReadAction<FileType> {
            if (diff == null) {
                getFileTypes(path?.substringAfterLast(".") ?: "txt")
            } else {
                FileTypes.PLAIN_TEXT
            }
        }

        field = EditorTextField(document, project, fileType, true, false).apply {
            addSettingsProvider { editorEx ->
                ApplicationManager.getApplication().runReadAction {
                    editorEx.colorsScheme = EditorColorsManager.getInstance().globalScheme

                    if (diff != null) {
                        text = diff
                        setDiff(diff, editorEx)
                    }
                }
            }
        }

        println(path)

        add(field, BorderLayout.CENTER)
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
        ApplicationManager.getApplication().runReadAction {
            editor.markupModel.addRangeHighlighter(
                startOffset,
                endOffset,
                HighlighterLayer.ADDITIONAL_SYNTAX,
                attributes,
                HighlighterTargetArea.LINES_IN_RANGE
            )
        }
    }

    fun setCode(code: String?) {
        ApplicationManager.getApplication().runWriteAction {
            field.text = code ?: ""
            field.revalidate()
        }
    }

    fun addCode(code: String?) {
        ApplicationManager.getApplication().runWriteAction {
            field.text += "\n$code"
            field.revalidate()
        }
    }
}