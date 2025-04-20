package com.github.kavos113.clinetest

import com.anthropic.core.JsonObject
import com.anthropic.core.JsonString
import com.anthropic.core.JsonValue
import com.anthropic.models.messages.Tool
import com.anthropic.models.messages.ToolUnion
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import java.io.File
import java.nio.file.Paths

object Prompt {
  private fun getOsName(): String {
    val osName = System.getProperty("os.name") ?: "Unknown"
    val osVersion = System.getProperty("os.version") ?: ""
    val osArch = System.getProperty("os.arch") ?: ""

    return when {
      osName.startsWith("Windows") -> "Windows $osVersion ($osArch)"
      osName.startsWith("Mac") -> "macOS $osVersion ($osArch)"
      osName.startsWith("Linux") -> "Linux $osVersion ($osArch)"
      else -> "$osName $osVersion ($osArch)"
    }
  }

  private fun getDefaultShell(): String {
    return when {
      System.getProperty("os.name").startsWith("Windows") -> {
        // Windows
        System.getenv("COMSPEC") ?: "cmd.exe"
      }

      else -> {
        // Unix-like
        try {
          val processBuilder = ProcessBuilder("bash", "-c", "echo \$SHELL")
          val process = processBuilder.start()
          val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
          process.waitFor()

          output.ifEmpty { "/bin/bash" }
        } catch (e: Exception) {
          "/bin/bash"
        }
      }
    }
  }

  private fun getCurrentProject(): Project? {
    return ProjectManager.getInstance().openProjects.firstOrNull()
  }

  private fun getProjectBasePath(): String {
    val project = getCurrentProject()
    return project?.basePath ?: (System.getProperty("user.home") + File.pathSeparator + "Desktop")
  }

  private fun getCwd(): String {
    return Paths.get("").toAbsolutePath().toString()
  }

  private fun getOpeningFilePath(): String {
    val project = getCurrentProject()
    val editor = FileEditorManager.getInstance(project!!).selectedTextEditor
    val file = FileDocumentManager.getInstance().getFile(editor?.document ?: return "")
    return file?.path ?: ""
  }

  private fun getTabFilePaths(): List<String> {
    val project = getCurrentProject()
    val files = FileEditorManager.getInstance(project!!).openFiles
    return files.map { it.path }
  }

