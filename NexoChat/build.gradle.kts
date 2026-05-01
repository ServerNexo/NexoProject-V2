description = "Motor AAA de chat inmersivo y cosméticos."

dependencies {
    // 🔗 DEPENDENCIAS INTERNAS
    compileOnly(project(":NexoCore"))

    // 🌟 FRAMEWORKS (El compilador los necesita, el Core los provee)
    compileOnly("com.github.revxrsal.Lamp:common:3.2.1")
    compileOnly("com.github.revxrsal.Lamp:bukkit:3.2.1")

    // 🧩 APIs EXTERNAS
    compileOnly("me.clip:placeholderapi:2.11.5")
    compileOnly("org.geysermc.floodgate:api:2.2.2-SNAPSHOT")
}

// ==========================================
// ⚙️ TAREAS DE COMPILACIÓN LOCALES
// ==========================================

// 🌟 FIX VISUAL PARA INTELLIJ: Usamos withType<ProcessResources> para evitar el falso positivo
tasks.withType<ProcessResources> {
    val props = mapOf("version" to project.version.toString())
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    mergeServiceFiles()
}

// 🌟 FIX CRÍTICO: Capturamos la ruta en la fase de configuración (Fuera de la ejecución)
val directorioDestino = rootProject.file("compilados")

tasks.build {
    dependsOn(tasks.shadowJar)
    doLast {
        copy {
            from(tasks.shadowJar.get().archiveFile)
            into(directorioDestino)
        }
    }
}