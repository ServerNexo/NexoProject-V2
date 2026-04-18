package me.nexo.dungeons.bosses;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import io.lumine.mythic.bukkit.events.MythicMobSpawnEvent;
import me.nexo.dungeons.NexoDungeons;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.DoubleAdder;

/**
 * 🏰 NexoDungeons - Gestor de Combates contra Jefes Globales (Arquitectura Enterprise Java 25)
 * Rendimiento: DoubleAdder Atómico, Pattern Matching y Virtual Threads seguros.
 */
@Singleton
public class BossFightManager implements Listener {

    private final NexoDungeons plugin;
    private final LootDistributor lootDistributor;

    // 🌟 OPTIMIZACIÓN RAM: Usamos DoubleAdder en lugar de Double para sumas atómicas lock-free
    private final Map<UUID, Map<UUID, DoubleAdder>> activeBosses = new ConcurrentHashMap<>();

    private final Set<String> trackedBossTypes = Set.of("NexoDragon", "ReyEsqueleto", "TitanDeMagma");

    @Inject
    public BossFightManager(NexoDungeons plugin, LootDistributor lootDistributor) {
        this.plugin = plugin;
        this.lootDistributor = lootDistributor;
    }

    // 🟢 1. Detectar cuando un Boss nace
    @EventHandler(priority = EventPriority.NORMAL)
    public void onMythicSpawn(MythicMobSpawnEvent event) {
        if (trackedBossTypes.contains(event.getMobType().getInternalName())) {
            activeBosses.put(event.getEntity().getUniqueId(), new ConcurrentHashMap<>());
            plugin.getLogger().info("🐉 [BOSS FIGHT] Iniciando rastreo de daño para el Titán: " + event.getMobType().getInternalName());
        }
    }

    // ⚔️ 2. Rastrear cada golpe de forma ultra-rápida
    // 🌟 FIX SEGURIDAD: Prioridad MONITOR (Se ejecuta al final de todos) para asegurar que el daño no fue cancelado por otro plugin
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBossDamage(EntityDamageByEntityEvent event) {
        UUID entityId = event.getEntity().getUniqueId();

        // Verificación O(1): Si no es un boss rastreado, ignoramos al instante
        Map<UUID, DoubleAdder> damageMap = activeBosses.get(entityId);
        if (damageMap == null) return;

        // 🌟 PATTERN MATCHING JAVA 21+: Código más limpio y rápido (Cero casteos pesados)
        Player atacante = switch (event.getDamager()) {
            case Player p -> p;
            case Projectile proj when proj.getShooter() instanceof Player p -> p;
            default -> null;
        };

        if (atacante != null) {
            // 🌟 FIX CONCURRENCIA: Suma Atómica de altísimo rendimiento (Wall-Street style)
            damageMap.computeIfAbsent(atacante.getUniqueId(), k -> new DoubleAdder()).add(event.getFinalDamage());
        }
    }

    // 💀 3. El Boss muere: Hora de calcular el botín
    @EventHandler(priority = EventPriority.NORMAL)
    public void onMythicDeath(MythicMobDeathEvent event) {
        UUID entityId = event.getEntity().getUniqueId();

        Map<UUID, DoubleAdder> damageMapAdder = activeBosses.remove(entityId);

        if (damageMapAdder != null && !damageMapAdder.isEmpty()) {

            // 🌟 FIX SEGURIDAD ASÍNCRONA: Extraemos datos nativos en el Main Thread
            final String bossName = event.getMobType().getInternalName();
            final Location deathLoc = event.getEntity().getLocation().clone();

            // Convertimos el mapa de DoubleAdder a Double normal para dárselo al LootDistributor
            Map<UUID, Double> finalDamageMap = new ConcurrentHashMap<>();
            damageMapAdder.forEach((k, v) -> finalDamageMap.put(k, v.sum()));

            // 🚀 Java 25 Virtual Threads: Cálculo asíncrono puro en RAM
            Thread.startVirtualThread(() -> {
                lootDistributor.distributeLoot(bossName, finalDamageMap, deathLoc);
            });
        }
    }
}