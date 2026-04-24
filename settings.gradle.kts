pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "NexoProject-V2"

// 🌟 INCLUSIÓN LIMPIA: Cada módulo se declara una sola vez
include(
    "NexoCore",
    "NexoItems",
    "NexoPvP",
    "NexoProtections",
    "NexoMinions",
    "NexoMechanics",
    "NexoFactories",
    "NexoEconomy",
    "NexoDungeons",
    "NexoColecciones",
    "NexoClans",
    "NexoWar"
)