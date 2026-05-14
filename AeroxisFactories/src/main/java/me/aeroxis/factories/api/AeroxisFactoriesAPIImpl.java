package me.aeroxis.factories.api;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.core.api.AeroxisFactoriesAPI;
import me.aeroxis.core.database.DatabaseManager; // 🌟 IMPORTANTE
import me.aeroxis.factories.AeroxisFactories;
import me.aeroxis.factories.core.ActiveFactory;
import me.aeroxis.factories.managers.FactoryManager;
import me.aeroxis.factories.visuals.FactoryVisualEngine;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * 🔌 AeroxisFactories - Implementación de la API de Enrutamiento
 * Rendimiento: Item Sorter O(N), Soporte Folia y Enrutamiento a la Nube (Silos VIP).
 */
@Singleton
public class AeroxisFactoriesAPIImpl implements AeroxisFactoriesAPI {

    private final AeroxisFactories plugin;
    private final FactoryManager factoryManager;
    private final FactoryVisualEngine visualEngine;
    private final DatabaseManager databaseManager; // 🌟 INYECTADO PARA LA NUBE

    @Inject
    public AeroxisFactoriesAPIImpl(AeroxisFactories plugin, FactoryManager factoryManager, FactoryVisualEngine visualEngine, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.factoryManager = factoryManager;
        this.visualEngine = visualEngine;
        this.databaseManager = databaseManager;
    }

    @Override
    public void routeItem(Location origin, UUID targetLinkId, ItemStack item) {

        // ☁️ 1. ¿EL DESTINO ES LA NUBE DE UN JUGADOR (Silo VIP)?
        OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetLinkId);
        if (offlineTarget.hasPlayedBefore() || offlineTarget.isOnline()) {
            addItemsToCloudAsync(targetLinkId, item);
            visualEngine.playProductionAnimation(origin, item); // Animación voladora "hacia el cielo"
            return;
        }

        // 🏭 2. ES UNA FÁBRICA O ALMACÉN FÍSICO
        ActiveFactory target = factoryManager.getFactoryById(targetLinkId);

        if (target != null) {
            // 🟢 EL CHUNK DESTINO ESTÁ CARGADO EN RAM
            if (target.getFactoryType().equalsIgnoreCase("ALMACEN_CENTRAL")) {
                // Lógica de Sorter Automático (En el Hilo del Chunk)
                Bukkit.getRegionScheduler().execute(plugin, target.getCoreLocation(), () -> {
                    procesarSorterFisico(target.getCoreLocation(), item.clone());
                });
            } else {
                // Es una fábrica normal, va a su memoria virtual
                target.addOutput(item.getAmount());
            }

            // Disparamos la animación voladora hacia el destino
            visualEngine.playProductionAnimation(origin, item);
        } else {
            // 🔴 EL CHUNK ESTÁ DESCARGADO (Deferred Queue de Fábrica)
            factoryManager.addDeferredOutputAsync(targetLinkId, item.getAmount());
        }
    }

    // ==========================================
    // ☁️ LOGÍSTICA EN LA NUBE (SILO VIP)
    // ==========================================
    private void addItemsToCloudAsync(UUID playerId, ItemStack item) {
        CompletableFuture.runAsync(() -> {
            // Usamos UPSERT de PostgreSQL (ON CONFLICT) para insertar o sumar si ya existe
            String sql = "INSERT INTO nexo_silos (player_id, item_id, amount) VALUES (CAST(? AS UUID), ?, ?) " +
                    "ON CONFLICT (player_id, item_id) DO UPDATE SET amount = nexo_silos.amount + EXCLUDED.amount";
            try (var conn = databaseManager.getConnection();
                 var ps = conn.prepareStatement(sql)) {

                ps.setString(1, playerId.toString());
                ps.setString(2, item.getType().name()); // O el Custom Model ID de Nexo
                ps.setInt(3, item.getAmount());
                ps.executeUpdate();

            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "❌ Error guardando en la Nube (Silo)", e);
            }
        });
    }

    // ==========================================
    // 🧠 ALGORITMO DE CLASIFICACIÓN FÍSICA (SORTER)
    // ==========================================
    private void procesarSorterFisico(Location core, ItemStack itemToStore) {
        int radius = 3; // Radio de alcance del Almacén (7x7x7)

        // 🔍 PASADA 1: Búsqueda Inteligente (Filtro por contenido o ItemFrame)
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block block = core.clone().add(x, y, z).getBlock();

                    if (block.getState() instanceof Container container) {
                        // Ignoramos si está lleno
                        if (!container.getInventory().containsAtLeast(itemToStore, 1) && !container.getInventory().contains(Material.AIR)) continue;

                        boolean esCofreCorrecto = false;

                        if (container.getInventory().contains(itemToStore.getType())) {
                            esCofreCorrecto = true;
                        } else {
                            for (org.bukkit.entity.Entity e : block.getWorld().getNearbyEntities(block.getLocation().add(0.5, 0.5, 0.5), 1.0, 1.0, 1.0)) {
                                if (e instanceof ItemFrame frame) {
                                    if (frame.getItem().getType() == itemToStore.getType()) {
                                        esCofreCorrecto = true;
                                        break;
                                    }
                                }
                            }
                        }

                        if (esCofreCorrecto) {
                            var leftover = container.getInventory().addItem(itemToStore);
                            if (leftover.isEmpty()) return;
                            itemToStore = leftover.get(0);
                        }
                    }
                }
            }
        }

        // 📥 PASADA 2: Búsqueda de Relleno (Meterlo en cualquier cofre con espacio vacío)
        if (itemToStore.getAmount() > 0) {
            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        Block block = core.clone().add(x, y, z).getBlock();
                        if (block.getState() instanceof Container container) {
                            var leftover = container.getInventory().addItem(itemToStore);
                            if (leftover.isEmpty()) return;
                            itemToStore = leftover.get(0);
                        }
                    }
                }
            }
        }

        // 💥 PASADA 3: Desbordamiento (Almacén Lleno)
        if (itemToStore.getAmount() > 0) {
            core.getWorld().dropItemNaturally(core.clone().add(0, 1, 0), itemToStore);
        }
    }
}