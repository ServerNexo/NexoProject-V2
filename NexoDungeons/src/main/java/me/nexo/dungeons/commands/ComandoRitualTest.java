package me.nexo.dungeons.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.dungeons.NexoDungeons;
import me.nexo.dungeons.modes.SummonDungeon;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.util.List;
import java.util.UUID;

/**
 * 🧪 Simulador de Sandbox para probar SummonDungeon en un mundo normal.
 */
@Singleton
@Command("ritualtest")
@CommandPermission("nexodungeons.admin")
public final class ComandoRitualTest implements Listener { // 🌟 FIX: Añadimos 'final' para silenciar el [this-escape]

    private final SummonDungeon ritualDungeon;
    private boolean pruebaActiva = false;

    @Inject
    public ComandoRitualTest(NexoDungeons plugin, SummonDungeon ritualDungeon) {
        this.ritualDungeon = ritualDungeon;
        // 🌟 Registramos este comando como Listener temporal para atrapar los eventos
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Subcommand("iniciar")
    public void iniciarPrueba(Player player) {
        if (pruebaActiva) {
            player.sendMessage("§c[!] El simulador ya está corriendo.");
            return;
        }

        Location loc = player.getLocation();

        // 1. Colocamos el bloque de Magnetita a los pies del jugador
        loc.getBlock().setType(Material.LODESTONE);

        // 2. Engañamos a la clase dándole el mundo normal y la Location actual
        ritualDungeon.setup(UUID.randomUUID(), player.getWorld(), List.of(player), loc);
        ritualDungeon.initialize();
        ritualDungeon.start();

        pruebaActiva = true;
        player.sendMessage("§a[!] Simulador iniciado. ¡El altar está frente a ti!");
    }

    @Subcommand("detener")
    public void detenerPrueba(Player player) {
        if (!pruebaActiva) return;

        ritualDungeon.destroyInstance();
        pruebaActiva = false;
        player.sendMessage("§e[!] Simulador apagado. (Quita el bloque de magnetita manualmente).");
    }

    // ==========================================
    // 🔀 ENRUTADOR DE EVENTOS (Puente Temporal)
    // ==========================================

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (pruebaActiva) {
            ritualDungeon.handleInteract(event);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (pruebaActiva) {
            ritualDungeon.handleMobDeath(event);
        }
    }
}