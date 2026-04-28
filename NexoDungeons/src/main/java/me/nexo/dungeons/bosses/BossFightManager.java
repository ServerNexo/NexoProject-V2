package me.nexo.dungeons.bosses;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.dungeons.NexoDungeons;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent; // 🌟 Evento Nativo
import org.bukkit.persistence.PersistentDataType; // 🌟 Para leer el PDC

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.stream.Collectors;

/**
 * 🏰 NexoDungeons - Gestor de Combates contra Jefes Globales (Arquitectura Enterprise)
 * Purificado de MythicMobs. Usa PDC Nativo, DoubleAdder Atómico y Virtual Threads.
 */
@Singleton
public class BossFightManager implements Listener {

    private final NexoDungeons plugin;
    private final LootDistributor lootDistributor;

    // 🌟 Llave nativa del PDC para identificar a los Jefes creados por NexoCore
    private final NamespacedKey bossKey;

    // 🌟 JAVA 21: Motor de Hilos Virtuales para cálculos asíncronos y delegaciones
    private final ExecutorService calcExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // 🌟 OPTIMIZACIÓN RAM: Usamos DoubleAdder en lugar de Double para sumas atómicas lock-free
    private final Map<UUID, Map<UUID, DoubleAdder>> activeBosses = new ConcurrentHashMap<>();

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public BossFightManager(NexoDungeons plugin, LootDistributor lootDistributor) {
        this.plugin = plugin;
        this.lootDistributor = lootDistributor;
        // La misma llave que el NexoCore le pone al Mob cuando spawnea
        this.bossKey = new NamespacedKey("nexocore", "boss_id");
    }

    // ⚔️ 1. Rastrear cada golpe de forma ultra-rápida
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBossDamage(EntityDamageByEntityEvent event) {
        var entity = event.getEntity();
        var entityId = entity.getUniqueId();

        // Buscamos si ya lo estamos rastreando (O(1))
        var damageMap = activeBosses.get(entityId);

        // 🌟 Lazy Initialization: Si no está en RAM, leemos su PDC
        if (damageMap == null) {
            // Si tiene la etiqueta secreta de jefe, lo registramos
            if (entity.getPersistentDataContainer().has(bossKey, PersistentDataType.STRING)) {
                damageMap = new ConcurrentHashMap<>();
                activeBosses.put(entityId, damageMap);
            } else {
                return; // Es un mob normal, ignorar
            }
        }

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

    // 💀 2. El Boss muere: Hora de calcular el botín
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBossDeath(EntityDeathEvent event) {
        var entity = event.getEntity();
        var entityId = entity.getUniqueId();

        // 🌟 Verificamos si la entidad muerta era un Jefe nuestro
        if (entity.getPersistentDataContainer().has(bossKey, PersistentDataType.STRING)) {

            // Extraemos su nombre real ("NexoDragon", "ReyEsqueleto", etc.)
            final String bossName = entity.getPersistentDataContainer().get(bossKey, PersistentDataType.STRING);

            // Sacamos el mapa de daño de la RAM
            var damageMapAdder = activeBosses.remove(entityId);

            if (damageMapAdder != null && !damageMapAdder.isEmpty() && bossName != null) {

                // Extraemos datos nativos inmutables en el Main Thread
                final Location deathLoc = entity.getLocation().clone();

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
}