import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

val kotlin_version: String by project

plugins {
    kotlin("jvm") version "1.6.21" // sync with kotlin_version
    id("com.github.johnrengelman.shadow") version "6.1.0"
    id("org.openapi.generator") version "6.6.0"
}

group = "com.wallubot.addons.spigot"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/public/") }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
    testImplementation(kotlin("test-junit"))
    testImplementation("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT")
    compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT")
    implementation("org.bstats:bstats-bukkit:3.0.1")
    // For openapi-generator client
    implementation("com.squareup.okhttp3:okhttp:3.12.4")
    implementation("com.squareup.moshi:moshi-kotlin:1.14.0")
}

tasks {
    named<ShadowJar>("shadowJar") {
        archiveBaseName.set("wallubot-spigot")
        relocate("org.bstats", "com.wallubot.addons.spigot.bstats")
        mergeServiceFiles()
    }
    named<GenerateTask>("openApiGenerate") {
        download("https://api.wallubot.com/docs/wallu-api-spec.yaml", "$buildDir/wallu-api-spec.yaml")
        inputSpec.set("$buildDir/wallu-api-spec.yaml")
        outputDir.set("$buildDir/generated")
        library.set("jvm-okhttp3") // will need to update at some point
        generatorName.set("kotlin")
        packageName.set("com.wallubot.addons")
        auth.set("apiKey")
        additionalProperties.set(
            mapOf(
                "enumPropertyNaming" to "UPPERCASE"
            )
        )
        doLast {
            // Script to fix double quotes in json name = ""unicode"" etc.
            val generatedFilePath = "$buildDir/generated/src/main/kotlin/com/wallubot/addons/models/OnMessageRequestBodyConfiguration.kt"
            File(generatedFilePath).apply {
                if (exists()) {
                    val correctedContent = readText().replace(Regex("@Json\\(name = \"\"(.*?)\"\"\\)"), "@Json(name = \"$1\")")
                    writeText(correctedContent)
                }
            }
        }
    }
    compileKotlin {
        dependsOn(openApiGenerate)
    }
    build {
        dependsOn(shadowJar)
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
}

sourceSets {
    main {
        java {
            srcDirs("$buildDir/generated/src/main/kotlin")
        }
    }
}

fun download(url: String, path: String) {
    val destFile = File(path)
    destFile.parentFile.mkdirs()
    ant.invokeMethod("get", mapOf("src" to url, "dest" to destFile))
}