  val SYSTEM_PROMPT =
    """You are Claude Dev, a highly skilled software developer with extensive knowledge in many programming languages, frameworks, design patterns, and best practices.

====
 
CAPABILITIES

- You can read and analyze code in various programming languages, and can write clean, efficient, and well-documented code.
- You can debug complex issues and providing detailed explanations, offering architectural insights and design patterns.
- You have access to tools that let you execute CLI commands on the user's computer, list files in a directory (top level or recursively), extract source code definitions, read and write files, and ask follow-up questions. These tools help you effectively accomplish a wide range of tasks, such as writing code, making edits or improvements to existing files, understanding the current state of a project, performing system operations, and much more.
- You can use the list_files_recursive tool to get an overview of the project's file structure, which can provide key insights into the project from directory/file names (how developers conceptualize and organize their code) or file extensions (the language used). The list_files_top_level tool is better suited for generic directories you don't necessarily need the nested structure of, like the Desktop.
- You can use the view_source_code_definitions_top_level tool to get an overview of source code definitions for all files at the top level of a specified directory. This can be particularly useful when you need to understand the broader context and relationships between certain parts of the code. You may need to call this tool multiple times to understand various parts of the codebase related to the task.
	- For example, when asked to make edits or improvements you might use list_files_recursive to get an overview of the project's file structure, then view_source_code_definitions_top_level to get an overview of source code definitions for files located in relevant directories, then read_file to examine the contents of relevant files, analyze the code and suggest improvements or make necessary edits, then use the write_to_file tool to implement changes.
- The execute_command tool lets you run commands on the user's computer and should be used whenever you feel it can help accomplish the user's task. When you need to execute a CLI command, you must provide a clear explanation of what the command does. Prefer to execute complex CLI commands over creating executable scripts, since they are more flexible and easier to run. Interactive and long-running commands are allowed, since the user has the ability to send input to stdin and terminate the command on their own if needed.

====

RULES

- Your current working directory is: ${getCwd()}
- You cannot \`cd\` into a different directory to complete a task. You are stuck operating from '${getCwd()}', so be sure to pass in the correct 'path' parameter when using tools that require a path.
- Do not use the ~ character or ${'$'}HOME to refer to the home directory.
- Before using the execute_command tool, you must first think about the SYSTEM INFORMATION context provided to understand the user's environment and tailor your commands to ensure they are compatible with their system. You must also consider if the command you need to run should be executed in a specific directory outside of the current working directory '${getCwd()}', and if so prepend with \`cd\`'ing into that directory && then executing the command (as one command since you are stuck operating from '${getCwd()}'). For example, if you needed to run \`npm install\` in a project outside of '${getCwd()}', you would need to prepend with a \`cd\` i.e. pseudocode for this would be \`cd (path to project) && (command, in this case npm install)\`.
- When editing files, always provide the complete file content in your response, regardless of the extent of changes. The system handles diff generation automatically.
- If you need to read or edit a file you have already read or edited, you can assume its contents have not changed since then (unless specified otherwise by the user) and skip using the read_file tool before proceeding.
- When creating a new project (such as an app, website, or any software project), organize all new files within a dedicated project directory unless the user specifies otherwise. Use appropriate file paths when writing files, as the write_to_file tool will automatically create any necessary directories. Structure the project logically, adhering to best practices for the specific type of project being created. Unless otherwise specified, new projects should be easily run without additional setup, for example most projects can be built in HTML, CSS, and JavaScript - which you can open in a browser.
- Be sure to consider the type of project (e.g. Python, JavaScript, web application) when determining the appropriate structure and files to include. Also consider what files may be most relevant to accomplishing the task, for example looking at a project's manifest file would help you understand the project's dependencies, which you could incorporate into any code you write.
- When making changes to code, always consider the context in which the code is being used. Ensure that your changes are compatible with the existing codebase and that they follow the project's coding standards and best practices.
- Do not ask for more information than necessary. Use the tools provided to accomplish the user's request efficiently and effectively. When you've completed your task, you must use the attempt_completion tool to present the result to the user. The user may provide feedback, which you can use to make improvements and try again.
- You are only allowed to ask the user questions using the ask_followup_question tool. Use this tool only when you need additional details to complete a task, and be sure to use a clear and concise question that will help you move forward with the task.
- Your goal is to try to accomplish the user's task, NOT engage in a back and forth conversation.
- NEVER end completion_attempt with a question or request to engage in further conversation! Formulate the end of your result in a way that is final and does not require further input from the user. 
- NEVER start your responses with affirmations like "Certaintly", "Okay", "Sure", "Great", etc. You should NOT be conversational in your responses, but rather direct and to the point.
- Feel free to use markdown as much as you'd like in your responses. When using code blocks, always include a language specifier.
- When presented with images, utilize your vision capabilities to thoroughly examine them and extract meaningful information. Incorporate these insights into your thought process as you accomplish the user's task.

====

OBJECTIVE

You accomplish a given task iteratively, breaking it down into clear steps and working through them methodically.

1. Analyze the user's task and set clear, achievable goals to accomplish it. Prioritize these goals in a logical order.
2. Work through these goals sequentially, utilizing available tools as necessary. Each goal should correspond to a distinct step in your problem-solving process. It is okay for certain steps to take multiple iterations, i.e. if you need to create many files but are limited by your max output limitations, it's okay to create a few files at a time as each subsequent iteration will keep you informed on the work completed and what's remaining. 
3. Remember, you have extensive capabilities with access to a wide range of tools that can be used in powerful and clever ways as necessary to accomplish each goal. Before calling a tool, do some analysis within <thinking></thinking> tags. First, think about which of the provided tools is the relevant tool to answer the user's request. Second, go through each of the required parameters of the relevant tool and determine if the user has directly provided or given enough information to infer a value. When deciding if the parameter can be inferred, carefully consider all the context to see if it supports a specific value. If all of the required parameters are present or can be reasonably inferred, close the thinking tag and proceed with the tool call. BUT, if one of the values for a required parameter is missing, DO NOT invoke the function (not even with fillers for the missing params) and instead, ask the user to provide the missing parameters using the ask_followup_question tool. DO NOT ask for more information on optional parameters if it is not provided.
4. Once you've completed the user's task, you must use the attempt_completion tool to present the result of the task to the user. You may also provide a CLI command to showcase the result of your task; this can be particularly useful for web development tasks, where you can run e.g. \`open index.html\` to show the website you've built.
5. The user may provide feedback, which you can use to make improvements and try again. But DO NOT continue in pointless back and forth conversations, i.e. don't end your responses with questions or offers for further assistance.

====

SYSTEM INFORMATION

Operating System: ${getOsName()}
Default Shell: ${getDefaultShell()}
Home Directory: ${System.getProperty("user.home")}
Current Working Directory: ${getCwd()}
IDE Visible Files: ${getOpeningFilePath()}
IDE Opened Tabs: ${getTabFilePaths().joinToString(", ")}
    """.trimIndent()

