plugins {
    // 🌟 FIX: Actualizado al nuevo nombre
    id("com.gradleup.shadow")
}

dependencies {
    // 🌟 API Base (Privada para el core, los demás ya la tienen en su plantilla)
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")

    // 🌟 LIBRERÍAS A EMPAQUETAR Y COMPARTIR (El puente api que hicimos antes)
    api("com.github.Revxrsal.Lamp:common:3.1.9")
    api("com.github.Revxrsal.Lamp:bukkit:3.1.9")
    api("com.google.inject:guice:7.0.0")
    api("org.spongepowered:configurate-yaml:4.1.2")

    // 🌟 FIX: LIBRERÍAS EXTERNAS COMPARTIDAS (Cambiado de compileOnly a compileOnlyApi)
    compileOnlyApi("org.geysermc.floodgate:api:2.2.3-SNAPSHOT")
    compileOnlyApi("me.clip:placeholderapi:2.11.6")
    // 🌟 FIX: Usamos 'api' en lugar de 'compileOnlyApi' para forzar a ShadowJar a empaquetarlas
    api("org.postgresql:postgresql:42.7.2")
    api("com.zaxxer:HikariCP:5.1.0")
    api("com.github.ben-manes.caffeine:caffeine:3.1.8") // ¡Aquí está la solución de tus imports!

    // 🌟 FIX: Ecosistema de Plugins compartido sin Kyori
    compileOnlyApi("com.nexomc:nexo:1.20.1") { exclude(group = "net.kyori") }
    compileOnlyApi("dev.triumphteam:triumph-gui:3.1.11") { exclude(group = "net.kyori") }
    compileOnlyApi("com.willfp:eco:6.77.5") { exclude(group = "net.kyori") }
    compileOnlyApi("dev.aurelium:auraskills-api-bukkit:2.3.9") { exclude(group = "net.kyori") }
}

tasks.shadowJar {
    relocate("com.google.inject", "me.nexo.core.libs.inject")
    relocate("javax.inject", "me.nexo.core.libs.javax.inject")
    relocate("revxrsal.commands", "me.nexo.core.libs.commands")
    relocate("org.spongepowered.configurate", "me.nexo.core.libs.configurate")

    archiveClassifier.set("")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}