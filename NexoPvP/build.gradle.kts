plugins {
    java
    // 🌟 Motor de empaquetado moderno para Java 21+
    id("com.gradleup.shadow") version "9.4.1"
}

group = "me.nexo"
version = "1.0-SNAPSHOT"
description = "NexoPvP"

java {
    toolchain {
        // 🚀 PILAR: Soporte estricto para Java 21 (Virtual Threads)
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") // PaperMC
    maven("https://jitpack.io") // JitPack (Lamp y otros)
}

dependencies {
    // ⚙️ API de PaperMC (1.21.5 Nativo)
    compileOnly("io.papermc.paper:paper-api:1.21.5-R0.1-SNAPSHOT")

    // ==========================================
    // 🔗 DEPENDENCIAS INTERNAS DEL ECOSISTEMA NEXO
    // ==========================================
    compileOnly(project(":NexoCore"))
    compileOnly(project(":NexoProtections"))
    compileOnly(project(":NexoWar"))
    compileOnly(project(":NexoEconomy"))

    // ==========================================
    // 🛡️ DEPENDENCIAS EXTERNAS PROVISTAS POR EL SERVIDOR
    // ==========================================
    compileOnly("dev.aurelium:auraskills-api-bukkit:2.3.9") {
        exclude(group = "net.kyori") // Previene choques con Adventure nativo
    }

    // ==========================================
    // 🚀 LIBRERÍAS EMPAQUETADAS (ShadowJar)
    // ==========================================
    implementation("com.google.inject:guice:7.0.0")
    implementation("com.github.Revxrsal.Lamp:common:3.1.9")
    implementation("com.github.Revxrsal.Lamp:bukkit:3.1.9")
    implementation("org.spongepowered:configurate-yaml:4.1.2")
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

        // Limpieza de metadatos para evitar alertas de firmas rotas
        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
        exclude("META-INF/LICENSE*")
        exclude("META-INF/NOTICE*")
        exclude("module-info.class")

        // 🛡️ Relocación táctica para NexoPvP
        relocate("com.google.inject", "me.nexo.pvp.libs.inject")
        relocate("javax.inject", "me.nexo.pvp.libs.javax.inject")
        relocate("revxrsal.commands", "me.nexo.pvp.libs.commands")
        relocate("org.spongepowered.configurate", "me.nexo.pvp.libs.configurate")
    }

    build {
        dependsOn(shadowJar)
    }
}