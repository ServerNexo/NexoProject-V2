package me.nexo.mechanics.archeology;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.mechanics.NexoMechanics;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class ArcheologyCompassTracker {

    private final NexoMechanics plugin;
    private final ArcheologyManager archeologyManager;
    private final NamespacedKey nexoIdKey;

    // 🌟 Almacenamiento seguro para la BossBar de cada jugador
    private final Map<UUID, BossBar> activeBossBars = new ConcurrentHashMap<>();

    @Inject
    public ArcheologyCompassTracker(NexoMechanics plugin, ArcheologyManager archeologyManager) {
        this.plugin = plugin;
        this.archeologyManager = archeologyManager;
        this.nexoIdKey = NamespacedKey.fromString("nexo:id");
    }

    public void startTracking() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {

            // 🧹 Limpieza: Eliminamos la BossBar de la memoria si el jugador se desconecta
            activeBossBars.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);

            for (Player player : Bukkit.getOnlinePlayers()) {
                ItemStack handItem = player.getInventory().getItemInMainHand();

                // Si no tiene la brújula, le quitamos la BossBar y saltamos al siguiente
                if (handItem.getType() != Material.COMPASS || !handItem.hasItemMeta()) {
                    removeBossBar(player);
                    continue;
                }

                ItemMeta meta = handItem.getItemMeta();
                boolean isTracker = false;

                if (meta.getPersistentDataContainer().has(nexoIdKey, PersistentDataType.STRING)) {
                    String idNexo = meta.getPersistentDataContainer().get(nexoIdKey, PersistentDataType.STRING);
                    if ("brujula_arqueologo".equals(idNexo)) {
                        isTracker = true;
                    }
                }

                if (!isTracker) {
                    removeBossBar(player);
                    continue;
                }

                Location pLoc = player.getLocation();
                Location closestTreasure = null;
                double closestDistanceSq = Double.MAX_VALUE;
                int tesorosOcultos = archeologyManager.getHiddenTreasures().size();

                for (Location treasureLoc : archeologyManager.getHiddenTreasures()) {
                    if (!treasureLoc.getWorld().equals(pLoc.getWorld())) continue;

                    double distSq = treasureLoc.distanceSquared(pLoc);
                    if (distSq < closestDistanceSq && distSq <= 900) { // 30 bloques de alcance
                        closestTreasure = treasureLoc;
                        closestDistanceSq = distSq;
                    }
                }

                // 🌟 Obtenemos su BossBar o le creamos una nueva si acaba de sacar la brújula
                BossBar bar = activeBossBars.computeIfAbsent(player.getUniqueId(), k -> {
                    BossBar newBar = BossBar.bossBar(Component.empty(), 0f, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
                    player.showBossBar(newBar);
                    return newBar;
                });

                MiniMessage mm = MiniMessage.miniMessage();

                if (closestTreasure != null) {
                    double actualDistance = Math.sqrt(closestDistanceSq);
                    enviarFeedback(player, closestTreasure, actualDistance);

                    // 🌟 HUD: Calcula el progreso de la barra (Se llena conforme te acercas)
                    float progress = Math.max(0.0f, Math.min(1.0f, 1.0f - (float) (actualDistance / 30.0)));

                    bar.name(mm.deserialize("<#55FF55>✨ Tesoro oculto a " + String.format("%.1f", actualDistance) + "m"));
                    bar.progress(progress);
                    bar.color(BossBar.Color.GREEN);
                } else {
                    // 🌟 HUD: Escaneando zona vacía
                    bar.name(mm.deserialize("<#FFAA00>🧭 Buscando rastros... (" + tesorosOcultos + " ocultos)"));
                    bar.progress(1.0f);
                    bar.color(BossBar.Color.BLUE);
                }
            }
        }, 0L, 10L);
    }

    // Método de seguridad para ocultar y destruir la BossBar
    private void removeBossBar(Player player) {
        BossBar bar = activeBossBars.remove(player.getUniqueId());
        if (bar != null) {
            player.hideBossBar(bar);
        }
    }

    private void enviarFeedback(Player player, Location target, double distance) {
        Location centerOfBlock = target.clone().add(0.5, 1.2, 0.5);

        if (distance <= 5.0) {
            player.spawnParticle(Particle.FLAME, centerOfBlock, 3, 0.2, 0.2, 0.2, 0.01);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 2.0f);
        } else {
            player.spawnParticle(Particle.END_ROD, centerOfBlock, 1, 0.2, 0.2, 0.2, 0.01);
            if (Math.random() > 0.5) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.3f, 1.0f);
            }
        }
    }
}