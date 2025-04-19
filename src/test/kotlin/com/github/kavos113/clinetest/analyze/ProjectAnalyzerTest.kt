package com.github.kavos113.clinetest.analyze

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.writeText

class ProjectAnalyzerTest {

  private lateinit var tempDir: Path

  @BeforeEach
  fun setUp() {
    tempDir = Files.createTempDirectory("project-analyzer-test")
  }

  @AfterEach
  fun tearDown() {
    tempDir.toFile().deleteRecursively()
  }

  @Test
  fun testGetAllProjectFilesBasicStructure() {
    val file1 = tempDir.resolve("file1.txt").createFile()
    val file2 = tempDir.resolve("file2.txt").createFile()
    val subDir = tempDir.resolve("subdir").createDirectories()
    val file3 = subDir.resolve("file3.txt").createFile()

    val files = ProjectAnalyzer.getAllProjectFiles(tempDir)

    assertEquals(3, files.size)
    assertTrue(files.contains(file1.toFile()))
    assertTrue(files.contains(file2.toFile()))
    assertTrue(files.contains(file3.toFile()))
  }

  @Test
  fun testGetAllProjectFilesWithGitignore() {
    val file1 = tempDir.resolve("file1.txt").createFile()
    val file2 = tempDir.resolve("file2.txt").createFile()
    val ignoredFile = tempDir.resolve("ignored.txt").createFile()

    val gitignore = tempDir.resolve(".gitignore").createFile()
    gitignore.writeText("ignored.txt")

    val files = ProjectAnalyzer.getAllProjectFiles(tempDir)

    assertEquals(3, files.size) // Including .gitignore
    assertTrue(files.contains(file1.toFile()))
    assertTrue(files.contains(file2.toFile()))
    assertTrue(files.contains(gitignore.toFile()))
    assertFalse(files.contains(ignoredFile.toFile()))
  }

  @Test
  fun testGetAllProjectFilesWithNestedGitignore() {
    val file1 = tempDir.resolve("file1.txt").createFile()
    val subDir = tempDir.resolve("subdir").createDirectories()
    val file2 = subDir.resolve("file2.txt").createFile()
    val ignoredFile = subDir.resolve("ignored.txt").createFile()

    val gitignore = subDir.resolve(".gitignore").createFile()
    gitignore.writeText("ignored.txt")

    val files = ProjectAnalyzer.getAllProjectFiles(tempDir)

    assertEquals(3, files.size) // Including .gitignore
    assertTrue(files.contains(file1.toFile()))
    assertTrue(files.contains(file2.toFile()))
    assertTrue(files.contains(gitignore.toFile()))
    assertFalse(files.contains(ignoredFile.toFile()))
  }

  @Test
  fun testGetAllProjectFilesWithPredefinedIgnorePatterns() {
    val file1 = tempDir.resolve("file1.txt").createFile()

    val nodeModules = tempDir.resolve("node_modules").createDirectories()
    val nodeModulesFile = nodeModules.resolve("package.json").createFile()

    val buildDir = tempDir.resolve("build").createDirectories()
    val buildDepsDir = buildDir.resolve("dependencies").createDirectories()
    val buildDepsFile = buildDepsDir.resolve("dep.jar").createFile()

    val hiddenDir = tempDir.resolve(".hidden").createDirectories()
    val hiddenFile = hiddenDir.resolve("hidden.txt").createFile()

    val files = ProjectAnalyzer.getAllProjectFiles(tempDir)

    assertEquals(1, files.size)
    assertTrue(files.contains(file1.toFile()))
    assertFalse(files.contains(nodeModulesFile.toFile()))
    assertFalse(files.contains(buildDepsFile.toFile()))
    assertFalse(files.contains(hiddenFile.toFile()))
  }

