plugins {
    java
    id("com.gradleup.shadow") version "9.4.1"
}

group = "me.aeroxis"
version = "1.0-SNAPSHOT"
description = "AeroxisCrates - AAA Gacha and Lootbox System"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
}

dependencies {
    // API de PaperMC (1.21.5 Nativo)
    compileOnly("io.papermc.paper:paper-api:1.21.5-R0.1-SNAPSHOT")

    // Dependencia principal del Core
    compileOnly(project(":AeroxisCore"))

    // Librerías inyectadas desde el Core
    compileOnly("com.google.inject:guice:7.0.0")
    compileOnly("com.github.revxrsal.Lamp:common:3.2.1")
    compileOnly("com.github.revxrsal.Lamp:bukkit:3.2.1")
    compileOnly("dev.triumphteam:triumph-gui:3.1.11") // Para el menú de Card Reveal

    // 🌟 FIX: Añadimos Configurate para poder leer los YAML
    compileOnly("org.spongepowered:configurate-yaml:4.1.2")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
        options.compilerArgs.add("-Xlint:all,-processing")
    }

    processResources {
        filteringCharset = "UTF-8"
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    shadowJar {
        // Fix Supremo: Dejamos el nombre -all.jar
        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
        exclude("module-info.class")
    }

    register<Copy>("copyJarToPlugins") {
        dependsOn(shadowJar)
        from(layout.buildDirectory.dir("libs"))
        into("C:/Users/faust/Desktop/NexoV2/plugins")
        include("*-all.jar")
        rename { it.replace("-all.jar", ".jar") }
        doNotTrackState("Copiar a entorno en vivo")
    }

    build {
        dependsOn(shadowJar)
        finalizedBy("copyJarToPlugins")
    }
}
