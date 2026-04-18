plugins {
    id("com.gradleup.shadow")
}

dependencies {
    // 🌟 API Base
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly(project(":NexoCore")) // Puente directo a tu núcleo
    compileOnly(project(":NexoClans"))

    // 🌟 Ecosistema de Plugins (Universales para toda tu red)
    compileOnly("com.nexomc:nexo:1.20.1") { exclude(group = "net.kyori") }
    compileOnly("dev.aurelium:auraskills-api-bukkit:2.3.9") { exclude(group = "net.kyori") }
    compileOnly("dev.triumphteam:triumph-gui:3.1.11") { exclude(group = "net.kyori") }
    compileOnly("com.willfp:eco:6.77.5") { exclude(group = "net.kyori") }
    compileOnly("me.clip:placeholderapi:2.11.6")
}

tasks.shadowJar {
    // 🌟 FIX CRÍTICO: Sincronización de memoria con NexoCore
    relocate("com.google.inject", "me.nexo.core.libs.inject")
    relocate("javax.inject", "me.nexo.core.libs.javax.inject")
    relocate("revxrsal.commands", "me.nexo.core.libs.commands")
    relocate("org.spongepowered.configurate", "me.nexo.core.libs.configurate")

    archiveClassifier.set("")
}