package com.github.kavos113.clinetest.ui.css

import com.intellij.ide.ui.UISettings
import com.intellij.openapi.editor.colors.EditorColorsManager

val GENERAL_CHAT_STYLE_SHEET = """
body {
    font-family: ${UISettings.getInstance().fontFace};
    font-size: 8px;
    color: #000000;
    margin: 0;
    padding: 0;
    text-align: left;
}
p {
    margin: 0;
}
h1, h2, h3, h4, h5, h6 {
    margin: 0.25em 0;
}
"""

val CODE_BLOCK_STYLE_SHEET = """
pre {
    font-family: ${EditorColorsManager.getInstance().globalScheme.editorFontName};
    font-size: ${EditorColorsManager.getInstance().globalScheme.editorFontSize}px;
    color: #000000;
    white-space: pre-wrap;
    margin: 0;
}
"""