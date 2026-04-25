// 🎒 NexoFactories - Build Script (Arquitectura Enterprise Java 21)

description = "NexoFactories"

// 🌟 Solo declaramos las dependencias EXCLUSIVAS de este módulo
dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.5-R0.1-SNAPSHOT")
    compileOnly("com.google.inject:guice:7.0.0")

    // Dependencias Internas del ecosistema
    compileOnly(project(":NexoCore"))
    compileOnly(project(":NexoProtections"))
    compileOnly(project(":NexoItems"))

    // 🌟 Inyectamos el framework de comandos (Lamp) para consistencia
    compileOnly("com.github.revxrsal.Lamp:common:3.2.1")
    compileOnly("com.github.revxrsal.Lamp:bukkit:3.2.1")

    // Dependencias Externas
    compileOnly("com.github.ben-manes.caffeine:caffeine:3.1.8")
    compileOnly("dev.aurelium:auraskills-api:2.2.6")
    compileOnly("com.nexomc:nexo:1.20.1")

    // 🌟 Añadido el motor de configuración Configurate (YAML)
    compileOnly("org.spongepowered:configurate-yaml:4.1.2")
}

tasks {
    // 🌟 FIX CRÍTICO: Sin esto, Guice falla al inyectar dependencias en Java 21
    compileJava {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
    }

    shadowJar {
        // Solo definimos el nombre. La ruta de salida (tu servidor)
        // y el clasificador ya los maneja el build.gradle.kts raíz.
        archiveBaseName.set("NexoFactories")

        // 💥 EXTERMINADOR DE LINKAGE ERROR:
        // Expulsamos físicamente estas librerías para forzar que use las del Core.
        dependencies {
            exclude(dependency("com.google.inject:guice:.*"))
            exclude(dependency("com.github.revxrsal.Lamp:common:.*"))
            exclude(dependency("com.github.revxrsal.Lamp:bukkit:.*"))
            exclude(dependency("org.spongepowered:configurate-yaml:.*"))
        }
    }

    processResources {
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        // 🌟 FIX CRÍTICO: Le indicamos que procese el archivo clásico
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}