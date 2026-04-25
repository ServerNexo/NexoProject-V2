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
    maven("https://jitpack.io") // 🌟 IMPRESCINDIBLE PARA LAMP
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
    // 🌟 FIX CRÍTICO 1: El compilador necesita saber qué es Guice
    compileOnly("com.google.inject:guice:7.0.0")

    // PaperMC ya provee Commons Lang 3 nativamente. 'compileOnly' evita engordar el JAR.
    compileOnly("org.apache.commons:commons-lang3:3.14.0")

    // 🌟 FIX CRÍTICO 2: Inyectamos el framework de comandos (Bloque aplanado, sin anidar)
    compileOnly("com.github.revxrsal.Lamp:common:3.2.1")
    compileOnly("com.github.revxrsal.Lamp:bukkit:3.2.1")

    // Motor de configuración Configurate (YAML)
    compileOnly("org.spongepowered:configurate-yaml:4.1.2")
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
        // 🌟 FIX CRÍTICO: Ahora Gradle buscará y procesará plugin.yml
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    shadowJar {
        archiveClassifier.set("")

        // 💥 EXTERMINADOR DE LINKAGE ERROR:
        // Excluimos físicamente estas librerías para forzar que use las de NexoCore.
        dependencies {
            exclude(dependency("com.google.inject:guice:.*"))
            exclude(dependency("com.github.revxrsal.Lamp:common:.*"))
            exclude(dependency("com.github.revxrsal.Lamp:bukkit:.*"))
            exclude(dependency("org.spongepowered:configurate-yaml:.*"))
        }

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