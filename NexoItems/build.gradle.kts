plugins {
    id("com.gradleup.shadow")
}

dependencies {
    // 🌟 API de Paper
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")

    // 🌟 Conexión interna: Le decimos que dependa de nuestro propio NexoCore
    compileOnly(project(":NexoCore"))
    compileOnly(project(":NexoItems"))

    // 🌟 APIs de terceros (con la exclusión de Kyori por si acaso)
    compileOnly("dev.aurelium:auraskills-api-bukkit:2.3.9") { exclude(group = "net.kyori") }
    compileOnly("com.nexomc:nexo:1.20.1") { exclude(group = "net.kyori") }

    // 🌟 Librerías a empaquetar (implementation)
    implementation("org.apache.commons:commons-lang3:3.14.0")
}

tasks.shadowJar {
    // 🌟 FIX CRÍTICO: Sincronización de memoria con NexoCore
    relocate("com.google.inject", "me.nexo.core.libs.inject")
    relocate("javax.inject", "me.nexo.core.libs.javax.inject")
    relocate("revxrsal.commands", "me.nexo.core.libs.commands")
    relocate("org.spongepowered.configurate", "me.nexo.core.libs.configurate")

    archiveClassifier.set("")
}