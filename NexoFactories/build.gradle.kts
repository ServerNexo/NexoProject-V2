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

    // Dependencias Externas
    compileOnly("com.github.ben-manes.caffeine:caffeine:3.1.8")
    compileOnly("dev.aurelium:auraskills-api:2.2.6")
    compileOnly("com.nexomc:nexo:1.20.1")
    // 🌟 Añadido el motor de configuración Configurate (YAML)
    compileOnly("org.spongepowered:configurate-yaml:4.1.2")
}

tasks {
    shadowJar {
        // Solo definimos el nombre. La ruta de salida (tu servidor)
        // y el clasificador ya los maneja el build.gradle.kts raíz.
        archiveBaseName.set("NexoFactories")
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