pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "NexoProject-V2"

// Encendemos todo el imperio de golpe
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
include("NexoClans")
include("NexoColecciones")
include("NexoDungeons")
include("NexoEconomy")
include("NexoFactories")
include("NexoMechanics")
include("NexoMinions")
include("NexoProtections")
include("NexoPvP")
include("NexoWar")
include("NexoCore")