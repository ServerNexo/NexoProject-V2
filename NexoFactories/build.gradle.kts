// 🎒 NexoFactories - Build Script (Arquitectura Enterprise Java 21)
plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1" // Optimizado para ecosistemas masivos
}

group = "me.nexo"
version = "1.0-SNAPSHOT"
description = "NexoFactories"

java {
    // 🌟 Obligamos al compilador a usar Java 21
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.nexomc.com/releases")
    maven("https://repo.aurelium.dev/releases")
}

dependencies {
    // 🌟 PAPER & GUICE NATIVO
    compileOnly("io.papermc.paper:paper-api:1.21.5-R0.1-SNAPSHOT")
    compileOnly("com.google.inject:guice:7.0.0") // Paper ya trae Guice, compileOnly es perfecto

    // 🌟 DEPENDENCIAS INTERNAS (Ecosistema Nexo)
    compileOnly(project(":NexoCore"))
    compileOnly(project(":NexoProtections"))
    compileOnly(project(":NexoItems"))

    // 🌟 DEPENDENCIAS EXTERNAS
    // Paper ya incluye Caffeine, por lo que usar compileOnly evita duplicarlo en tu Jar
    compileOnly("com.github.ben-manes.caffeine:caffeine:3.1.8")
    compileOnly("dev.aurelium:auraskills-api:2.2.6")
    compileOnly("com.nexomc:nexo:1.20.1")
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(21) // Habilita Virtual Threads y Text Blocks
    }

    // Configuración para empaquetar el plugin limpio
    build {
        dependsOn(shadowJar)
    }

    shadowJar {
        archiveBaseName.set("NexoFactories")
        archiveClassifier.set("")
        archiveVersion.set(project.version.toString())

        // 💡 NOTA: Si en el futuro añades librerías 'implementation',
        // puedes usar 'relocate("paquete.viejo", "me.nexo.libs")' aquí.
    }

    processResources {
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }
}