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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.stream.Collectors;

/**
 * 🏰 NexoDungeons - Gestor de Combates contra Jefes Globales (Arquitectura Enterprise)
 * Rendimiento: DoubleAdder Atómico, Pattern Matching y Virtual Thread Executor.
 */
@Singleton
public class BossFightManager implements Listener {

    private final NexoDungeons plugin;
    private final LootDistributor lootDistributor;

    // 🌟 JAVA 21: Motor de Hilos Virtuales para cálculos asíncronos y delegaciones
    private final ExecutorService calcExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // 🌟 OPTIMIZACIÓN RAM: Usamos DoubleAdder en lugar de Double para sumas atómicas lock-free
    private final Map<UUID, Map<UUID, DoubleAdder>> activeBosses = new ConcurrentHashMap<>();

    private final Set<String> trackedBossTypes = Set.of("NexoDragon", "ReyEsqueleto", "TitanDeMagma");

    // 💉 PILAR 1: Inyección de Dependencias Directa
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
    // 🌟 FIX SEGURIDAD: Prioridad MONITOR para asegurar que el daño no fue cancelado por otro plugin (ej. Protecciones)
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBossDamage(EntityDamageByEntityEvent event) {
        var entityId = event.getEntity().getUniqueId();

        // Verificación O(1): Si no es un boss rastreado, ignoramos al instante
        var damageMap = activeBosses.get(entityId);
        if (damageMap == null) return;

        // 🌟 PATTERN MATCHING JAVA 21+: Código más limpio y rápido (Cero casteos pesados)
        Player atacante = switch (event.getDamager()) {
            case Player p -> p;
            case Projectile proj when proj.getShooter() instanceof Player p -> p;
            default -> null;
        };

        if (atacante != null) {
            // 🌟 FIX CONCURRENCIA: Suma Atómica de altísimo rendimiento
            damageMap.computeIfAbsent(atacante.getUniqueId(), k -> new DoubleAdder()).add(event.getFinalDamage());
        }
    }

    // 💀 3. El Boss muere: Hora de calcular el botín
    @EventHandler(priority = EventPriority.NORMAL)
    public void onMythicDeath(MythicMobDeathEvent event) {
        var entityId = event.getEntity().getUniqueId();

        var damageMapAdder = activeBosses.remove(entityId);

        if (damageMapAdder != null && !damageMapAdder.isEmpty()) {

            // 🌟 FIX SEGURIDAD ASÍNCRONA: Extraemos datos nativos inmutables en el Main Thread
            final String bossName = event.getMobType().getInternalName();
            final Location deathLoc = event.getEntity().getLocation().clone();

            // 🚀 JAVA 21 VIRTUAL THREADS: Cálculo asíncrono puro gestionado por el Executor
            calcExecutor.submit(() -> {
                // Transformación fluida y pura de DoubleAdder a Double con Streams
                Map<UUID, Double> finalDamageMap = damageMapAdder.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().sum()));

                // Delegamos la repartición a nuestra clase previamente purificada
                lootDistributor.distributeLoot(bossName, finalDamageMap, deathLoc);
            });
        }
    }
}