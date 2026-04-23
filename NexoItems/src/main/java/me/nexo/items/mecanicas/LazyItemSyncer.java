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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 🎒 NexoItems - Sincronizador Perezoso de Ítems (Arquitectura Enterprise Java 21)
 * Rendimiento: Folia Region Scheduler, Virtual Threads unificados, Anti-Ghost Items y Cero Estáticos.
 */
@Singleton
public class LazyItemSyncer implements Listener {

    // 🌟 DEPENDENCIAS PROPAGADAS
    private final NexoItems plugin;
    private final ItemManager itemManager;

    // 🚀 EL MOTOR DE RENDIMIENTO I/O: Hilos Virtuales unificados
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // 🌟 LLAVES CONSTANTES LOCALES (Evita llamadas estáticas a ItemManager y mejora el caché de la CPU)
    private static final NamespacedKey WEAPON_ID_KEY = new NamespacedKey("nexoitems", "weapon_id");
    private static final NamespacedKey TOOL_ID_KEY = new NamespacedKey("nexoitems", "tool_id");
    private static final NamespacedKey ARMOR_ID_KEY = new NamespacedKey("nexoitems", "armor_id");
    
    private static final NamespacedKey REFORGE_KEY = new NamespacedKey("nexoitems", "reforge");
    private static final NamespacedKey ENCHANT_ID_KEY = new NamespacedKey("nexoitems", "enchant_id");
    private static final NamespacedKey ENCHANT_LEVEL_KEY = new NamespacedKey("nexoitems", "enchant_level");
    private static final NamespacedKey PRESTIGE_KEY = new NamespacedKey("nexoitems", "evolution_level");

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public LazyItemSyncer(NexoItems plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
    }

    /**
     * 🛡️ Escaneo Diferido (Lazy Sync)
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onContainerOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        // Escaneamos ambos inventarios de forma segura, pasando al jugador para el scheduler de Folia
        sincronizarInventario(player, event.getInventory());
        sincronizarInventario(player, player.getInventory());
    }

    private void sincronizarInventario(Player player, Inventory inventory) {
        // 🚨 1. LEEMOS EN EL HILO PRINCIPAL (100% Seguro)
        ItemStack[] contents = inventory.getContents();

        // 🌟 FIX OVERHEAD: Iniciamos UN SOLO hilo virtual para procesar todo el array.
        virtualExecutor.execute(() -> {

            for (int i = 0; i < contents.length; i++) {
                var original = contents[i];

                // 🌟 FIX GHOST ITEMS: Uso de isEmpty() nativo de Paper
                if (original == null || original.isEmpty() || !original.hasItemMeta()) continue;

                // 🛡️ FIX CORRUPCIÓN (ITEM WIPE): CLONAMOS el ítem.
                var snapshot = original.clone();
                var snapshotPdc = snapshot.getItemMeta().getPersistentDataContainer();

                // Filtro rápido O(1) para no procesar tierra, piedra, etc.
                if (!snapshotPdc.has(WEAPON_ID_KEY, PersistentDataType.STRING) &&
                        !snapshotPdc.has(TOOL_ID_KEY, PersistentDataType.STRING) &&
                        !snapshotPdc.has(ARMOR_ID_KEY, PersistentDataType.STRING)) {
                    continue;
                }

                // Generamos el nuevo meta asíncronamente
                var nuevoMeta = generarNuevoMeta(snapshot, snapshotPdc);

                if (nuevoMeta != null) {
                    final int slot = i; // Guardamos el slot exacto para la validación

                    // 3. 🛡️ FOLIA SYNC: Volvemos al hilo de la región del jugador para aplicar (Anti-Dupe)
                    player.getScheduler().run(plugin, task -> {

                        // Verificamos que el jugador no haya movido el ítem mientras el hilo virtual pensaba
                        var currentLive = inventory.getItem(slot);

                        if (currentLive != null && currentLive.getType() == original.getType() && currentLive.hasItemMeta()) {
                            // Aplicamos los datos frescos de manera segura
                            currentLive.setItemMeta(nuevoMeta);

                            // 🌟 USO DE DEPENDENCIA INYECTADA (Cero estáticos)
                            itemManager.sincronizarItemAsync(currentLive);
                        }
                    }, null);
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
            // 🌟 Delegamos la generación a la instancia inyectada de ItemManager
            if (oldPdc.has(WEAPON_ID_KEY, PersistentDataType.STRING)) {
                String id = oldPdc.get(WEAPON_ID_KEY, PersistentDataType.STRING);
                freshTemplate = itemManager.generarArmaRPG(id);
            }
            else if (oldPdc.has(TOOL_ID_KEY, PersistentDataType.STRING)) {
                String id = oldPdc.get(TOOL_ID_KEY, PersistentDataType.STRING);
                freshTemplate = itemManager.generarHerramientaProfesion(id);
            }
            else if (oldPdc.has(ARMOR_ID_KEY, PersistentDataType.STRING)) {
                String id = oldPdc.get(ARMOR_ID_KEY, PersistentDataType.STRING);

                // 🌟 FIX CRÍTICO: Extracción segura del tipo de pieza
                String matName = snapshot.getType().name();
                String tipoPieza = matName.contains("_") ? matName.substring(matName.indexOf('_') + 1) : matName;

                freshTemplate = itemManager.generarArmaduraProfesion(id, tipoPieza);
            }
        } catch (Exception e) {
            return null; // Si el YAML de ese ítem fue borrado, abortamos
        }

        if (freshTemplate == null || !freshTemplate.hasItemMeta()) return null;

        var freshMeta = freshTemplate.getItemMeta();
        var freshPdc = freshMeta.getPersistentDataContainer();

        // 🛡️ MIGRACIÓN SEGURA DE DATOS VALIOSOS DEL JUGADOR

        if (oldPdc.has(REFORGE_KEY, PersistentDataType.STRING)) {
            freshPdc.set(REFORGE_KEY, PersistentDataType.STRING, oldPdc.get(REFORGE_KEY, PersistentDataType.STRING));
        }

        if (oldPdc.has(ENCHANT_ID_KEY, PersistentDataType.STRING)) {
            freshPdc.set(ENCHANT_ID_KEY, PersistentDataType.STRING, oldPdc.get(ENCHANT_ID_KEY, PersistentDataType.STRING));

            if (oldPdc.has(ENCHANT_LEVEL_KEY, PersistentDataType.INTEGER)) {
                freshPdc.set(ENCHANT_LEVEL_KEY, PersistentDataType.INTEGER, oldPdc.get(ENCHANT_LEVEL_KEY, PersistentDataType.INTEGER));
            }
        }

        if (oldPdc.has(PRESTIGE_KEY, PersistentDataType.INTEGER)) {
            int level = oldPdc.get(PRESTIGE_KEY, PersistentDataType.INTEGER);
            freshPdc.set(PRESTIGE_KEY, PersistentDataType.INTEGER, Math.min(level, 60)); // Cap Nivel 60 preservado
        }

        return freshMeta;
    }
}