  @Test
  fun testGetAllProjectFilesWithWildcardIgnorePatterns() {
    val file1 = tempDir.resolve("file1.txt").createFile()
    val file2 = tempDir.resolve("file2.txt").createFile()

    val ignoredFile = tempDir.resolve("ignored_file.txt").createFile()
    val ignoredDir = tempDir.resolve("ignored_dir").createDirectories()
    val ignoredFileInDir = ignoredDir.resolve("ignored_file_in_dir.txt").createFile()

    val gitignore = tempDir.resolve(".gitignore").createFile()
    gitignore.writeText("ignored*")

    val files = ProjectAnalyzer.getAllProjectFiles(tempDir)

    assertEquals(3, files.size) // Including .gitignore
    assertTrue(files.contains(file1.toFile()))
    assertTrue(files.contains(file2.toFile()))
    assertTrue(files.contains(gitignore.toFile()))
    assertFalse(files.contains(ignoredFile.toFile()))
    assertFalse(files.contains(ignoredFileInDir.toFile()))
  }

  @Test
  fun testAnalyzeProjectWithJavaFile() {
    // Create a simple Java file with a class and method
    val javaDir = tempDir.resolve("src/main/java/com/example").createDirectories()
    val javaFile = javaDir.resolve("Example.java").createFile()
    javaFile.writeText(
      """
            package com.example;

            public class Example {
                public void exampleMethod() {
                    // Method body
                }
            }
        """.trimIndent()
    )

    // Analyze the project
    val result = ProjectAnalyzer.analyzeProject(tempDir)

    // Verify the result contains the expected content
    assertTrue(result.contains("# Source code definitions:"))
    assertTrue(result.contains("src/main/java/com/example/Example.java"))
    assertTrue(result.contains("|public class Example {"))
    assertTrue(result.contains("public void exampleMethod() {"))
  }

  @Test
  fun testAnalyzeProjectWithMultipleLanguages() {
    // Create a Java file
    val javaDir = tempDir.resolve("src/main/java/com/example").createDirectories()
    val javaFile = javaDir.resolve("Example.java").createFile()
    javaFile.writeText(
      """
            package com.example;

            public class Example {
                public void exampleMethod() {
                    // Method body
                }
            }
        """.trimIndent()
    )

    // Create a Python file
    val pythonDir = tempDir.resolve("src/main/python").createDirectories()
    val pythonFile = pythonDir.resolve("example.py").createFile()
    pythonFile.writeText(
      """
            class PythonExample:
                def example_method(self):
                    # Method body
                    pass
        """.trimIndent()
    )

    // Create a JavaScript file
    val jsDir = tempDir.resolve("src/main/js").createDirectories()
    val jsFile = jsDir.resolve("example.js").createFile()
    jsFile.writeText(
      """
            class JsExample {
                constructor() {
                    // Constructor
                }

                exampleMethod() {
                    // Method body
                }
            }
        """.trimIndent()
    )

    // Analyze the project
    val result = ProjectAnalyzer.analyzeProject(tempDir)

    // Verify the result contains the expected content for all languages
    assertTrue(result.contains("# Source code definitions:"))

    // Java
    assertTrue(result.contains("src/main/java/com/example/Example.java"))
    assertTrue(result.contains("|public class Example {"))
    assertTrue(result.contains("public void exampleMethod() {"))

    // Python
    assertTrue(result.contains("src/main/python/example.py"))
    assertTrue(result.contains("|class PythonExample:"))
    assertTrue(result.contains("def example_method(self):"))

    // JavaScript
    assertTrue(result.contains("src/main/js/example.js"))
    assertTrue(result.contains("|class JsExample {"))
    assertTrue(result.contains("exampleMethod() {"))
  }

  @Test
  fun testAnalyzeProjectWithIgnoredFiles() {
    // Create a Java file
    val javaDir = tempDir.resolve("src/main/java/com/example").createDirectories()
    val javaFile = javaDir.resolve("Example.java").createFile()
    javaFile.writeText(
      """
            package com.example;

            public class Example {
                public void exampleMethod() {
                    // Method body
                }
            }
        """.trimIndent()
    )

    // Create files that should be ignored
    val nodeModules = tempDir.resolve("node_modules").createDirectories()
    val nodeModulesFile = nodeModules.resolve("package.json").createFile()
    nodeModulesFile.writeText(
      """
            {
              "name": "example",
              "version": "1.0.0"
            }
        """.trimIndent()
    )

    val txtFile = tempDir.resolve("readme.txt").createFile()
    txtFile.writeText("This is a readme file")

    // Analyze the project
    val result = ProjectAnalyzer.analyzeProject(tempDir)

    // Verify the result contains the expected content
    assertTrue(result.contains("# Source code definitions:"))
    assertTrue(result.contains("src/main/java/com/example/Example.java"))

    // Verify ignored files are listed in unparsed files
    assertTrue(result.contains("# Unparsed files:"))
    assertTrue(result.contains("readme.txt"))

    // Verify node_modules files are not included at all
    assertFalse(result.contains("node_modules/package.json"))
  }

