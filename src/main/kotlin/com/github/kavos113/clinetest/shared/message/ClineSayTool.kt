package com.github.kavos113.clinetest.shared.message


data class ClineSayTool(
  val tool: ClineSayTools,
  val path: String? = null,
  val diff: String? = null,
  val content: String? = null,
)

enum class ClineSayTools {
  EditedExistingFile,
  NewFileCreated,
  ReadFile,
  ListFiles,
  AnalyzeProject,
}