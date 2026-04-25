plugins {
    java
    // 🌟 Motor de empaquetado moderno para Java 21+
    id("com.gradleup.shadow") version "9.4.1"
}

group = "me.nexo"
version = "1.0-SNAPSHOT"
description = "Motor de Ítems Personalizados y Base de Encantamientos"

java {
    toolchain {
        // 🚀 PILAR: Soporte estricto para Java 21 (Virtual Threads ready)
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") // PaperMC Nativo
    maven("https://repo.nexomc.com/releases") // Repositorio de NexoMC

    // 🌟 FIX CRÍTICO 1: Aquí es donde vive la librería de comandos (Lamp)
    maven("https://jitpack.io")
}

dependencies {
    // ⚙️ API de PaperMC (1.21.5 Nativo)
    compileOnly("io.papermc.paper:paper-api:1.21.5-R0.1-SNAPSHOT")

    // ==========================================
    // 🔗 DEPENDENCIAS INTERNAS DEL ECOSISTEMA
    // ==========================================
    compileOnly(project(":NexoCore"))

    // 🌟 FIX CRÍTICO: El compilador necesita saber qué es Guice (El Core lo proveerá en el servidor)
    compileOnly("com.google.inject:guice:7.0.0")

    // 🌟 FIX CRÍTICO 2: Añadimos explícitamente el módulo 'common' que contiene el @Command
    compileOnly("com.github.revxrsal.Lamp:common:3.2.1")
    // 🌟 FIX CRÍTICO 3: Coordenadas oficiales correctas para JitPack (Lamp)
    compileOnly("com.github.revxrsal.Lamp:bukkit:3.2.1")

    // 🌟 FIX CRÍTICO 4: Añadido el motor de configuración Configurate (YAML)
    compileOnly("org.spongepowered:configurate-yaml:4.1.2")

    // ==========================================
    // ⚔️ DEPENDENCIAS EXTERNAS
    // ==========================================
    compileOnly("dev.aurelium:auraskills-api-bukkit:2.3.9")
    compileOnly("com.nexomc:nexo:1.20.1")

    // Paper ya incluye commons-lang3, usamos compileOnly para no inflar el JAR
    compileOnly("org.apache.commons:commons-lang3:3.14.0")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        // 🌟 CLAVE PARA GUICE: Permite la inyección directa leyendo los nombres de variables
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
        // Expulsa físicamente estas librerías para forzar que use las del Core.
        dependencies {
            exclude(dependency("com.google.inject:guice:.*"))
            exclude(dependency("com.github.revxrsal.Lamp:common:.*"))
            exclude(dependency("com.github.revxrsal.Lamp:bukkit:.*"))
            exclude(dependency("org.spongepowered:configurate-yaml:.*"))
        }

        // Limpieza de metadatos para evitar alertas de firmas rotas en el servidor
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