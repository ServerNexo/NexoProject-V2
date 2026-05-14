package me.aeroxis.minions.manager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.colecciones.colecciones.CollectionManager;
import me.aeroxis.core.crossplay.CrossplayUtils;
import me.aeroxis.islas.data.IslandProfile;
import me.aeroxis.islas.managers.IslandLevelEngine;
import me.aeroxis.islas.managers.IslandManager;
import me.aeroxis.minions.AeroxisMinions;
import me.aeroxis.minions.config.ConfigManager;
import me.aeroxis.minions.data.MinionDNA;
import me.aeroxis.minions.data.MinionKeys;
import me.aeroxis.minions.data.UpgradesConfig;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 🤖 AeroxisMinions - Gestor de Minions (Arquitectura Enterprise Java 21+)
 * Rendimiento: Matemáticas Asíncronas + Inyección de Genoma (Omni-Minion).
 */
@Singleton
public class MinionManager {

    private final AeroxisMinions plugin;
    private final ConfigManager configManager;
    private final CrossplayUtils crossplayUtils;

    private final UpgradesConfig upgradesConfig;
    private final CollectionManager collectionManager;

    // 🌟 DEPENDENCIAS DE ISLA
    private final IslandManager islandManager;
    private final IslandLevelEngine islandLevelEngine;

    // 🌟 MOTOR ENTERPRISE: Executor formal para el Tick Asíncrono Masivo
    private final ExecutorService tickExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // Mapa Concurrente para operaciones Thread-Safe
    private final ConcurrentHashMap<UUID, ActiveMinion> minionsActivos = new ConcurrentHashMap<>();

