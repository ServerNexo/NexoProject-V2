package me.aeroxis.core.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.core.cataclysms.CataclysmManager;
import me.aeroxis.core.hub.HubDonationGUI;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

/**
 * ⌨️ AeroxisCore - Comandos de la Fase 2 (Eventos y Reconstrucción)
 */
@Singleton
@Command({"aeroxis", "eventos"})
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
    // Comando a usar en FancyNPCs: /fancynpcs action add <npc> playercommand eventos donar
    @Subcommand("donar")
    public void abrirMenuDonaciones(Player player) {
        // Abrimos el menú del proyecto "herreria_t2"
        donationGUI.openMenu(player, "herreria_t2");
    }

    // ==========================================
    // ☄️ CONTROL DE CATACLISMOS (Solo Administradores)
    // ==========================================
    // Comando: /aeroxis cataclismo o /eventos cataclismo
    @Subcommand("cataclismo")
    @CommandPermission("aeroxis.admin")
    public void forzarMeteorito(Player player) {
        if (cataclysmManager.isMeteorActive()) {
            player.sendMessage("§c❌ Ya hay un meteorito activo en el Spawn.");
            return;
        }

        cataclysmManager.triggerRandomMeteorite();
        player.sendMessage("§a✅ ¡Meteorito invocado con éxito en uno de los nodos!");
    }
}