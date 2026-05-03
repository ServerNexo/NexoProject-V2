package me.nexo.core.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.cataclysms.CataclysmManager;
import me.nexo.core.hub.HubDonationGUI;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

/**
 * ⌨️ NexoCore - Comandos de la Fase 2 (Eventos y Reconstrucción)
 */
@Singleton
@Command({"nexo", "eventos"}) // 🌟 FIX: Cambiado de "hub" a "eventos" para evitar conflicto con Bungee/Velocity
public class ComandoEventos {

    private final HubDonationGUI donationGUI;
    private final CataclysmManager cataclysmManager;

    @Inject
    public ComandoEventos(HubDonationGUI donationGUI, CataclysmManager cataclysmManager) {
        this.donationGUI = donationGUI;
        this.cataclysmManager = cataclysmManager;
    }

    // ==========================================
    // 🏛️ MENÚ DE DONACIONES (Comando de Jugador / NPC)
    // ==========================================
    // Comando a usar en FancyNPCs: /fancynpcs action add <npc> playercommand nexo donar
    @Subcommand("donar")
    public void abrirMenuDonaciones(Player player) {
        // Abrimos el menú del proyecto "herreria_t2"
        donationGUI.openMenu(player, "herreria_t2");
    }

    // ==========================================
    // ☄️ CONTROL DE CATACLISMOS (Solo Administradores)
    // ==========================================
    // Comando: /nexo cataclismo o /eventos cataclismo
    @Subcommand("cataclismo")
    @CommandPermission("nexo.admin")
    public void forzarMeteorito(Player player) {
        if (cataclysmManager.isMeteorActive()) {
            player.sendMessage("§c❌ Ya hay un meteorito activo en el Spawn.");
            return;
        }
        
        cataclysmManager.triggerRandomMeteorite();
        player.sendMessage("§a✅ ¡Meteorito invocado con éxito en uno de los nodos!");
    }
}