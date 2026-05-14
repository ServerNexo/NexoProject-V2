pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "AeroxisProject-V2"

// 🌟 INCLUSIÓN LIMPIA: Cada módulo se declara una sola vez
include(
    "AeroxisCore",
    "AeroxisItems",
    "AeroxisPvP",
    "AeroxisProtections",
    "AeroxisMinions",
    "AeroxisMechanics",
    "AeroxisFactories",
    "AeroxisEconomy",
    "AeroxisDungeons",
    "AeroxisColecciones",
    "AeroxisClans",
    "AeroxisWar",
    "AeroxisChat",
    "AeroxisIslas",
    "AeroxisTools"

)



include("AeroxisCrates")
include("AeroxisCosmetics")