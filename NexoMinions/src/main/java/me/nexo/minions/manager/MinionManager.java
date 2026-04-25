package me.nexo.minions.manager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.nexomc.nexo.api.NexoItems;
import me.nexo.colecciones.colecciones.CollectionManager;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.minions.NexoMinions;
import me.nexo.minions.config.ConfigManager;
import me.nexo.minions.data.MinionDNA;
import me.nexo.minions.data.MinionKeys;
import me.nexo.minions.data.MinionType;
import me.nexo.minions.data.UpgradesConfig;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
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
 * 🤖 NexoMinions - Gestor de Minions (Arquitectura Enterprise Java 25)
 * Rendimiento: Matemáticas Asíncronas (Executor Virtual) + Inyección de Genoma (DNA).
 */
@Singleton
public class MinionManager {

    private final NexoMinions plugin;
    private final ConfigManager configManager;
    private final CrossplayUtils crossplayUtils;

    // 🌟 DEPENDENCIAS PROPAGADAS PARA EL ACTIVE MINION
    private final UpgradesConfig upgradesConfig;
    private final CollectionManager collectionManager;

    // 🌟 MOTOR ENTERPRISE: Executor formal para el Tick Asíncrono Masivo (Java 21+)
    private final ExecutorService tickExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // Mapa Concurrente para operaciones Thread-Safe
    private final ConcurrentHashMap<UUID, ActiveMinion> minionsActivos = new ConcurrentHashMap<>();

    // 💉 PILAR 1: Inyección Directa (Añadimos UpgradesConfig y CollectionManager)
    @Inject
    public MinionManager(NexoMinions plugin, ConfigManager configManager, CrossplayUtils crossplayUtils,
                         UpgradesConfig upgradesConfig, CollectionManager collectionManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.crossplayUtils = crossplayUtils;
        this.upgradesConfig = upgradesConfig;
        this.collectionManager = collectionManager;
    }

    // ==========================================
    // ⚙️ GESTIÓN DE CICLO DE VIDA (SPAWN Y REMOVE)
    // ==========================================
    public void spawnMinion(Location loc, UUID ownerId, MinionType type, int tier) {
        // 🧬 Creamos el ADN Base del recién nacido (Inmutable)
        MinionDNA initialDna = MinionDNA.createBase(ownerId, type, tier);

        // El spawn físico DEBE ocurrir en el hilo principal
        loc.getWorld().spawn(loc, ItemDisplay.class, display -> {
            var nexoItemBuilder = NexoItems.itemFromId(type.getNexoModelID());
            if (nexoItemBuilder != null) display.setItemStack(nexoItemBuilder.build());

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

            // 🌟 Inyectamos el ADN al objeto de memoria para arrancar la máquina de estado
            minionsActivos.put(display.getUniqueId(), new ActiveMinion(
                    plugin, display, hitbox, holograma, initialDna,
                    upgradesConfig, this, crossplayUtils, collectionManager
            ));
        });
    }

    public void recogerMinion(Player player, UUID displayId) {
        var minion = minionsActivos.remove(displayId);
        if (minion == null) return;

        MinionDNA dna = minion.getDna(); // Extraemos el genoma actual

        // Entregar Upgrades al jugador
        for (ItemStack upgrade : minion.getUpgrades()) {
            if (upgrade != null && !upgrade.isEmpty()) {
                player.getInventory().addItem(upgrade).values().forEach(drop ->
                        player.getWorld().dropItemNaturally(player.getLocation(), drop)
                );
            }
        }

        // Entregar botín almacenado al jugador leyendo el ADN
        if (dna.storedItems() > 0) {
            int cantidad = dna.storedItems();
            var mat = dna.type().getTargetMaterial();

            while (cantidad > 0) {
                int dar = Math.min(cantidad, 64);
                player.getInventory().addItem(new ItemStack(mat, dar)).values().forEach(drop ->
                        player.getWorld().dropItemNaturally(player.getLocation(), drop)
                );
                cantidad -= dar;
            }
            crossplayUtils.sendMessage(player, "&#55FF55[✓] Extracción remota completada. Ítems recuperados: &#FFAA00" + dna.storedItems());
        }

        // Eliminar las 3 entidades del mundo (Display, Hitbox y Holograma)
        if (minion.getEntity() != null) minion.getEntity().remove();
        if (minion.getHitbox() != null) minion.getHitbox().remove();
        if (minion.getHolograma() != null) minion.getHolograma().remove();

        // Lógica de límites
        var owner = Bukkit.getPlayer(dna.ownerId());
        if (owner != null && owner.isOnline()) {
            addPlacedMinion(owner, -1); // Restamos 1 al límite

            if (owner.getUniqueId().equals(player.getUniqueId())) {
                crossplayUtils.sendMessage(owner, "&#FF5555[!] Has desmantelado a tu operario automatizado. Tienes: &#FFAA00" + getPlacedMinions(owner) + " / " + getMaxMinions(owner));
            } else {
                crossplayUtils.sendMessage(owner, "&#FF5555[!] ¡Alerta! Un administrador ha desmantelado uno de tus Minions.");
                crossplayUtils.sendMessage(player, "&#55FF55[✓] Desmantelamiento administrativo exitoso.");
            }
        }

        // Devolvemos el ítem (Minion en forma de huevo) al jugador
        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "minion give " + player.getName() + " " + dna.type().name() + " " + dna.tier());
    }

    // ==========================================
    // 🚀 EL MOTOR ASÍNCRONO (TICK ENGINE)
    // ==========================================
    public void tickAll(long currentTimeMillis) {
        // 🌟 MAGIA ENTERPRISE: Procesamiento Asíncrono Masivo Gestionado.
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
            minion.saveData(); // Obliga a guardar variables RAM -> ADN Binario de la Entidad
        }
        plugin.getLogger().info("💾 Progreso de " + minionsActivos.size() + " Minions guardado de forma segura en sus entidades.");
    }

    // ==========================================
    // ⚙️ UTILIDADES DE LÍMITE (PDC CACHEADO)
    // ==========================================
    public ActiveMinion getMinion(UUID displayId) {
        return minionsActivos.get(displayId);
    }

    public int getPlacedMinions(Player player) {
        return player.getPersistentDataContainer().getOrDefault(MinionKeys.PLACED_LIMIT, PersistentDataType.INTEGER, 0);
    }

    public void addPlacedMinion(Player player, int amount) {
        int current = getPlacedMinions(player);
        player.getPersistentDataContainer().set(MinionKeys.PLACED_LIMIT, PersistentDataType.INTEGER, Math.max(0, current + amount));
    }

    public int getMaxMinions(Player player) {
        for (int i = 50; i >= 1; i--) {
            if (player.hasPermission("nexominions.limit." + i)) return i;
        }
        return 5; // Default seguro
    }

    public ConcurrentHashMap<UUID, ActiveMinion> getMinionsActivos() {
        return minionsActivos;
    }
}