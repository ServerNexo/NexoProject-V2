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
        // 🚀 PILAR: Soporte estricto para Java 21 (Virtual Threads y Text Blocks)
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") // PaperMC
    maven("https://repo.nexomc.com/releases") // Nexo (Custom Items)
    maven("https://jitpack.io") // 🌟 IMPRESCINDIBLE PARA LAMP Y AURASKILLS
}

dependencies {
    // ⚙️ API de PaperMC (1.21.5 Nativo)
    compileOnly("io.papermc.paper:paper-api:1.21.5-R0.1-SNAPSHOT")

    // ==========================================
    // 🔗 DEPENDENCIAS INTERNAS DEL ECOSISTEMA NEXO
    // ==========================================
    compileOnly(project(":NexoCore"))
    compileOnly(project(":NexoProtections"))
    compileOnly(project(":NexoColecciones"))

    // 🌟 FIX CRÍTICO 1: El compilador necesita saber qué es Guice (El Core lo proveerá en el servidor)
    compileOnly("com.google.inject:guice:7.0.0")

    // ==========================================
    // 🚀 LIBRERÍAS EXTERNAS (CompileOnly)
    // ==========================================
    compileOnly("com.nexomc:nexo:1.20.1")
    compileOnly("dev.aurelium:auraskills-api:2.2.6")

    // Lombok requiere Annotation Processor explícito en Gradle
    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")

    // 🌟 FIX CRÍTICO 2: Actualizamos Lamp a 3.2.1 (minúsculas) para mantener coherencia
    compileOnly("com.github.revxrsal.Lamp:common:3.2.1")
    compileOnly("com.github.revxrsal.Lamp:bukkit:3.2.1")

    // Motor de configuración Configurate (YAML)
    compileOnly("org.spongepowered:configurate-yaml:4.1.2")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        // 🌟 CLAVE PARA GUICE: Lectura de constructores en los Managers Inyectables
        options.compilerArgs.add("-parameters")
    }

    processResources {
        filteringCharset = "UTF-8"
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        // 🌟 FIX CRÍTICO: Ahora Gradle buscará y procesará plugin.yml
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    shadowJar {
        archiveClassifier.set("")

        // 💥 EXTERMINADOR DE LINKAGE ERROR:
        // Excluimos físicamente estas librerías para forzar que use las de NexoCore.
        dependencies {
            exclude(dependency("com.google.inject:guice:.*"))
            exclude(dependency("com.github.revxrsal.Lamp:common:.*"))
            exclude(dependency("com.github.revxrsal.Lamp:bukkit:.*"))
            exclude(dependency("org.spongepowered:configurate-yaml:.*"))
        }

        // Limpieza de metadatos para evitar alertas de firmas rotas
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