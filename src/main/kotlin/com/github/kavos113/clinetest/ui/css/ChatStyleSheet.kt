package com.github.kavos113.clinetest.ui.css

import com.intellij.ide.ui.UISettings
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.util.ui.JBFont

val GENERAL_CHAT_STYLE_SHEET = """
body {
    font-family: ${UISettings.getInstance().fontFace};
    font-size: ${UISettings.getInstance().fontSize}px;
    color: #000000;
    margin: 0;
    padding: 0;
    text-align: left;
    border: 1px solid #000000;
}
"""

val CODE_BLOCK_STYLE_SHEET = """
pre {
    font-family: ${EditorColorsManager.getInstance().globalScheme.editorFontName};
    font-size: ${EditorColorsManager.getInstance().globalScheme.editorFontSize}px;
    color: #000000;
    white-space: pre-wrap;
    margin: 0;
    border: 1px solid #000000;
}
"""