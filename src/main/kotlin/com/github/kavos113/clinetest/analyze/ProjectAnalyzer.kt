package com.github.kavos113.clinetest.analyze

import com.github.kavos113.clinetest.analyze.queries.CPP_QUERY
import com.github.kavos113.clinetest.analyze.queries.C_QUERY
import com.github.kavos113.clinetest.analyze.queries.C_SHARP_QUERY
import com.github.kavos113.clinetest.analyze.queries.GO_QUERY
import com.github.kavos113.clinetest.analyze.queries.JAVA_QUERY
import com.github.kavos113.clinetest.analyze.queries.JS_QUERY
import com.github.kavos113.clinetest.analyze.queries.PHP_QUERY
import com.github.kavos113.clinetest.analyze.queries.PYTHON_QUERY
import com.github.kavos113.clinetest.analyze.queries.RUBY_QUERY
import com.github.kavos113.clinetest.analyze.queries.RUST_QUERY
import com.github.kavos113.clinetest.analyze.queries.SWIFT_QUERY
import com.github.kavos113.clinetest.analyze.queries.TS_QUERY
import com.intellij.openapi.diagnostic.thisLogger
import org.eclipse.jgit.ignore.IgnoreNode
import org.treesitter.TSLanguage
import org.treesitter.TSParser
import org.treesitter.TSQuery
import org.treesitter.TSQueryCursor
import org.treesitter.TSQueryMatch
import org.treesitter.TreeSitterC
import org.treesitter.TreeSitterCSharp
import org.treesitter.TreeSitterCpp
import org.treesitter.TreeSitterGo
import org.treesitter.TreeSitterJava
import org.treesitter.TreeSitterJavascript
import org.treesitter.TreeSitterPhp
import org.treesitter.TreeSitterPython
import org.treesitter.TreeSitterRuby
import org.treesitter.TreeSitterRust
import org.treesitter.TreeSitterSwift
import org.treesitter.TreeSitterTypescript
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Path

object ProjectAnalyzer {
  private data class ParserWithQuery(
    val parser: TSParser,
    val query: TSQuery
  )

  private data class LanguageParsers(
    val parsers: Map<String, ParserWithQuery>
  )

  fun analyzeProject(dirPath: Path): String {
    val files = getAllProjectFiles(dirPath)
    val (filesToParse, filesToIgnore) = extractFilesToParse(files)
    val parsers = loadRequiredLanguageParsers(filesToParse)

    val fileWithOutDefinitions = mutableListOf<File>()
    val result = StringBuilder()

    files.forEach { file ->
      val definitions = parseFile(file, parsers)
      if (definitions != null) {
        if (result.isEmpty()) {
          result.append("# Source code definitions:\n\n")
        }
        val relativePath = dirPath.relativize(file.toPath()).toString().replace("\\", "/")
        result.append("$relativePath$definitions\n")
      } else {
        fileWithOutDefinitions.add(file)
      }
    }

    result.append("# Unparsed files:\n\n")
    fileWithOutDefinitions.forEach { file ->
      val relativePath = dirPath.relativize(file.toPath()).toString().replace("\\", "/")
      result.append("$relativePath\n")
    }
    filesToIgnore.forEach { file ->
      val relativePath = dirPath.relativize(file.toPath()).toString().replace("\\", "/")
      result.append("$relativePath\n")
    }

    return result.toString()
  }

  fun getAllProjectFiles(dirPath: Path): List<File> {
    val dirsToIgnore = listOf(
      "node_modules",
      "__pycache__",
      "env",
      "venv",
      "target/dependency",
      "build/dependencies",
      "dist",
      "out",
      "bundle",
      "vendor",
      "tmp",
      "temp",
      "deps",
      "pkg",
      "Pods",
      ".*",
    ).map { "**/$it/**" }

    val ignoreNode = IgnoreNode()

    dirsToIgnore.forEach { pattern ->
      ignoreNode.parse(ByteArrayInputStream(pattern.toByteArray()))
    }

    val gitignoreFile = dirPath.resolve(".gitignore").toFile()
    if (gitignoreFile.exists()) {
      ignoreNode.parse(ByteArrayInputStream(gitignoreFile.readText().toByteArray()))
    }

    return collectFiles(dirPath.toFile(), dirPath.toFile(), ignoreNode)
  }

