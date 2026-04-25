plugins {
    java
    // 🌟 FIX CRÍTICO: Usamos el fork moderno oficial (GradleUp) que soporta Java 21 y Gradle 8+ nativamente
    id("com.gradleup.shadow") version "9.4.1"
}

group = "me.nexo"
version = "1.0-SNAPSHOT"
description = "NexoCore"

java {
    toolchain {
        // 🚀 PILAR: Soporte estricto para Java 21 (Virtual Threads)
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") // Repositorio oficial Paper
    maven("https://repo.nexomc.com/releases") // NexoMC
    maven("https://repo.opencollab.dev/main/") // OpenCollab
    maven("https://repo.auxilor.io/repository/maven-public/") // Auxilor (Eco)
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") // PlaceholderAPI
    maven("https://repo.triumphteam.dev/releases/") // TriumphTeam
    maven("https://jitpack.io") // Jitpack genérico
}

dependencies {
    // ⚙️ API de PaperMC (1.21.5 Nativo)
    compileOnly("io.papermc.paper:paper-api:1.21.5-R0.1-SNAPSHOT")

    // ==========================================
    // 🚀 LIBRERÍAS EMPAQUETADAS (ShadowJar)
    // ==========================================

    // Revxrsal Lamp (Framework de comandos)
    implementation("com.github.revxrsal.Lamp:common:3.2.1")
    implementation("com.github.revxrsal.Lamp:bukkit:3.2.1")

    // Google Guice (Inyección de Dependencias)
    implementation("com.google.inject:guice:7.0.0")

    // Sponge Configurate (YAML Type-Safe)
    implementation("org.spongepowered:configurate-yaml:4.1.2")

    // 🗄️ FIX CRÍTICO: Base de Datos inyectada por ShadowJar
    // Evita el colapso al cargar Guice antes que el ClassLoader de Paper
    implementation("org.postgresql:postgresql:42.7.2")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    // ==========================================
    // 🛡️ LIBRERÍAS PROVISTAS POR EL SERVIDOR (Zero-Bloat)
    // ==========================================

    compileOnly("org.geysermc.floodgate:api:2.2.3-SNAPSHOT")

    compileOnly("com.nexomc:nexo:1.20.1") {
        exclude(group = "net.kyori") // Evita chocar con Adventure API nativo
    }

    compileOnly("dev.triumphteam:triumph-gui:3.1.11") {
        exclude(group = "net.kyori")
    }

    compileOnly("com.willfp:eco:6.77.5") {
        exclude(group = "net.kyori")
    }

    compileOnly("me.clip:placeholderapi:2.11.6")

    compileOnly("dev.aurelium:auraskills-api-bukkit:2.3.9") {
        exclude(group = "net.kyori")
    }
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        // 🌟 CLAVE PARA GUICE: Permite que el inyector lea los nombres de los parámetros de los constructores
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

        // Limpieza de meta-archivos innecesarios para reducir el peso y evitar errores de firmas rotas
        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
        exclude("META-INF/LICENSE*")
        exclude("META-INF/NOTICE*")
        exclude("module-info.class")

        // 💥 ELIMINADA la relocación de Lamp. Ahora todo tu ecosistema lo podrá encontrar.
        // Solo escondemos Configurate.
        relocate("org.spongepowered.configurate", "me.nexo.core.libs.configurate")
    }

    build {
        dependsOn(shadowJar)
    }
}