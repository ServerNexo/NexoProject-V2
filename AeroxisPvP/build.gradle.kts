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
    maven("https://repo.nexomc.com/releases") // NexoMC
}

dependencies {
    // ⚙️ API de PaperMC (1.21.5 Nativo)
    compileOnly("io.papermc.paper:paper-api:1.21.5-R0.1-SNAPSHOT")

    // ==========================================
    // 🔗 DEPENDENCIAS INTERNAS DEL ECOSISTEMA Aeroxis
    // ==========================================
    compileOnly(project(":AeroxisCore"))
    compileOnly(project(":AeroxisProtections"))
    compileOnly(project(":AeroxisWar"))
    compileOnly(project(":AeroxisEconomy"))

    // ==========================================
    // 🛡️ DEPENDENCIAS EXTERNAS PROVISTAS POR EL SERVIDOR
    // ==========================================
    compileOnly("dev.aurelium:auraskills-api-bukkit:2.3.9") {
        exclude(group = "net.kyori") // Previene choques con Adventure nativo
    }

    // 🌟 AÑADIDO: API de Nexo (Ajusta la versión a la que tengas instalada en tu server si es necesario)
    compileOnly("com.nexomc:nexo:1.20.1")

    // ==========================================
    // 🚀 LIBRERÍAS EXTERNAS (CompileOnly - Provistas por AeroxisCore)
    // ==========================================
    // El Core ya empaqueta esto, aquí solo lo necesitamos para compilar.
    compileOnly("com.google.inject:guice:7.0.0")
    compileOnly("com.github.revxrsal.Lamp:common:3.2.1")
    compileOnly("com.github.revxrsal.Lamp:bukkit:3.2.1")
    compileOnly("org.spongepowered:configurate-yaml:4.1.2")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        // 🌟 CLAVE PARA GUICE: Lectura de constructores
        options.compilerArgs.add("-parameters")

        // 🌟 FIX: Mostrar todos los warnings excepto el de procesador de anotaciones
        options.compilerArgs.add("-Xlint:all,-processing")
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
        // Excluimos físicamente estas librerías para que ShadowJar NO las meta en el JAR.
        // Esto garantiza que NexoPvP use el Guice y Lamp que ya están en memoria por el Core.
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

    // ==========================================
    // 🚀 TAREAS PERSONALIZADAS DE GRADLE
    // ==========================================
    register<Copy>("copyJarToPlugins") {
        // Depende de que ShadowJar termine de crear el artefacto final
        dependsOn(shadowJar)

        from(layout.buildDirectory.dir("libs"))
        into("C:/Users/faust/Desktop/NexoV2/plugins")
        include("*.jar")

        // 🌟 FIX: Dile a Gradle que NO escanee la carpeta de destino
        // Esto evita que crashee si LuckPerms u otro plugin tiene archivos bloqueados.
        doNotTrackState("Copiar hacia un servidor en vivo con archivos bloqueados")
    }

    build {
        dependsOn(shadowJar)
        finalizedBy("copyJarToPlugins")
    }
}