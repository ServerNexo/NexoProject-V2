plugins {
    java
    // 🌟 Motor de empaquetado moderno para Java 21+
    id("com.gradleup.shadow") version "9.4.1"
}

group = "me.nexo"
version = "1.0-SNAPSHOT"
description = "Motor Nativo de Esclavos Evolutivos para Paper 1.21.5"

java {
    toolchain {
        // 🚀 PILAR: Soporte estricto para Java 21
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") // PaperMC
    maven("https://repo.nexomc.com/releases") // Nexo (Custom Items)
    maven("https://jitpack.io") // Lamp (Comandos)

    // 🌟 REPOSITORIOS DE CODEMC (Para EvenMoreFish)
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.codemc.io/repository/EvenMoreFish/")

    // 🌟 EL SALVAVIDAS: Repositorio de Modrinth (Nunca se cae)
    maven("https://api.modrinth.com/maven")
}

dependencies {
    // ⚙️ API de PaperMC (1.21.5 Nativo)
    compileOnly("io.papermc.paper:paper-api:1.21.5-R0.1-SNAPSHOT")

    // ==========================================
    // 🔗 DEPENDENCIAS INTERNAS DEL ECOSISTEMA NEXO
    // ==========================================
    compileOnly(project(":AeroxisCore"))
    compileOnly(project(":AeroxisProtections"))
    compileOnly(project(":AeroxisColecciones"))
    compileOnly(project(":AeroxisIslas"))
    compileOnly(project(":AeroxisFactories"))

    // El compilador necesita saber qué es Guice
    compileOnly("com.google.inject:guice:7.0.0")

    // 🌟 DEPENDENCIAS DE EVEN MORE FISH (API Oficial)
    compileOnly("com.oheers.evenmorefish:even-more-fish-api:2.2.3")

    // ==========================================
    // 🚀 LIBRERÍAS EXTERNAS
    // ==========================================
    compileOnly("com.nexomc:nexo:1.20.1")

    // 🌟 FIX DEFINITIVO: Descargamos AuraSkills directo desde el espejo de Modrinth
    compileOnly("maven.modrinth:auraskills:2.2.6")

    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")

    compileOnly("com.github.revxrsal.Lamp:common:3.2.1")
    compileOnly("com.github.revxrsal.Lamp:bukkit:3.2.1")

    compileOnly("org.spongepowered:configurate-yaml:4.1.2")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
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
        archiveClassifier.set("")

        dependencies {
            exclude(dependency("com.google.inject:guice:.*"))
            exclude(dependency("com.github.revxrsal.Lamp:common:.*"))
            exclude(dependency("com.github.revxrsal.Lamp:bukkit:.*"))
            exclude(dependency("org.spongepowered:configurate-yaml:.*"))
        }

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