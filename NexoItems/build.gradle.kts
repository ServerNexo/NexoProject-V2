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
}

dependencies {
    // ⚙️ API de PaperMC (1.21.5 Nativo)
    compileOnly("io.papermc.paper:paper-api:1.21.5-R0.1-SNAPSHOT")

    // ==========================================
    // 🔗 DEPENDENCIAS INTERNAS DEL ECOSISTEMA
    // ==========================================
    compileOnly(project(":NexoCore"))

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
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }

    shadowJar {
        archiveClassifier.set("")

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