  private fun collectFiles(rootDir: File, currentDir: File, ignoreNode: IgnoreNode): List<File> {
    val result = mutableListOf<File>()

    currentDir.listFiles()?.forEach { file ->
      val relativePath = rootDir.toPath().relativize(file.toPath()).toString().replace("\\", "/")

      val matchResult = ignoreNode.isIgnored(relativePath, file.isDirectory)
      val isIgnored = matchResult == IgnoreNode.MatchResult.IGNORED

      if (!isIgnored) {
        if (file.isDirectory) {
          val gitignoreFile = File(file, ".gitignore")
          if (gitignoreFile.exists()) {
            val rules = ignoreNode.rules.map { it }
            val dirIgnoreNode = IgnoreNode(rules)
            dirIgnoreNode.parse(ByteArrayInputStream(gitignoreFile.readText().toByteArray()))

            result.addAll(collectFiles(rootDir, file, dirIgnoreNode))
          } else {
            result.addAll(collectFiles(rootDir, file, ignoreNode))
          }
        } else {
          result.add(file)
        }
      }
    }

    return result
  }
  
  private fun extractFilesToParse(files: List<File>): Pair<List<File>, List<File>> {
    val extentions = listOf(
      "js",
      "jsx",
      "ts",
      "tsx",
      "py",
      "rs",
      "go",
      "c",
      "h",
      "cpp",
      "hpp",
      "cs",
      "rb",
      "java",
      "php",
      "swift",
    )

    val filesToParse = mutableListOf<File>()
    val filesToIgnore = mutableListOf<File>()

    files.forEach { file ->
      if (file.extension in extentions) {
        filesToParse.add(file)
      } else {
        filesToIgnore.add(file)
      }
    }

    return Pair(filesToParse, filesToIgnore)
  }

  private fun loadRequiredLanguageParsers(files: List<File>): LanguageParsers {
    val parsers = mutableMapOf<String, ParserWithQuery>()

    files.forEach { file ->
      val ext = file.extension

      var language: TSLanguage
      var query: TSQuery

      when(ext) {
        "js", "jsx" -> {
          language = TreeSitterJavascript()
          query = TSQuery(language, JS_QUERY)
        }
        "ts", "tsx" -> {
          language = TreeSitterTypescript()
          query = TSQuery(language, TS_QUERY)
        }
        "py" -> {
          language = TreeSitterPython()
          query = TSQuery(language, PYTHON_QUERY)
        }
        "rs" -> {
          language = TreeSitterRust()
          query = TSQuery(language, RUST_QUERY)
        }
        "go" -> {
          language = TreeSitterGo()
          query = TSQuery(language, GO_QUERY)
        }
        "cpp", "hpp", "cc", "cxx", "c++", "h" -> {
          language = TreeSitterCpp()
          query = TSQuery(language, CPP_QUERY)
        }
        "c" -> {
          language = TreeSitterC()
          query = TSQuery(language, C_QUERY)
        }
        "cs" -> {
          language = TreeSitterCSharp()
          query = TSQuery(language, C_SHARP_QUERY)
        }
        "rb" -> {
          language = TreeSitterRuby()
          query = TSQuery(language, RUBY_QUERY)
        }
        "java" -> {
          language = TreeSitterJava()
          query = TSQuery(language, JAVA_QUERY)
        }
        "php" -> {
          language = TreeSitterPhp()
          query = TSQuery(language, PHP_QUERY)
        }
        "swift" -> {
          language = TreeSitterSwift()
          query = TSQuery(language, SWIFT_QUERY)
        }
        else -> {
          thisLogger().warn("Unsupported file extension: $ext")
          return@forEach
        }
      }

      val parser = TSParser()
      parser.language = language

      parsers[ext] = ParserWithQuery(parser, query)
    }

    return LanguageParsers(parsers)
  }

  private fun parseFile(file: File, parsers: LanguageParsers): String? {
    val content = file.readText(Charsets.UTF_8)

    val parser = parsers.parsers[file.extension]?.parser
    val query = parsers.parsers[file.extension]?.query
    if (parser == null || query == null) {
      return null
    }

    val tree = parser.parseString(null, content)
    val cursor = TSQueryCursor()
    cursor.exec(query, tree.rootNode)
    val match = TSQueryMatch()

    val lines = content.split("\n")
    val formattedOutput = StringBuilder()
    var lastLine = -1
    while (cursor.nextMatch(match)) {
      val captures = match.captures.sortedBy { it.node.startPoint.row }
      captures.forEach { capture ->
        val node = capture.node
        val startLine = node.startPoint.row
        val endLine = node.endPoint.row

        if (node.type.contains("identifier") && lines[startLine].isNotBlank()) {
          if (lastLine != -1) {
            formattedOutput.append("|----\n")
          }
          formattedOutput.append("|${lines[startLine]}\n")
        }

        lastLine = endLine
      }
    }

    if (formattedOutput.isNotEmpty()) {
      return "\n$formattedOutput|----\n"
    } else {
      return null
    }
  }
}
