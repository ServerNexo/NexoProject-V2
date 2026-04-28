package me.nexo.dungeons.waves;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.dungeons.NexoDungeons;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🏰 NexoDungeons - Instancia de Arena de Oleadas (Arquitectura Enterprise Java 21)
 * Rendimiento: RegionSchedulers, Zero-Garbage Sets, Radares Nativos de Jugadores Paper.
 * 100% Independiente de MythicMobs.
 */
public class WaveArena {

    private final NexoDungeons plugin;
    private final String arenaId;
    private final Location spawnCenter;
    private final CrossplayUtils crossplayUtils; // 🌟 Sinergia Propagada
    private int currentWave;

    // 🌟 FIX CONCURRENCIA: Set Concurrente Lock-Free para evitar C.M.E.
    private final Set<UUID> activeArenaMobs; // Renombrado de activeMythicMobs
    private boolean isActive;

    // 🌟 DEPENDENCIAS PROPAGADAS: CrossplayUtils ahora entra por constructor
    public WaveArena(NexoDungeons plugin, String arenaId, Location spawnCenter, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.arenaId = arenaId;
        this.spawnCenter = spawnCenter;
        this.crossplayUtils = crossplayUtils;
        this.currentWave = 0;
        this.activeArenaMobs = ConcurrentHashMap.newKeySet();
        this.isActive = false;
    }

    public void start() {
        this.isActive = true;
        this.currentWave = 0;
        nextWave();
    }

    public void nextWave() {
        if (!isActive) return;
        this.currentWave++;

        // Punto de Control cada 5 oleadas
        if (this.currentWave > 1 && (this.currentWave - 1) % 5 == 0) {
            int checkpoint = this.currentWave - 1;

            // 🌟 PAPER 1.21 FIX: getNearbyPlayers es O(1) de CPU
            spawnCenter.getNearbyPlayers(30).forEach(p -> {
                crossplayUtils.sendMessage(p, "&#FFAA00[!] <bold>PUNTO DE CONTROL:</bold> &#E6CCFFHas sobrevivido hasta la oleada &#55FF55" + checkpoint + "&#E6CCFF.");
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            });
        }

        // Anuncio de nueva oleada
        spawnCenter.getNearbyPlayers(30).forEach(p -> {
            crossplayUtils.sendTitle(p,
                    "&#FF5555<bold>OLEADA " + currentWave + "</bold>",
                    "&#E6CCFFDefiende la zona..."
            );
            p.playSound(p.getLocation(), Sound.EVENT_RAID_HORN, 1.0f, 1.0f);
        });

        // 🌟 FOLIA FIX: Usamos RegionScheduler anclado al centro de la arena
        Bukkit.getRegionScheduler().runDelayed(plugin, spawnCenter, task -> spawnMobs(), 60L);
    }

    private void spawnMobs() {
        if (!isActive) return; // Doble check de seguridad

        int mobsToSpawn = 3 + (currentWave * 2);
        boolean isBossWave = currentWave % 5 == 0;

        for (int i = 0; i < mobsToSpawn; i++) {
            double offsetX = (Math.random() - 0.5) * 10;
            double offsetZ = (Math.random() - 0.5) * 10;
            var spawnLoc = spawnCenter.clone().add(offsetX, 0, offsetZ);

            LivingEntity spawnedEntity;

            // 🌟 SPAWNEO NATIVO DE PAPER (Cero dependencias)
            if (isBossWave) {
                spawnedEntity = (LivingEntity) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.WITHER_SKELETON);
                spawnedEntity.setCustomName("§4§lNexo Boss Minion");
                if (spawnedEntity.getEquipment() != null) {
                    spawnedEntity.getEquipment().setItemInMainHand(new ItemStack(Material.NETHERITE_SWORD));
                }
            } else {
                spawnedEntity = (LivingEntity) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.ZOMBIE);
                spawnedEntity.setCustomName("§c§lNexo Guerrero");
                if (spawnedEntity.getEquipment() != null) {
                    spawnedEntity.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
                }
            }

            spawnedEntity.setCustomNameVisible(true);
            this.activeArenaMobs.add(spawnedEntity.getUniqueId());
            escalarAtributos(spawnedEntity); // 💪 Hacemos a los mobs más fuertes
        }
    }

    private void escalarAtributos(LivingEntity mob) {
        // Incrementa las stats un 20% por cada oleada
        double multiplier = Math.pow(1.2, Math.max(0, currentWave - 1));

        // 🌟 PAPER 1.21.4 FIX: Eliminado el prefijo GENERIC_ para atributos
        var healthAttr = mob.getAttribute(Attribute.MAX_HEALTH);
        if (healthAttr != null) {
            double newHealth = healthAttr.getBaseValue() * multiplier;
            healthAttr.setBaseValue(newHealth);
            mob.setHealth(newHealth); // Lo curamos a su nueva vida máxima
        }

        var damageAttr = mob.getAttribute(Attribute.ATTACK_DAMAGE);
        if (damageAttr != null) {
            double newDamage = damageAttr.getBaseValue() * multiplier;
            damageAttr.setBaseValue(newDamage);
        }
    }

    public void registrarMuerteMob(UUID mobId) {
        if (!isActive) return;

        if (activeArenaMobs.remove(mobId)) {
            if (activeArenaMobs.isEmpty()) {
                // 🌟 FOLIA FIX: RegionScheduler para avanzar a la siguiente oleada
                Bukkit.getRegionScheduler().runDelayed(plugin, spawnCenter, task -> nextWave(), 40L);
            }
        }
    }

    public void stop() {
        this.isActive = false;
        this.activeArenaMobs.clear();
    }

    public String getArenaId() { return arenaId; }
    public boolean isActive() { return isActive; }
    public int getCurrentWave() { return currentWave; }
}