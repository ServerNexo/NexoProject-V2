package me.nexo.items.mecanicas;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.items.NexoItems;
import me.nexo.items.managers.ItemManager;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * 🎒 NexoItems - Sincronizador Perezoso de Ítems (Arquitectura Enterprise)
 * Rendimiento: Clonación Thread-Safe, Prevención de Item Wipes, Batch Virtual Threads y Safe Array Parsing.
 */
@Singleton
public class LazyItemSyncer implements Listener {

    private final NexoItems plugin;

    // 🌟 Claves de datos inmutables
    private final NamespacedKey reforgeKey;
    private final NamespacedKey enchantKey;
    private final NamespacedKey prestigeKey;

    @Inject
    public LazyItemSyncer(NexoItems plugin) {
        this.plugin = plugin;
        this.reforgeKey = ItemManager.llaveReforja;
        this.enchantKey = ItemManager.llaveEnchantId;
        this.prestigeKey = ItemManager.llaveNivelEvolucion;
    }

    /**
     * 🛡️ Escaneo Diferido (Lazy Sync)
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onContainerOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        // Escaneamos ambos inventarios de forma segura
        sincronizarInventario(event.getInventory());
        sincronizarInventario(event.getPlayer().getInventory());
    }

    private void sincronizarInventario(Inventory inventory) {
        // 🚨 1. LEEMOS EN EL HILO PRINCIPAL (100% Seguro)
        ItemStack[] contents = inventory.getContents();

        // 🌟 FIX OVERHEAD: Iniciamos UN SOLO hilo virtual para procesar todo el array, no 54 hilos.
        Thread.startVirtualThread(() -> {

            for (int i = 0; i < contents.length; i++) {
                ItemStack original = contents[i];

                if (original == null || original.getType().isAir() || !original.hasItemMeta()) continue;

                // 🛡️ FIX CORRUPCIÓN (ITEM WIPE): CLONAMOS el ítem.
                // Trabajar con el original asíncronamente causa ConcurrentModificationException si el jugador lo mueve.
                ItemStack snapshot = original.clone();
                PersistentDataContainer snapshotPdc = snapshot.getItemMeta().getPersistentDataContainer();

                // Filtro rápido O(1) para no procesar tierra, piedra, etc.
                if (!snapshotPdc.has(ItemManager.llaveWeaponId, PersistentDataType.STRING) &&
                        !snapshotPdc.has(ItemManager.llaveHerramientaId, PersistentDataType.STRING) &&
                        !snapshotPdc.has(ItemManager.llaveArmaduraId, PersistentDataType.STRING)) {
                    continue;
                }

                // Generamos el nuevo meta asíncronamente
                ItemMeta nuevoMeta = generarNuevoMeta(snapshot, snapshotPdc);

                if (nuevoMeta != null) {
                    final int slot = i; // Guardamos el slot exacto para la validación

                    // 3. 🛡️ VOLVEMOS AL HILO PRINCIPAL PARA APLICAR (Anti-Dupe)
                    plugin.getServer().getScheduler().runTask(plugin, () -> {

                        // Verificamos que el jugador no haya movido el ítem mientras el hilo virtual pensaba
                        ItemStack currentLive = inventory.getItem(slot);

                        if (currentLive != null && currentLive.getType() == original.getType() && currentLive.hasItemMeta()) {
                            // Aplicamos los datos frescos de manera segura
                            currentLive.setItemMeta(nuevoMeta);

                            // Llamamos al sincronizador de Lore/Nombres asíncrono
                            ItemManager.sincronizarItemAsync(currentLive);
                        }
                    });
                }
            }
        });
    }

    /**
     * 🧠 PROCESAMIENTO MATEMÁTICO ASÍNCRONO
     * Toma el Snapshot y devuelve el ItemMeta fresco sin tocar el mundo de Bukkit.
     */
    private ItemMeta generarNuevoMeta(ItemStack snapshot, PersistentDataContainer oldPdc) {
        ItemStack freshTemplate = null;

        try {
            if (oldPdc.has(ItemManager.llaveWeaponId, PersistentDataType.STRING)) {
                String id = oldPdc.get(ItemManager.llaveWeaponId, PersistentDataType.STRING);
                freshTemplate = ItemManager.generarArmaRPG(id);
            }
            else if (oldPdc.has(ItemManager.llaveHerramientaId, PersistentDataType.STRING)) {
                String id = oldPdc.get(ItemManager.llaveHerramientaId, PersistentDataType.STRING);
                freshTemplate = ItemManager.generarHerramientaProfesion(id);
            }
            else if (oldPdc.has(ItemManager.llaveArmaduraId, PersistentDataType.STRING)) {
                String id = oldPdc.get(ItemManager.llaveArmaduraId, PersistentDataType.STRING);

                // 🌟 FIX CRÍTICO (ArrayIndexOutOfBounds): Extracción segura del tipo de pieza
                String matName = snapshot.getType().name();
                String tipoPieza = matName.contains("_") ? matName.substring(matName.indexOf('_') + 1) : matName;

                freshTemplate = ItemManager.generarArmaduraProfesion(id, tipoPieza);
            }
        } catch (Exception e) {
            return null; // Si el YAML de ese ítem fue borrado, abortamos
        }

        if (freshTemplate == null || !freshTemplate.hasItemMeta()) return null;

        ItemMeta freshMeta = freshTemplate.getItemMeta();
        PersistentDataContainer freshPdc = freshMeta.getPersistentDataContainer();

        // 🛡️ MIGRACIÓN SEGURA DE DATOS VALIOSOS DEL JUGADOR

        if (oldPdc.has(reforgeKey, PersistentDataType.STRING)) {
            freshPdc.set(reforgeKey, PersistentDataType.STRING, oldPdc.get(reforgeKey, PersistentDataType.STRING));
        }

        if (oldPdc.has(enchantKey, PersistentDataType.STRING)) {
            freshPdc.set(enchantKey, PersistentDataType.STRING, oldPdc.get(enchantKey, PersistentDataType.STRING));

            if (oldPdc.has(ItemManager.llaveEnchantNivel, PersistentDataType.INTEGER)) {
                freshPdc.set(ItemManager.llaveEnchantNivel, PersistentDataType.INTEGER, oldPdc.get(ItemManager.llaveEnchantNivel, PersistentDataType.INTEGER));
            }
        }

        if (oldPdc.has(prestigeKey, PersistentDataType.INTEGER)) {
            int level = oldPdc.get(prestigeKey, PersistentDataType.INTEGER);
            freshPdc.set(prestigeKey, PersistentDataType.INTEGER, Math.min(level, 60)); // Cap Nivel 60
        }

        return freshMeta;
    }
}