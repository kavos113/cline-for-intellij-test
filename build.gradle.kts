plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.10"
    id("org.jetbrains.intellij.platform") version "2.3.0"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")

    implementation("com.anthropic:anthropic-java:0.8.0")
    implementation("com.anthropic:anthropic-java-bedrock:1.2.0")
    implementation("com.amazonaws:aws-java-sdk:1.12.782")
    implementation("com.openai:openai-java:1.4.1")

    implementation("io.github.java-diff-utils:java-diff-utils:4.15")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.3")
    implementation("com.vladsch.flexmark:flexmark-all:0.64.8")
    implementation("org.eclipse.jgit:org.eclipse.jgit:7.1.0.202411261347-r")

    implementation("io.github.bonede:tree-sitter:0.24.5")
    implementation("io.github.bonede:tree-sitter-typescript:0.21.1")
    implementation("io.github.bonede:tree-sitter-javascript:0.23.1")
    implementation("io.github.bonede:tree-sitter-python:0.23.4")
    implementation("io.github.bonede:tree-sitter-cpp:0.23.4")
    implementation("io.github.bonede:tree-sitter-swift:0.5.0")
    implementation("io.github.bonede:tree-sitter-go:0.23.3")
    implementation("io.github.bonede:tree-sitter-ruby:0.23.1")
    implementation("io.github.bonede:tree-sitter-rust:0.23.1")
    implementation("io.github.bonede:tree-sitter-c-sharp:0.23.1")
    implementation("io.github.bonede:tree-sitter-java:0.23.4")
    implementation("io.github.bonede:tree-sitter-php:0.23.11")
    implementation("io.github.bonede:tree-sitter-c:0.23.2")

    intellijPlatform {
        val type = providers.gradleProperty("platformType").get()
        val version = providers.gradleProperty("platformVersion").get()

        create(type, version)

        bundledPlugins(providers.gradleProperty("platformBundledPlugins").map { it.split(',') })
        plugins(providers.gradleProperty("platformPlugins").map { it.split(',') })
    }
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}

intellijPlatform {
    pluginConfiguration {
        version = providers.gradleProperty("pluginVersion").get()
        description = ""
    }

    signing {

    }

    publishing {

    }

    pluginVerification {

    }
}

intellijPlatformTesting {
    runIde {
        register("runIdeForUiTests") {
            task {
                jvmArgumentProviders += CommandLineArgumentProvider {
                    listOf(
                        "-Drobot-server.port=8082",
                        "-Dide.mac.message.dialogs.as.sheets=false",
                        "-Djb.privacy.policy.text=<!--999.999-->",
                        "-Djb.consents.confirmation.enabled=false",
                    )
                }
            }

            plugins {
                robotServerPlugin()
            }
        }
    }
}