  @Test
  fun testAnalyzeProjectWithEmptyFiles() {
    // Create an empty Java file
    val javaDir = tempDir.resolve("src/main/java/com/example").createDirectories()
    val emptyJavaFile = javaDir.resolve("Empty.java").createFile()
    emptyJavaFile.writeText("")

    // Create a Java file with no definitions (just comments)
    val commentsJavaFile = javaDir.resolve("Comments.java").createFile()
    commentsJavaFile.writeText(
      """
            // This is a comment
            /* This is a multi-line comment
               with no actual code
            */
        """.trimIndent()
    )

    // Create a valid Java file for comparison
    val validJavaFile = javaDir.resolve("Valid.java").createFile()
    validJavaFile.writeText(
      """
            package com.example;

            public class Valid {
                public void validMethod() {
                    // Method body
                }
            }
        """.trimIndent()
    )

    // Analyze the project
    val result = ProjectAnalyzer.analyzeProject(tempDir)

    // Verify the result contains the expected content
    assertTrue(result.contains("# Source code definitions:"))
    assertTrue(result.contains("src/main/java/com/example/Valid.java"))
    assertTrue(result.contains("|public class Valid {"))

    // Verify empty files are listed in unparsed files
    assertTrue(result.contains("# Unparsed files:"))
    assertTrue(result.contains("src/main/java/com/example/Empty.java"))
    assertTrue(result.contains("src/main/java/com/example/Comments.java"))
  }

  @Test
  fun testAnalyzeProjectWithInterfacesAndInheritance() {
    val javaDir = tempDir.resolve("src/main/java/com/example").createDirectories()

    // Create an interface
    val interfaceFile = javaDir.resolve("ExampleInterface.java").createFile()
    interfaceFile.writeText(
      """
            package com.example;

            public interface ExampleInterface {
                void interfaceMethod();
            }
        """.trimIndent()
    )

    // Create a class that implements the interface
    val implementingClassFile = javaDir.resolve("ImplementingClass.java").createFile()
    implementingClassFile.writeText(
      """
            package com.example;

            public class ImplementingClass implements ExampleInterface {
                @Override
                public void interfaceMethod() {
                    // Implementation
                }

                public void additionalMethod() {
                    // Additional method
                }
            }
        """.trimIndent()
    )

    // Create a base class
    val baseClassFile = javaDir.resolve("BaseClass.java").createFile()
    baseClassFile.writeText(
      """
            package com.example;

            public class BaseClass {
                public void baseMethod() {
                    // Base method
                }
            }
        """.trimIndent()
    )

    // Create a class that extends the base class
    val childClassFile = javaDir.resolve("ChildClass.java").createFile()
    childClassFile.writeText(
      """
            package com.example;

            public class ChildClass extends BaseClass {
                @Override
                public void baseMethod() {
                    // Overridden method
                }

                public void childMethod() {
                    // Child method
                }
            }
        """.trimIndent()
    )

    // Analyze the project
    val result = ProjectAnalyzer.analyzeProject(tempDir)

    // Verify the result contains the expected content
    assertTrue(result.contains("# Source code definitions:"))

    // Interface
    assertTrue(result.contains("src/main/java/com/example/ExampleInterface.java"))
    assertTrue(result.contains("|public interface ExampleInterface {"))
    assertTrue(result.contains("void interfaceMethod();"))

    // Implementing class
    assertTrue(result.contains("src/main/java/com/example/ImplementingClass.java"))
    assertTrue(result.contains("|public class ImplementingClass implements ExampleInterface {"))
    assertTrue(result.contains("public void interfaceMethod() {"))
    assertTrue(result.contains("public void additionalMethod() {"))

    // Base class
    assertTrue(result.contains("src/main/java/com/example/BaseClass.java"))
    assertTrue(result.contains("|public class BaseClass {"))
    assertTrue(result.contains("public void baseMethod() {"))

    // Child class
    assertTrue(result.contains("src/main/java/com/example/ChildClass.java"))
    assertTrue(result.contains("|public class ChildClass extends BaseClass {"))
    assertTrue(result.contains("public void baseMethod() {"))
    assertTrue(result.contains("public void childMethod() {"))
  }

