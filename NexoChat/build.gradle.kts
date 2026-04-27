description = "Motor AAA de chat inmersivo y cosméticos."

dependencies {
    // Dependencia principal de tu ecosistema
    compileOnly(project(":NexoCore"))

    compileOnly("me.clip:placeholderapi:2.11.5")

    // 🌟 Le damos acceso a Triumph-GUI para que pueda compilar el menú
    compileOnly("dev.triumphteam:triumph-gui:3.1.11") {
        exclude(group = "net.kyori") // Excluimos Kyori porque Paper ya lo trae nativo
    }
}