    @Inject
    public MinionManager(AeroxisMinions plugin, ConfigManager configManager, CrossplayUtils crossplayUtils,
                         UpgradesConfig upgradesConfig, CollectionManager collectionManager,
                         IslandManager islandManager, IslandLevelEngine islandLevelEngine) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.crossplayUtils = crossplayUtils;
        this.upgradesConfig = upgradesConfig;
        this.collectionManager = collectionManager;
        this.islandManager = islandManager;
        this.islandLevelEngine = islandLevelEngine;
    }

    // ==========================================
    // ⚙️ GESTIÓN DE CICLO DE VIDA (SPAWN Y REMOVE)
    // ==========================================

    // 🌟 FASE 3: Reemplazado MinionType por String productionId (Omni-Minion)
    public void spawnMinion(Location loc, UUID ownerId, String productionId, int tier) {
        // 🧬 Creamos el ADN Base del recién nacido (Inmutable)
        MinionDNA initialDna = MinionDNA.createBase(ownerId, productionId, tier);

        // El spawn físico DEBE ocurrir en el hilo principal de Bukkit/Folia
        loc.getWorld().spawn(loc, ItemDisplay.class, display -> {

            // 🌟 FASE 3 VISUAL: El minion toma la forma del material que produce
            try {
                Material visualMat = Material.valueOf(productionId);
                display.setItemStack(new ItemStack(visualMat));
            } catch (Exception e) {
                display.setItemStack(new ItemStack(Material.COBBLESTONE)); // Fallback de seguridad
            }

            display.setBillboard(ItemDisplay.Billboard.FIXED);
            display.setInvulnerable(true);
            display.setInterpolationDuration(20);
            display.setInterpolationDelay(0);

            var pdc = display.getPersistentDataContainer();

            // 🌟 MAGIA AAA: Guardamos toda la información en un solo paso binario ultra-rápido
            pdc.set(MinionKeys.DNA_KEY, MinionKeys.DNA_TYPE, initialDna);

            var hitbox = loc.getWorld().spawn(loc, Interaction.class, inter -> {
                inter.setInteractionWidth(1.2f);
                inter.setInteractionHeight(1.5f);
                inter.getPersistentDataContainer().set(MinionKeys.INTERACTION_ID, PersistentDataType.STRING, display.getUniqueId().toString());
            });

            var holoLoc = loc.clone().add(0, 1.2, 0);
            var holograma = loc.getWorld().spawn(holoLoc, TextDisplay.class, holo -> {
                holo.setBillboard(TextDisplay.Billboard.CENTER);
                holo.setBackgroundColor(Color.fromARGB(100, 0, 0, 0));
                holo.text(crossplayUtils.parseCrossplay(null, "&#55FF55[⚙] Iniciando Sistemas..."));
            });

            pdc.set(MinionKeys.HOLO_ID, PersistentDataType.STRING, holograma.getUniqueId().toString());

            minionsActivos.put(display.getUniqueId(), new ActiveMinion(
                    plugin, display, hitbox, holograma, initialDna,
                    upgradesConfig, this, crossplayUtils, collectionManager,
                    islandManager, islandLevelEngine
            ));
        });
    }

    public void recogerMinion(Player player, UUID displayId) {
        var minion = minionsActivos.remove(displayId);
        if (minion == null) return;

        MinionDNA dna = minion.getDna();

        // Entregar Upgrades al jugador
        for (ItemStack upgrade : minion.getUpgrades()) {
            if (upgrade != null && !upgrade.isEmpty()) {
                player.getInventory().addItem(upgrade).values().forEach(drop ->
                        player.getWorld().dropItemNaturally(player.getLocation(), drop)
                );
            }
        }

        // Entregar botín almacenado al jugador (Dinámico según su producción actual)
        if (dna.storedItems() > 0) {
            int cantidad = dna.storedItems();

            // 🌟 FASE 3: Obtenemos el Material desde el String dinámico
            Material mat = Material.COBBLESTONE; // Default
            try { mat = Material.valueOf(dna.currentProductionId()); } catch (Exception ignored) {}

            while (cantidad > 0) {
                int dar = Math.min(cantidad, 64);
                player.getInventory().addItem(new ItemStack(mat, dar)).values().forEach(drop ->
                        player.getWorld().dropItemNaturally(player.getLocation(), drop)
                );
                cantidad -= dar;
            }
            crossplayUtils.sendMessage(player, "&#55FF55[✓] Extracción remota completada. Ítems recuperados: &#FFAA00" + dna.storedItems());
        }

        Location minionLoc = minion.getEntity().getLocation();

        // Eliminar las 3 entidades del mundo (Display, Hitbox y Holograma)
        if (minion.getEntity() != null) minion.getEntity().remove();
        if (minion.getHitbox() != null) minion.getHitbox().remove();
        if (minion.getHolograma() != null) minion.getHolograma().remove();

        // 🌟 Lógica de límites usando AeroxisIslas
        IslandProfile profile = islandManager.getIslandAt(minionLoc);
        if (profile != null) {
            long placedMinions = minionsActivos.values().stream()
                    .filter(m -> m.getEntity().isValid() && m.getEntity().getLocation().getWorld().equals(minionLoc.getWorld()))
                    .count();

            Player owner = Bukkit.getPlayer(dna.ownerId());
            if (owner != null && owner.isOnline()) {
                if (owner.getUniqueId().equals(player.getUniqueId())) {
                    crossplayUtils.sendMessage(owner, "&#FF5555[!] Has desmantelado a un operario automatizado. Tienes: &#FFAA00" + placedMinions + " / " + profile.getRealMinionLimit());
                } else {
                    crossplayUtils.sendMessage(owner, "&#FF5555[!] ¡Alerta! Un administrador ha desmantelado uno de tus Minions.");
                    crossplayUtils.sendMessage(player, "&#55FF55[✓] Desmantelamiento administrativo exitoso.");
                }
            }
        }

        // Devolvemos el ítem (Omni-Minion en forma de huevo) al jugador
        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "minion give " + player.getName() + " OMNI_MINION " + dna.tier());
    }

    // ==========================================
    // 🚀 EL MOTOR ASÍNCRONO (TICK ENGINE)
    // ==========================================
    public void tickAll(long currentTimeMillis) {
        tickExecutor.submit(() -> {
            for (ActiveMinion minion : minionsActivos.values()) {
                minion.tick(currentTimeMillis);
            }
        });
    }

    // ==========================================
    // 💾 SISTEMA DE GUARDADO
    // ==========================================
    public void saveAllMinionsSync() {
        for (ActiveMinion minion : minionsActivos.values()) {
            minion.saveData();
        }
        plugin.getLogger().info("💾 Progreso de " + minionsActivos.size() + " Minions guardado de forma segura.");
    }

    public ActiveMinion getMinion(UUID displayId) {
        return minionsActivos.get(displayId);
    }

    public ConcurrentHashMap<UUID, ActiveMinion> getMinionsActivos() {
        return minionsActivos;
    }
}