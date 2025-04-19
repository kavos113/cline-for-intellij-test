package com.github.kavos113.clinetest.shared.tool

enum class ToolName {
  WriteToFile,
  ReadFile,
  AnalyzeProject,
  ListFiles,
  ExecuteCommand,
  AskFollowupQuestion,
  AttemptCompletion;

  companion object {
    fun fromString(name: String): ToolName {
      return when (name) {
        "write_to_file" -> WriteToFile
        "read_file" -> ReadFile
        "analyze_project" -> AnalyzeProject
        "list_files" -> ListFiles
        "execute_command" -> ExecuteCommand
        "ask_followup_question" -> AskFollowupQuestion
        "attempt_completion" -> AttemptCompletion
        else -> throw IllegalStateException("Unknown tool name: $name")
      }
    }
  }
}