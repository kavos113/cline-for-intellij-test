package com.github.kavos113.clinetest.analyze

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.openapi.util.Key
import com.intellij.util.io.delete
import java.io.File
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.jar.JarFile
import kotlin.io.path.absolutePathString

object ProjectAnalyzer {
    fun analyzeProject(dirPath: Path): String {
        val strPath = dirPath.absolutePathString()

        val tempDir = Files.createTempDirectory("analyze-project")
        val jsFile = extractResources(tempDir)

        val commandParts = if (System.getProperty("os.name").lowercase().contains("win")) {
            listOf("cmd", "/c", "node ${jsFile.absolutePath} $strPath")
        } else {
            listOf("node", jsFile.absolutePath, strPath)
        }

        val commandLine = GeneralCommandLine(commandParts)
        commandLine.charset = StandardCharsets.UTF_8

        val latch = CountDownLatch(1)

        val stringBuilder = StringBuilder()

        val processHandler = OSProcessHandler(commandLine)
        processHandler.addProcessListener(object : ProcessAdapter() {
            override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                stringBuilder.append(event.text)
            }

            override fun processTerminated(event: ProcessEvent) {
                latch.countDown()
            }
        })

        processHandler.startNotify()
        latch.await()

        jsFile.delete()
        tempDir.delete()

        return stringBuilder.toString()
    }

    private fun extractResources(tempDir: Path): File {
        val resourcePath = "tree.js"
        val inputStream = ProjectAnalyzer::class.java.classLoader.getResourceAsStream(resourcePath)
            ?: throw IllegalArgumentException("resource not found: $resourcePath")

        val tempFile = File(tempDir.toFile(), "tree.js")
        tempFile.deleteOnExit()

        inputStream.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        val resourceDir = "analyze-project"
        val resourceList: MutableList<String> = mutableListOf()

        val dirURL = ProjectAnalyzer::class.java.classLoader.getResource(resourceDir)
        if (dirURL?.protocol == "jar") {
            val jarPath = dirURL.path.substring(5, dirURL.path.indexOf("!"))
            val jar = JarFile(URLDecoder.decode(jarPath, StandardCharsets.UTF_8))
            val entries = jar.entries()

            while (entries.hasMoreElements()) {
                val entry = entries.nextElement().name
                if (entry.startsWith(resourceDir) && !entry.endsWith("/")) {
                    resourceList.add(entry)
                }
            }

            jar.close()
        } else {
            val dirFile = File(dirURL!!.toURI())
            dirFile.walkTopDown()
                .filter { it.isFile }
                .map { it.relativeTo(dirFile).path }
        }

        for (resource in resourceList) {
            val stream = ProjectAnalyzer::class.java.classLoader.getResource(resource)
                ?: continue

            val outFile = File(tempDir.toFile(), resource)
            outFile.parentFile.mkdirs()

            stream.openStream().use { input ->
                outFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        return tempFile
    }
}