plugins {
    java
    // 🌟 Motor de empaquetado moderno para Java 21+
    id("com.gradleup.shadow") version "9.4.1"
}

group = "me.nexo"
version = "1.0-SNAPSHOT"
description = "NexoProtections"

java {
    toolchain {
        // 🚀 PILAR: Soporte estricto para Java 21 (Virtual Threads)
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io") // 🌟 IMPRESCINDIBLE
}

dependencies {
    // ⚙️ API de PaperMC (1.21.5 Nativo)
    compileOnly("io.papermc.paper:paper-api:1.21.5-R0.1-SNAPSHOT")

    // ==========================================
    // 🔗 DEPENDENCIAS INTERNAS DEL ECOSISTEMA NEXO
    // ==========================================
    compileOnly(project(":NexoCore"))
    compileOnly(project(":NexoClans"))
    compileOnly(project(":NexoItems"))

    // ==========================================
    // 🚀 LIBRERÍAS EXTERNAS
    // ==========================================
    // PaperMC ya provee Commons Lang 3 nativamente. 'compileOnly' evita engordar el JAR.
    compileOnly("org.apache.commons:commons-lang3:3.14.0")
    dependencies {
        // ... otras dependencias (NexoCore, Paper API, etc.)

        // 🌟 FIX CRÍTICO: Inyectamos el framework de comandos
        compileOnly("com.github.revxrsal.Lamp:common:3.2.1")
        compileOnly("com.github.revxrsal.Lamp:bukkit:3.2.1")

        // 🌟 No olvides Configurate si usas ConfigManager en este módulo
        compileOnly("org.spongepowered:configurate-yaml:4.1.2")
    }
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        // 🌟 CLAVE PARA GUICE: Lectura de constructores en los Managers Inyectables
        options.compilerArgs.add("-parameters")
    }

    processResources {
        filteringCharset = "UTF-8"
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }

    shadowJar {
        archiveClassifier.set("")

        // Limpieza de metadatos para evitar alertas de firmas rotas
        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
        exclude("META-INF/LICENSE*")
        exclude("META-INF/NOTICE*")
        exclude("module-info.class")
    }

    build {
        dependsOn(shadowJar)
    }
}