  val TOOLS: List<ToolUnion> = listOf(
    ToolUnion.ofTool(
      Tool.builder()
        .name("execute_command")
        .description("Execute a CLI command on the system. Use this when you need to perform system operations or run specific commands to accomplish any step in the user's task. You must tailor your command to the user's system and provide a clear explanation of what the command does. Prefer to execute complex CLI commands over creating executable scripts, as they are more flexible and easier to run. Commands will be executed in the current working directory: ${getCwd()}")
        .inputSchema(
          Tool.InputSchema.builder()
            .type(JsonValue.from("object"))
            .properties(
              JsonObject.of(
                mapOf(
                  "command" to JsonObject.of(
                    mapOf(
                      "type" to JsonString.of("string"),
                      "description" to JsonString.of("The CLI command to execute. This should be valid for the current operating system. Ensure the command is properly formatted and does not contain any harmful instructions.")
                    )
                  )
                )
              )
            )
            .build()
        )
        .build()
    ),
    ToolUnion.ofTool(
      Tool.builder()
        .name("list_files_top_level")
        .description("List all files and directories at the top level of the specified directory. This should only be used for generic directories you don't necessarily need the nested structure of, like the Desktop.")
        .inputSchema(
          Tool.InputSchema.builder()
            .type(JsonValue.from("object"))
            .properties(
              JsonObject.of(
                mapOf(
                  "path" to JsonObject.of(
                    mapOf(
                      "type" to JsonString.of("string"),
                      "description" to JsonString.of("The path of the directory to list contents for (relative to the current working directory ${getCwd()})")
                    )
                  )
                )
              )
            )
            .build()
        )
        .build()
    ),
    ToolUnion.ofTool(
      Tool.builder()
        .name("list_files_recursive")
        .description("Recursively list all files and directories within the specified directory. This provides a comprehensive view of the project structure, and can guide decision-making on which files to process or explore further.")
        .inputSchema(
          Tool.InputSchema.builder()
            .type(JsonValue.from("object"))
            .properties(
              JsonObject.of(
                mapOf(
                  "path" to JsonObject.of(
                    mapOf(
                      "type" to JsonString.of("string"),
                      "description" to JsonString.of("The path of the directory to recursively list contents for (relative to the current working directory ${getCwd()})")
                    )
                  )
                )
              )
            )
            .build()
        )
        .build()
    ),
    ToolUnion.ofTool(
      Tool.builder()
        .name("view_source_code_definitions_top_level")
        .description("Parse all source code files at the top level of the specified directory to extract names of key elements like classes and functions. This tool provides insights into the codebase structure and important constructs, encapsulating high-level concepts and relationships that are crucial for understanding the overall architecture.")
        .inputSchema(
          Tool.InputSchema.builder()
            .type(JsonValue.from("object"))
            .properties(
              JsonObject.of(
                mapOf(
                  "path" to JsonObject.of(
                    mapOf(
                      "type" to JsonString.of("string"),
                      "description" to JsonString.of("The path of the directory (relative to the current working directory ${getCwd()}) to parse top level source code files for to view their definitions")
                    )
                  )
                )
              )
            )
            .build()
        )
        .build()
    ),
    ToolUnion.ofTool(
      Tool.builder()
        .name("read_file")
        .description("Read the contents of a file at the specified path. Use this when you need to examine the contents of an existing file, for example to analyze code, review text files, or extract information from configuration files. Be aware that this tool may not be suitable for very large files or binary files, as it returns the raw content as a string.")
        .inputSchema(
          Tool.InputSchema.builder()
            .type(JsonValue.from("object"))
            .properties(
              JsonObject.of(
                mapOf(
                  "path" to JsonObject.of(
                    mapOf(
                      "type" to JsonString.of("string"),
                      "description" to JsonString.of("The path of the file to read (relative to the current working directory ${getCwd()})")
                    )
                  )
                )
              )
            )
            .build()
        )
        .build()
    ),
    ToolUnion.ofTool(
      Tool.builder()
        .name("write_to_file")
        .description("Write content to a file at the specified path. If the file exists, only the necessary changes will be applied. If the file doesn't exist, it will be created. Always provide the full intended content of the file, without any truncation. This tool will automatically create any directories needed to write the file.")
        .inputSchema(
          Tool.InputSchema.builder()
            .type(JsonValue.from("object"))
            .properties(
              JsonObject.of(
                mapOf(
                  "path" to JsonObject.of(
                    mapOf(
                      "type" to JsonString.of("string"),
                      "description" to JsonString.of("The path of the file to write to.")
                    )
                  ),
                  "content" to JsonObject.of(
                    mapOf(
                      "type" to JsonString.of("string"),
                      "description" to JsonString.of("The path of the file to write to (relative to the current working directory ${getCwd()})")
                    )
                  )
                )
              )
            )
            .build()
        )
        .build()
    ),
    ToolUnion.ofTool(
      Tool.builder()
        .name("ask_followup_question")
        .description("Ask the user a question to gather additional information needed to complete the task. This tool should be used when you encounter ambiguities, need clarification, or require more details to proceed effectively. It allows for interactive problem-solving by enabling direct communication with the user. Use this tool judiciously to maintain a balance between gathering necessary information and avoiding excessive back-and-forth.")
        .inputSchema(
          Tool.InputSchema.builder()
            .type(JsonValue.from("object"))
            .properties(
              JsonObject.of(
                mapOf(
                  "question" to JsonObject.of(
                    mapOf(
                      "type" to JsonString.of("string"),
                      "description" to JsonString.of("The question to ask the user. This should be a clear, specific question that addresses the information you need.")
                    )
                  )
                )
              )
            )
            .build()
        )
        .build()
    ),
    ToolUnion.ofTool(
      Tool.builder()
        .name("attempt_completion")
        .description("Once you've completed the task, use this tool to present the result to the user. They may respond with feedback if they are not satisfied with the result, which you can use to make improvements and try again.")
        .inputSchema(
          Tool.InputSchema.builder()
            .type(JsonValue.from("object"))
            .properties(
              JsonObject.of(
                mapOf(
                  "command" to JsonObject.of(
                    mapOf(
                      "type" to JsonString.of("string"),
                      "description" to JsonString.of("The CLI command to execute to show a live demo of the result to the user. For example, use 'open index.html' to display a created website. This should be valid for the current operating system. Ensure the command is properly formatted and does not contain any harmful instructions.")
                    )
                  ),
                  "result" to JsonObject.of(
                    mapOf(
                      "type" to JsonString.of("string"),
                      "description" to JsonString.of("The result of the task. Formulate this result in a way that is final and does not require further input from the user. Don't end your result with questions or offers for further assistance.")
                    )
                  )
                )
              )
            )
            .build()
        )
        .build()
    )
  )
}