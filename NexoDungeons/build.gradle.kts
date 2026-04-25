plugins {
    java
    // 🌟 Motor de empaquetado moderno para Java 21+
    id("com.gradleup.shadow") version "9.4.1"
}

group = "me.nexo"
version = "1.0-SNAPSHOT"
description = "Motor de Instancias y Mazmorras (WorldEdit + MythicMobs)"

java {
    toolchain {
        // 🚀 PILAR: Soporte estricto para Java 21
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") // PaperMC
    maven("https://maven.enginehub.org/repo/") // WorldEdit
    maven("https://mvn.lumine.io/repository/maven-public/") // MythicMobs
    maven("https://jitpack.io") // 🌟 Añadido para mantener coherencia con Lamp
}

dependencies {
    // ⚙️ API de PaperMC (1.21.5 Nativo)
    compileOnly("io.papermc.paper:paper-api:1.21.5-R0.1-SNAPSHOT")

    // ==========================================
    // 🔗 DEPENDENCIAS INTERNAS DEL ECOSISTEMA
    // ==========================================
    compileOnly(project(":NexoCore"))
    compileOnly(project(":NexoEconomy"))

    // ==========================================
    // ⚔️ DEPENDENCIAS EXTERNAS (APIs de Terceros)
    // ==========================================
    // 🌟 FIX CRÍTICO: El compilador necesita saber qué es Guice
    compileOnly("com.google.inject:guice:7.0.0")

    // 🌟 Inyectamos el framework de comandos (Lamp) para consistencia de la arquitectura
    compileOnly("com.github.revxrsal.Lamp:common:3.2.1")
    compileOnly("com.github.revxrsal.Lamp:bukkit:3.2.1")

    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.3.0")
    compileOnly("io.lumine:Mythic-Dist:5.7.2")

    // Motor de configuración Configurate (YAML)
    compileOnly("org.spongepowered:configurate-yaml:4.1.2")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        // 🌟 CLAVE PARA GUICE: Permite la inyección directa en constructores
        options.compilerArgs.add("-parameters")
    }

    processResources {
        filteringCharset = "UTF-8"
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        // 🌟 FIX CRÍTICO: Le indicamos que procese el archivo clásico
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