package me.nexo.pvp.pvp;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.bukkit.annotation.CommandPermission; 

/**
 * 🏛️ NexoPvP - Comando PvP (Arquitectura Enterprise)
 * Cero CommandExecutor, Cero Chequeos de Consola. 100% Inyectado.
 */
@Singleton // 🌟 FIX CRÍTICO: Garantiza una única instancia en la memoria de Guice
public class ComandoPvP {

    // 💉 PILAR 1: Inyección de Dependencias Limpia
    private final PvPManager manager;

    @Inject
    public ComandoPvP(PvPManager manager) {
        this.manager = manager;
    }

    // 💡 PILAR 2: Framework Lamp con Permisos Nativos
    @Command("pvp")
    @CommandPermission("nexopvp.user") // 🔒 Permiso asignado
    public void togglePvP(Player player) {
        manager.togglePvP(player);
    }
}