  @Test
  fun testAnalyzeProjectWithMixedFileTypes() {
    // Create supported file types
    val javaDir = tempDir.resolve("src/main/java").createDirectories()
    val javaFile = javaDir.resolve("Example.java").createFile()
    javaFile.writeText(
      """
            public class Example {
                public void method() {}
            }
        """.trimIndent()
    )

    val pythonDir = tempDir.resolve("src/main/python").createDirectories()
    val pythonFile = pythonDir.resolve("example.py").createFile()
    pythonFile.writeText(
      """
            class Example:
                def method(self):
                    pass
        """.trimIndent()
    )

    // Create unsupported file types
    val docsDir = tempDir.resolve("docs").createDirectories()
    val markdownFile = docsDir.resolve("README.md").createFile()
    markdownFile.writeText(
      """
            # Project Documentation
            This is a sample project.
        """.trimIndent()
    )

    val configFile = tempDir.resolve("config.yml").createFile()
    configFile.writeText(
      """
            version: 1.0
            name: test-project
        """.trimIndent()
    )

    val htmlFile = docsDir.resolve("index.html").createFile()
    htmlFile.writeText(
      """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Test Project</title>
            </head>
            <body>
                <h1>Test Project</h1>
            </body>
            </html>
        """.trimIndent()
    )

    // Analyze the project
    val result = ProjectAnalyzer.analyzeProject(tempDir)

    // Verify the result contains the expected content
    assertTrue(result.contains("# Source code definitions:"))

    // Supported files should be parsed
    assertTrue(result.contains("src/main/java/Example.java"))
    assertTrue(result.contains("|public class Example {"))

    assertTrue(result.contains("src/main/python/example.py"))
    assertTrue(result.contains("|class Example:"))

    // Unsupported files should be in the unparsed section
    assertTrue(result.contains("# Unparsed files:"))
    assertTrue(result.contains("docs/README.md"))
    assertTrue(result.contains("config.yml"))
    assertTrue(result.contains("docs/index.html"))

    // Make sure the content of unsupported files is not parsed
    assertFalse(result.contains("|# Project Documentation"))
    assertFalse(result.contains("|version: 1.0"))
    assertFalse(result.contains("|<!DOCTYPE html>"))
  }

  @Test
  fun testAnalyzeProjectWithNoParseableFiles() {
    // Create only unsupported file types
    val docsDir = tempDir.resolve("docs").createDirectories()
    val markdownFile = docsDir.resolve("README.md").createFile()
    markdownFile.writeText("# Project Documentation")

    val configFile = tempDir.resolve("config.yml").createFile()
    configFile.writeText("version: 1.0")

    val txtFile = tempDir.resolve("notes.txt").createFile()
    txtFile.writeText("Some notes")

    // Analyze the project
    val result = ProjectAnalyzer.analyzeProject(tempDir)

    // Verify the result does not contain source code definitions section
    assertFalse(result.contains("# Source code definitions:"))

    // Verify all files are in the unparsed section
    assertTrue(result.contains("# Unparsed files:"))
    assertTrue(result.contains("docs/README.md"))
    assertTrue(result.contains("config.yml"))
    assertTrue(result.contains("notes.txt"))
  }

  @Test
  fun testAnalyzeProjectWithEmptyDirectory() {
    // Don't create any files

    // Analyze the project
    val result = ProjectAnalyzer.analyzeProject(tempDir)

    // Verify the result is empty or contains only headers
    assertTrue(result.contains("# Unparsed files:"))
    assertFalse(result.contains("# Source code definitions:"))

    // The result should be minimal, just the header for unparsed files
    assertEquals("# Unparsed files:\n\n", result)
  }
}
