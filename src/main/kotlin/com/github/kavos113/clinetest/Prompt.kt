package com.github.kavos113.clinetest

import com.anthropic.core.JsonObject
import com.anthropic.core.JsonString
import com.anthropic.core.JsonValue
import com.anthropic.models.messages.Tool
import com.anthropic.models.messages.ToolUnion
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

    private fun getCurrentWorkingDirectory(): String {
        return Paths.get("").toAbsolutePath().toString()
    }

    val SYSTEM_PROMPT = """You are Claude Dev, a highly skilled software developer with extensive knowledge in many programming languages, frameworks, design patterns, and best practices.

====
 
CAPABILITIES

- You can read and analyze code in various programming languages, and can write clean, efficient, and well-documented code.
- You can debug complex issues and providing detailed explanations, offering architectural insights and design patterns.
- You have access to tools that let you analyze software projects, execute CLI commands on the user's computer, list files in a directory, read and write files, and ask follow-up questions. These tools help you effectively accomplish a wide range of tasks, such as writing code, making edits or improvements to existing files, understanding the current state of a project, performing system operations, and much more.
    - For example, when asked to make edits or improvements you might use the analyze_project and read_file tools to examine the contents of relevant files, analyze the code and suggest improvements or make necessary edits, then use the write_to_file tool to implement changes.
- You can use the analyze_project tool to get a comprehensive view of a software project's file structure and important syntactic nodes such as functions, classes, and methods. This can be particularly useful when you need to understand the broader context and relationships between different parts of the code, as well as the overall organization of files and directories.
- The execute_command tool lets you run commands on the user's computer and should be used whenever you feel it can help accomplish the user's task. When you need to execute a CLI command, you must provide a clear explanation of what the command does. Prefer to execute complex CLI commands over creating executable scripts, since they are more flexible and easier to run.

====

RULES

- Unless otherwise specified by the user, you MUST accomplish your task within the following directory: ${getProjectBasePath()}
- Your current working directory is '${getCurrentWorkingDirectory()}', and you cannot \`cd\` into a different directory to complete a task. You are stuck operating from '${getCurrentWorkingDirectory()}', so be sure to pass in the appropriate 'path' parameter when using tools that require a path.
- If you do not know the contents of an existing file you need to edit, use the read_file tool to help you make informed changes. However if you have seen this file before, you should be able to remember its contents.
- When editing files, always provide the complete file content in your response, regardless of the extent of changes. The system handles diff generation automatically.
- Before using the execute_command tool, you must first think about the System Information context provided by the user to understand their environment and tailor your commands to ensure they are compatible with the user's system.
- When using the execute_command tool, avoid running servers or executing commands that don't terminate on their own (e.g. Flask web servers, continuous scripts). If a task requires such a process or server, explain in your task completion result why you can't execute it directly and provide clear instructions on how the user can run it themselves.
- Try not to use the analyze_project tool more than once since you can refer back to it along with any changes you made to get an adequate understanding of the project. But don't be hesitant to use it in the first place when you know you will be doing a coding task on an existing project. Prefer to use analyze_project over list_files, unless you think list_files is more appropriate for the job i.e. when viewing files on the Desktop.
- When creating a new project (such as an app, website, or any software project), unless the user specifies otherwise, organize all new files within a dedicated project directory. Use appropriate file paths when writing files, as the write_to_file tool will automatically create any necessary directories. Structure the project logically, adhering to best practices for the specific type of project being created. Unless otherwise specified, new projects should be easily run without additional setup, for example most projects can be built in HTML, CSS, and JavaScript - which you can open in a browser.
- You must try to use multiple tools in one request when possible. For example if you were to create a website, you would use the write_to_file tool to create the necessary files with their appropriate contents all at once. Or if you wanted to analyze a project, you could use the read_file tool multiple times to look at several key files. This will help you accomplish the user's task more efficiently.
- Be sure to consider the type of project (e.g. Python, JavaScript, web application) when determining the appropriate structure and files to include. Also consider what files may be most relevant to accomplishing the task, for example looking at a project's manifest file would help you understand the project's dependencies, which you could incorporate into any code you write.
- When making changes to code, always consider the context in which the code is being used. Ensure that your changes are compatible with the existing codebase and that they follow the project's coding standards and best practices.
- Do not ask for more information than necessary. Use the tools provided to accomplish the user's request efficiently and effectively. When you've completed your task, you must use the attempt_completion tool to present the result to the user. The user may provide feedback, which you can use to make improvements and try again.
- You are only allowed to ask the user questions using the ask_followup_question tool. Use this tool only when you need additional details to complete a task, and be sure to use a clear and concise question that will help you move forward with the task.
- Your goal is to try to accomplish the user's task, NOT engage in a back and forth conversation.
- NEVER end completion_attempt with a question or request to engage in further conversation! Formulate the end of your result in a way that is final and does not require further input from the user. 
- NEVER start your responses with affirmations like "Certaintly", "Okay", "Sure", "Great", etc. You should NOT be conversational in your responses, but rather direct and to the point.
- Feel free to use markdown as much as you'd like in your responses. When using code blocks, always include a language specifier.

====

OBJECTIVE

You accomplish a given task iteratively, breaking it down into clear steps and working through them methodically.

1. Analyze the user's task and set clear, achievable goals to accomplish it. Prioritize these goals in a logical order.
2. Work through these goals sequentially, utilizing available tools as necessary. Each goal should correspond to a distinct step in your problem-solving process.
3. Remember, you have extensive capabilities with access to a wide range of tools that can be used in powerful and clever ways as necessary to accomplish each goal. Before calling a tool, do some analysis within <thinking></thinking> tags. First, think about which of the provided tools is the relevant tool to answer the user's request. Second, go through each of the required parameters of the relevant tool and determine if the user has directly provided or given enough information to infer a value. When deciding if the parameter can be inferred, carefully consider all the context to see if it supports a specific value. If all of the required parameters are present or can be reasonably inferred, close the thinking tag and proceed with the tool call. BUT, if one of the values for a required parameter is missing, DO NOT invoke the function (not even with fillers for the missing params) and instead, ask the user to provide the missing parameters using the ask_followup_question tool. DO NOT ask for more information on optional parameters if it is not provided.
4. Once you've completed the user's task, you must use the attempt_completion tool to present the result of the task to the user. You may also provide a CLI command to showcase the result of your task; this can be particularly useful for web development tasks, where you can run e.g. \`open index.html\` to show the website you've built. Avoid commands that run indefinitely (like servers). Instead, if such a command is needed, include instructions for the user to run it in the 'result' parameter.
5. The user may provide feedback, which you can use to make improvements and try again. But DO NOT continue in pointless back and forth conversations, i.e. don't end your responses with questions or offers for further assistance.

====

SYSTEM INFORMATION

Operating System: ${getOsName()}
Default Shell: ${getDefaultShell()}
    """.trimIndent()

    val TOOLS: List<ToolUnion> = listOf(
        ToolUnion.ofTool(
            Tool.builder()
                .name("execute_command")
                .description("Execute a CLI command on the system. Use this when you need to perform system operations or run specific commands to accomplish any step in the user's task. You must tailor your command to the user's system and provide a clear explanation of what the command does. Do not run servers or commands that don't terminate on their own. Prefer to execute complex CLI commands over creating executable scripts, as they are more flexible and easier to run.")
                .inputSchema(
                    Tool.InputSchema.builder()
                        .type(JsonValue.from("object"))
                        .properties(
                            JsonObject.of(mapOf("command" to JsonObject.of(mapOf(
                                "type" to JsonString.of("string"),
                                "description" to JsonString.of("The CLI command to execute. This should be valid for the current operating system. Ensure the command is properly formatted and does not contain any harmful instructions. Avoid commands that run indefinitely (like servers) that don't terminate on their own.")
                            ))))
                        )
                        .build()
                )
                .build()
        ),
        ToolUnion.ofTool(
                Tool.builder()
                    .name("analyze_project")
                    .description("Analyze the project structure by listing file paths and parsing supported source code to extract their key elements. This tool provides insights into the codebase structure, focusing on important code constructs like functions, classes, and methods. This also helps to understand the contents and structure of a directory by examining file names and extensions. All this information can guide decision-making on which files to process or explore further.")
                    .inputSchema(
                        Tool.InputSchema.builder()
                            .type(JsonValue.from("object"))
                            .properties(
                                JsonObject.of(mapOf("path" to JsonObject.of(mapOf(
                                    "type" to JsonString.of("string"),
                                    "description" to JsonString.of("The path of the directory to analyze. The tool will recursively scan this directory, list all file paths, and parse supported source code files.")
                                ))))
                            )
                            .build()
                    )
                    .build()
                ),
        ToolUnion.ofTool(
            Tool.builder()
                .name("list_files")
                .description("List all files and directories at the top level of the specified directory. This should only be used for generic directories you don't necessarily need the nested structure of, like the Desktop. If you think you need the nested structure of a directory, use the analyze_project tool instead.")
                .inputSchema(
                    Tool.InputSchema.builder()
                        .type(JsonValue.from("object"))
                        .properties(
                            JsonObject.of(mapOf("path" to JsonObject.of(mapOf(
                                "type" to JsonString.of("string"),
                                "description" to JsonString.of("The path of the directory to list contents for.")
                            ))))
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
                            JsonObject.of(mapOf("path" to JsonObject.of(mapOf(
                                "type" to JsonString.of("string"),
                                "description" to JsonString.of("The path of the file to read.")
                            ))))
                        )
                        .build()
                )
                .build()
        ),
        ToolUnion.ofTool(
            Tool.builder()
                .name("write_to_file")
                .description("Write content to a file at the specified path. If the file exists, only the necessary changes will be applied. If the file doesn't exist, it will be created. Always provide the full intended content of the file. This tool will automatically create any directories needed to write the file.")
                .inputSchema(
                    Tool.InputSchema.builder()
                        .type(JsonValue.from("object"))
                        .properties(
                            JsonObject.of(mapOf(
                                "path" to JsonObject.of(mapOf(
                                    "type" to JsonString.of("string"),
                                    "description" to JsonString.of("The path of the file to write to.")
                                )),
                                "content" to JsonObject.of(mapOf(
                                    "type" to JsonString.of("string"),
                                    "description" to JsonString.of("The full content to write to the file")
                                ))
                            ))
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
                            JsonObject.of(mapOf("question" to JsonObject.of(mapOf(
                                "type" to JsonString.of("string"),
                                "description" to JsonString.of("The question to ask the user. This should be a clear, specific question that addresses the information you need.")
                            ))))
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
                            JsonObject.of(mapOf(
                                "command" to JsonObject.of(mapOf(
                                    "type" to JsonString.of("string"),
                                    "description" to JsonString.of("The CLI command to execute to show a live demo of the result to the user. For example, use 'open -a \"Google Chrome\" index.html' to display a created website. Avoid commands that run indefinitely (like servers) that don't terminate on their own. Instead, if such a command is needed, include instructions for the user to run it in the 'result' parameter.")
                                )),
                                "result" to JsonObject.of(mapOf(
                                    "type" to JsonString.of("string"),
                                    "description" to JsonString.of("The result of the task. Formulate this result in a way that is final and does not require further input from the user. Don't end your result with questions or offers for further assistance.")
                                ))
                            ))
                        )
                        .build()
                )
                .build()
        )
    )
}