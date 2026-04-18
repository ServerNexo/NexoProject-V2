package me.nexo.pvp.pvp;

import com.google.inject.Inject;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.bukkit.annotation.CommandPermission; // 💡 Importación del permiso

/**
 * 🏛️ NexoPvP - Comando PvP (Arquitectura Enterprise)
 * Cero CommandExecutor, Cero Chequeos de Consola.
 */
public class ComandoPvP {

    // 💉 PILAR 3: Inyección de Dependencias Limpia
    private final PvPManager manager;

    @Inject
    public ComandoPvP(PvPManager manager) {
        this.manager = manager;
    }

    // 💡 PILAR 1: Framework Lamp con Permisos Nativos
    @Command("pvp")
    @CommandPermission("nexopvp.user") // 🔒 Permiso asignado
    public void togglePvP(Player player) {
        manager.togglePvP(player);
    }
}