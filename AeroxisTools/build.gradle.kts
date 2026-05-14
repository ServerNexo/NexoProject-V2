plugins {
    java
    id("com.gradleup.shadow") version "9.4.1"
}

group = "me.nexo"
version = "1.0-SNAPSHOT"
description = "NexoTools - Utilities & Essentials Module"

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
    // API de PaperMC
    compileOnly("io.papermc.paper:paper-api:1.21.5-R0.1-SNAPSHOT")

    // Dependencia del Core
    compileOnly(project(":AeroxisCore"))

    // Librerías provistas por el Core en tiempo de ejecución
    compileOnly("com.google.inject:guice:7.0.0")
    compileOnly("com.github.revxrsal.Lamp:common:3.2.1")
    compileOnly("com.github.revxrsal.Lamp:bukkit:3.2.1")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-parameters", "-Xlint:all,-processing"))
    }

    shadowJar {
        archiveClassifier.set("")
        dependencies {
            exclude(dependency("com.google.inject:guice:.*"))
            exclude(dependency("com.github.revxrsal.Lamp:.*"))
        }
    }

    register<Copy>("copyJarToPlugins") {
        dependsOn(shadowJar)
        from(layout.buildDirectory.dir("libs"))
        into("C:/Users/faust/Desktop/NexoV2/plugins")
        include("*.jar")
        doNotTrackState("Evitar choque con archivos bloqueados")
    }

    build {
        dependsOn(shadowJar)
        finalizedBy("copyJarToPlugins")
    }
}