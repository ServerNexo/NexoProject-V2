package me.nexo.items.guardarropa;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.DefaultFor; // 🌟 EL IMPORT CORRECTO PARA MÉTODOS
import revxrsal.commands.annotation.Description;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

/**
 * 🎒 NexoItems - Comando Principal del Guardarropa (Arquitectura Enterprise Java 21)
 * Rendimiento: Lamp Command Framework, Cero Dependencias Muertas e Inyección Estricta.
 */
@Singleton
@Command({"wardrobe", "armario"}) // 🌟 LAMP: Define el comando principal y sus alias al instante
@CommandPermission("nexoitems.user")
@Description("Abre el menú de Guardarropa RPG.")
public class ComandoWardrobe {

    // 🌟 DEPENDENCIAS PROPAGADAS
    private final GuardarropaListener listener;
    private final CrossplayUtils crossplayUtils;

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public ComandoWardrobe(GuardarropaListener listener, CrossplayUtils crossplayUtils) {
        this.listener = listener;
        this.crossplayUtils = crossplayUtils;
    }

    // 1. Abrir Menú (Se ejecuta por defecto al poner /wardrobe sin argumentos)
    @DefaultFor({"~"}) // 🌟 FIX CRÍTICO: Indica que este es el método raíz del comando
    public void openWardrobe(Player player) {
        // 🌟 LAMP: Ya verificó por nosotros que el Sender es un Player. Cero casteos manuales.
        listener.abrirMenu(player);
    }

    // 2. Ayuda (/wardrobe help)
    @Subcommand("help")
    @Description("Muestra la guía del guardarropa.")
    public void helpWardrobe(Player player) {
        // 🌟 USO DE DEPENDENCIA INYECTADA (Cero estáticos)
        crossplayUtils.sendMessage(player, "&#555555--------------------------------");
        crossplayUtils.sendMessage(player, "&#ff00ff👔 <bold>SISTEMA DE GUARDARROPA</bold>");
        crossplayUtils.sendMessage(player, "&#00f5ff/wardrobe &#E6CCFF- Abre tu armario de armaduras.");
        crossplayUtils.sendMessage(player, "&#555555--------------------------------");
    }
}