package me.nexo.items.mochilas;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.database.DatabaseManager;
import me.nexo.core.utils.Base64Util;
import me.nexo.items.NexoItems;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 🎒 NexoItems - Manager de Mochilas (Arquitectura Enterprise Java 21)
 * Rendimiento: Virtual Threads para I/O, Folia Region Scheduler y Cero Service Locators.
 */
@Singleton
public class MochilaManager {

    // 🌟 DEPENDENCIAS PROPAGADAS
    private final NexoItems plugin;
    private final DatabaseManager dbManager;
    private final CrossplayUtils crossplayUtils;
    private final Base64Util base64Util;

    // 🚀 EL MOTOR DE RENDIMIENTO I/O: Hilos Virtuales nativos (Ciclo de vida atado al Singleton)
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // 🛡️ PATRÓN ENTERPRISE: InventoryHolder Personalizado
    public static class MochilaHolder implements InventoryHolder {
        private final Inventory inventory;
        private final int mochilaId;

        public MochilaHolder(int mochilaId, net.kyori.adventure.text.Component title) {
            this.mochilaId = mochilaId;
            this.inventory = Bukkit.createInventory(this, 54, title);
        }

        @Override
        public Inventory getInventory() { return inventory; }
        public int getMochilaId() { return mochilaId; }
    }

    // 💉 PILAR 1: Inyección de Dependencias Estricta
    @Inject
    public MochilaManager(NexoItems plugin, DatabaseManager dbManager, CrossplayUtils crossplayUtils, Base64Util base64Util) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        this.crossplayUtils = crossplayUtils;
        this.base64Util = base64Util;
    }

    public void abrirMochila(Player p, int id) {
        // 🚀 Despachamos al hilo virtual para leer la base de datos
        virtualExecutor.execute(() -> {
            String base64Data = null;
            String sql = "SELECT contenido FROM mochilas WHERE uuid = ? AND mochila_id = ?";

            try (var conn = dbManager.getConnection();
                 var ps = conn.prepareStatement(sql)) {
                 
                ps.setString(1, p.getUniqueId().toString());
                ps.setInt(2, id);
                
                try (var rs = ps.executeQuery()) {
                    if (rs.next()) {
                        base64Data = rs.getString("contenido");
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().severe("❌ Error cargando mochila de la BD: " + e.getMessage());
                return;
            }

            final String finalData = base64Data;

            // 🌟 FOLIA SYNC: Volvemos al hilo de la región del jugador para crear y abrir el inventario
            p.getScheduler().run(plugin, task -> {
                var titulo = crossplayUtils.parseCrossplay(p, "&#555555<bold>»</bold> &#00E5FFMochila Virtual #" + id);
                var holder = new MochilaHolder(id, titulo);
                var inv = holder.getInventory();

                if (finalData != null && !finalData.isEmpty()) {
                    try {
                        ItemStack[] items = base64Util.itemStackArrayFromBase64(finalData);
                        inv.setContents(items);
                    } catch (Exception e) {
                        plugin.getLogger().severe("❌ Error deserializando la mochila de " + p.getName() + ": " + e.getMessage());
                    }
                }

                p.openInventory(inv);
                p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f);
            }, null);
        });
    }

    public void guardarMochila(Player p, int id, Inventory inv) {
        // Obtenemos los contenidos en el hilo síncrono antes de saltar al virtual
        ItemStack[] contents = inv.getContents();
        
        virtualExecutor.execute(() -> {
            try {
                String base64Data = base64Util.itemStackArrayToBase64(contents);
                guardarMochilaSync(p.getUniqueId().toString(), id, base64Data);
            } catch (Exception e) {
                plugin.getLogger().severe("❌ Error serializando la mochila de " + p.getName() + ": " + e.getMessage());
            }
        });
    }

    /**
     * 🛡️ Puede ser llamado de manera segura desde el VirtualExecutor o síncronamente durante el onDisable()
     */
    public void guardarMochilaSync(String uuid, int id, String base64Data) {
        String sql = "INSERT INTO mochilas (uuid, mochila_id, contenido) VALUES (?, ?, ?) ON CONFLICT (uuid, mochila_id) DO UPDATE SET contenido = EXCLUDED.contenido;";
        
        try (var conn = dbManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
             
            ps.setString(1, uuid);
            ps.setInt(2, id);
            ps.setString(3, base64Data);
            ps.executeUpdate();
            
        } catch (Exception e) {
            plugin.getLogger().severe("❌ Error guardando mochila en la Base de Datos: " + e.getMessage());
        }
    }
}