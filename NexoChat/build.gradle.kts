plugins {
    java
    id("com.gradleup.shadow") version "9.4.1" // O la versión que uses en tus otros módulos
}

group = "me.nexo"
version = "1.0-SNAPSHOT"
description = "Motor AAA de chat inmersivo y cosméticos."

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.5-R0.1-SNAPSHOT")

    // Dependencia principal
    compileOnly(project(":NexoCore"))

    // 🌟 AQUI ESTÁ LA SOLUCIÓN AL ERROR (LAMP + GUICE)
    compileOnly("com.github.revxrsal.Lamp:common:3.2.1")
    compileOnly("com.github.revxrsal.Lamp:bukkit:3.2.1")
    compileOnly("com.google.inject:guice:7.0.0")

    // APIs Externas
    compileOnly("me.clip:placeholderapi:2.11.5")
    compileOnly("org.geysermc.floodgate:api:2.2.2-SNAPSHOT")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-parameters", "-Xlint:deprecation"))
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    mergeServiceFiles()
}

// 🌟 FIX CRÍTICO: Capturamos la ruta en la fase de configuración (Fuera de la ejecución)
val directorioDestino = rootProject.file("compilados")

tasks.build {
    dependsOn(tasks.shadowJar)
    doLast {
        copy {
            from(tasks.shadowJar.get().archiveFile)
            into(directorioDestino) // Usamos la variable capturada limpiamente
        }
    }
}