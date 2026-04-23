plugins {
    java
    // 🌟 Usamos el mismo motor de ShadowJar (GradleUp) que configuramos en NexoCore
    id("com.gradleup.shadow") version "9.4.1"
}

group = "me.nexo"
version = "1.0-SNAPSHOT"
description = "NexoWar"

java {
    toolchain {
        // 🚀 PILAR: Soporte estricto para Java 21 (Virtual Threads)
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") // PaperMC
    maven("https://repo.opencollab.dev/main/") // Floodgate / Geyser
}

dependencies {
    // ⚙️ API de PaperMC (1.21.5 Nativo)
    compileOnly("io.papermc.paper:paper-api:1.21.5-R0.1-SNAPSHOT")

    // ==========================================
    // 🔗 DEPENDENCIAS INTERNAS DEL ECOSISTEMA NEXO
    // (Resolución directa por Multi-Project Build)
    // ==========================================
    compileOnly(project(":NexoCore"))
    compileOnly(project(":NexoClans"))
    compileOnly(project(":NexoEconomy"))
    compileOnly(project(":NexoProtections"))

    // ==========================================
    // 🛡️ DEPENDENCIAS EXTERNAS PROVISTAS POR EL SERVIDOR
    // ==========================================
    compileOnly("org.geysermc.floodgate:api:2.2.3-SNAPSHOT")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        // 🌟 CLAVE PARA GUICE: Lectura de constructores
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