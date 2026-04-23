package me.nexo.colecciones.colecciones;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.colecciones.NexoColecciones;
import me.nexo.core.database.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 📚 NexoColecciones - Escucha de Eventos de Farmeo (Arquitectura Enterprise)
 * Rendimiento: Anti-Exploit O(1), I/O Delegado al Cerebro, Cero Service Locator.
 */
@Singleton
public class ColeccionesListener implements Listener {

    private final NexoColecciones plugin;
    private final CollectionManager manager;
    private final DatabaseManager db;
    private final Gson gson;
    
    // 🌟 MOTOR ENTERPRISE: Pool para tareas rápidas de red (Guardado al desconectar)
    private final ExecutorService ioExecutor = Executors.newVirtualThreadPerTaskExecutor();

    private static final String ANTI_EXPLOIT_KEY = "nexo_player_placed";
    private static final String BREWER_KEY = "nexo_last_brewer";

    // 💉 PILAR 1: Inyección de Dependencias
    @Inject
    public ColeccionesListener(NexoColecciones plugin, CollectionManager manager, DatabaseManager db) {
        this.plugin = plugin;
        this.manager = manager; 
        this.db = db;
        this.gson = new Gson(); // Instanciado una sola vez para ahorrar RAM
    }

    // ==========================================
    // 🛡️ ANTI-EXPLOIT (Marcador de Bloques Artificiales)
    // ==========================================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        // Le ponemos una etiqueta invisible a los bloques que coloca el jugador
        event.getBlock().setMetadata(ANTI_EXPLOIT_KEY, new FixedMetadataValue(plugin, true));
    }

    // ==========================================
    // ⛏️ TALA, MINERÍA Y FARMING
    // ==========================================
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        // Si el evento fue cancelado, solo permitimos romper si está en la "Mina" (Zonas seguras)
        if (event.isCancelled() && !event.getPlayer().getWorld().getName().equalsIgnoreCase("Mina")) {
            return;
        }

        var block = event.getBlock();
        String blockId = block.getType().name();

        // 🌾 Lógica especial para Cultivos (Ya que el jugador los planta)
        if (block.getBlockData() instanceof Ageable cultivo) {
            if (cultivo.getAge() < cultivo.getMaximumAge()) return; // Ignoramos si no está maduro

            manager.addProgress(event.getPlayer(), blockId, 1);
            if (block.hasMetadata(ANTI_EXPLOIT_KEY)) block.removeMetadata(ANTI_EXPLOIT_KEY, plugin);
            return;
        }

        // 🚫 Anti-Exploit para bloques normales (Diamante, Madera, etc)
        if (block.hasMetadata(ANTI_EXPLOIT_KEY)) {
            block.removeMetadata(ANTI_EXPLOIT_KEY, plugin); // Quitamos la marca para limpiar memoria
            return; // No otorgamos puntos
        }

        // Si es un bloque 100% natural, sumamos a la colección
        manager.addProgress(event.getPlayer(), blockId, 1);
    }

    // ==========================================
    // ⚔️ FIGHTING (MOBS)
    // ==========================================
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        var killer = event.getEntity().getKiller();
        if (killer != null) {
            String mobType = event.getEntity().getType().name();
            manager.addProgress(killer, mobType, 1);
        }
    }

    // ==========================================
    // 🎣 FISHING (PESCA)
    // ==========================================
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH && event.getCaught() instanceof Item itemCaught) {
            String fishType = itemCaught.getItemStack().getType().name();
            manager.addProgress(event.getPlayer(), fishType, 1);
        }
    }

    // ==========================================
    // 🧪 ALQUIMIA (POCIONES)
    // ==========================================
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getInventory().getType() == InventoryType.BREWING && event.getInventory().getLocation() != null) {
            event.getInventory().getLocation().getBlock().setMetadata(BREWER_KEY, new FixedMetadataValue(plugin, event.getPlayer().getUniqueId().toString()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBrew(BrewEvent event) {
        if (event.getBlock().hasMetadata(BREWER_KEY)) {
            String uuidStr = event.getBlock().getMetadata(BREWER_KEY).get(0).asString();
            Player player = Bukkit.getPlayer(UUID.fromString(uuidStr));

            if (player != null && player.isOnline()) {
                var ingrediente = event.getContents().getIngredient();
                if (ingrediente != null && ingrediente.getType() != Material.AIR) {
                    manager.addProgress(player, ingrediente.getType().name(), 1);
                }
            }
        }
    }

    // ==========================================
    // 📚 ENCANTAMIENTOS
    // ==========================================
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        var player = event.getEnchanter();
        int lapisUsado = event.whichButton() + 1;
        manager.addProgress(player, "LAPIS_LAZULI", lapisUsado);

        if (event.getItem().getType() == Material.BOOK) {
            manager.addProgress(player, "BOOK", 1);
        }
    }

    // ==========================================
    // 📥 GESTIÓN DE DATOS DEL JUGADOR (SQL Asíncrono)
    // ==========================================
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // 🌟 FIX: El cerebro (CollectionManager) ya gestiona sus propios hilos virtuales.
        // Solo necesitamos pasarle el UUID y él hace el resto.
        manager.loadPlayerFromDatabase(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        var profile = manager.getProfile(uuid);

        if (profile != null && profile.isNeedsFlush()) {

            // 🌟 FIX: Guardado mediante Executor Virtual Inyectado. Seguro contra crasheos.
            ioExecutor.submit(() -> {
                String sql = "INSERT INTO nexo_collections (uuid, collections_data, claimed_tiers) VALUES (?, ?::jsonb, ?::jsonb) " +
                        "ON CONFLICT (uuid) DO UPDATE SET collections_data = EXCLUDED.collections_data, claimed_tiers = EXCLUDED.claimed_tiers";

                try (var conn = db.getConnection();
                     var ps = conn.prepareStatement(sql)) {

                    ps.setObject(1, uuid); // Usamos Object para UUID nativo de Supabase
                    ps.setString(2, gson.toJson(profile.getProgressMap()));
                    ps.setString(3, gson.toJson(profile.getClaimedTiersMap()));

                    ps.executeUpdate();

                } catch (Exception e) {
                    plugin.getLogger().severe("❌ Error crítico al guardar el Grimorio de Colecciones de " + event.getPlayer().getName() + ": " + e.getMessage());
                }
            });
        }

        // Limpiamos de la memoria síncronamente al salir para evitar fugas de RAM
        manager.removeProfile(uuid);
    }
}