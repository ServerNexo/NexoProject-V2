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
    implementation("com.github.Revxrsal.Lamp:common:3.1.9")
    implementation("com.github.Revxrsal.Lamp:bukkit:3.1.9")

    // Google Guice (Inyección de Dependencias)
    implementation("com.google.inject:guice:7.0.0")

    // Sponge Configurate (YAML Type-Safe)
    implementation("org.spongepowered:configurate-yaml:4.1.2")

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

    // 🗄️ Base de Datos: Hikari y Postgres
    // Nota del Arquitecto: En tu paper-plugin.yml, asegúrate de añadirlas a la lista "libraries:"
    compileOnly("org.postgresql:postgresql:42.7.2")
    compileOnly("com.zaxxer:HikariCP:5.1.0")
    compileOnly("com.github.ben-manes.caffeine:caffeine:3.1.8")
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
        filesMatching("paper-plugin.yml") {
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

        // 🛡️ Relocación: Aislamos nuestras dependencias empaquetadas para no chocar con otros plugins
        relocate("com.google.inject", "me.nexo.core.libs.inject")
        relocate("javax.inject", "me.nexo.core.libs.javax.inject")
        relocate("revxrsal.commands", "me.nexo.core.libs.commands")
        relocate("org.spongepowered.configurate", "me.nexo.core.libs.configurate")
    }

    build {
        dependsOn(shadowJar)
    }
}