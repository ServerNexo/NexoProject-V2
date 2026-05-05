plugins {
    java
    // 🌟 Motor de empaquetado moderno para Java 21+
    id("com.gradleup.shadow") version "9.4.1"
}

group = "me.nexo"
version = "1.0-SNAPSHOT"
description = "Motor de Skyblock MMO (Arquitectura ASP RAM Nativa)"

java {
    toolchain {
        // 🚀 Soporte estricto para Java 21+ (Virtual Threads ready)
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") // PaperMC
    maven("https://jitpack.io") // Lamp

    // 🌟 REPOSITORIO OFICIAL DE ADVANCED SLIME PAPER
    maven("https://repo.infernalsuite.com/repository/maven-snapshots/")

    // 🌟 REPOSITORIO PARA AURASKILLS (CodeMC)
    maven("https://repo.codemc.io/repository/maven-public/")
}

dependencies {
    // ⚙️ API de PaperMC (1.21.5 Nativo)
    compileOnly("io.papermc.paper:paper-api:1.21.5-R0.1-SNAPSHOT")

    // ==========================================
    // 🔗 DEPENDENCIAS INTERNAS DEL ECOSISTEMA
    // ==========================================
    compileOnly(project(":NexoCore")) // 🌟 De aquí sacamos el NexoPasterService
    compileOnly(project(":NexoEconomy")) // Para cobrar expansiones y mejoras de isla

    // ==========================================
    // ☕ DEPENDENCIAS EXTERNAS
    // ==========================================
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    // 🌟 Guice para la inyección de dependencias
    compileOnly("com.google.inject:guice:7.0.0")

    // 🌟 Lamp para los comandos
    compileOnly("com.github.revxrsal.Lamp:common:3.2.1")
    compileOnly("com.github.revxrsal.Lamp:bukkit:3.2.1")

    // Caché de ultra-alto rendimiento (Caffeine)
    compileOnly("com.github.ben-manes.caffeine:caffeine:3.1.8")

    // Configurate para YAML
    compileOnly("org.spongepowered:configurate-yaml:4.1.2")

    // 🌟 API NATIVA DE ADVANCED SLIME PAPER V4
    compileOnly("com.infernalsuite.asp:api:4.0.0-SNAPSHOT")

    // 🌟 AÑADIMOS EL LOADER Y LO IMPLEMENTAMOS (Para que shadowJar lo empaquete)
    implementation("com.infernalsuite.asp:file-loader:4.0.0-SNAPSHOT")

    // 🌟 API DE AURASKILLS (Para multiplicar XP en las islas)
    compileOnly("dev.aurelium:auraskills-api-bukkit:2.3.9") {
        exclude(group = "net.kyori") // Previene choques con Adventure nativo
    }
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
        // 🌟 FIX: Linter de deprecación y casteo activados para depuración extrema
        options.compilerArgs.add("-Xlint:deprecation")
        options.compilerArgs.add("-Xlint:unchecked")
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