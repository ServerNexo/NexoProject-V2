package me.nexo.items.guardarropa;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.database.DatabaseManager; // Asumido desde NexoCore
import me.nexo.core.utils.Base64Util;
import me.nexo.items.NexoItems;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 🎒 NexoItems - Manager del Sistema de Guardarropa (Arquitectura Enterprise Java 21)
 * Rendimiento: Virtual Threads, Inyección Estricta, Anti-Dupe y Folia Region Sync.
 */
@Singleton
public class GuardarropaManager {

    // 🌟 DEPENDENCIAS PROPAGADAS
    private final NexoItems plugin;
    private final DatabaseManager dbManager;
    private final CrossplayUtils crossplayUtils;
    private final Base64Util base64Util;

    // 🚀 MOTOR I/O: Hilos Virtuales para operaciones de Base de Datos
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public GuardarropaManager(NexoItems plugin, DatabaseManager dbManager, CrossplayUtils crossplayUtils, Base64Util base64Util) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        this.crossplayUtils = crossplayUtils;
        this.base64Util = base64Util;
    }

    public void guardarPreset(Player p, int presetId) {
        var armadura = p.getInventory().getArmorContents();
        boolean estaDesnudo = true;

        for (var item : armadura) {
            // 🌟 FIX GHOST ITEMS
            if (item != null && !item.isEmpty()) {
                estaDesnudo = false;
                break;
            }
        }

        if (estaDesnudo) {
            crossplayUtils.sendMessage(p, "&#FF5555[!] No llevas ninguna armadura equipada para guardar en este preset.");
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // Serializamos síncronamente antes de enviar al hilo asíncrono
        String base64Data;
        try {
            base64Data = base64Util.itemStackArrayToBase64(armadura);
        } catch (Exception e) {
            plugin.getLogger().severe("❌ Error serializando armadura: " + e.getMessage());
            return;
        }

        // 🚀 Despachamos a un Hilo Virtual
        virtualExecutor.execute(() -> {
            guardarPresetSync(p.getUniqueId().toString(), presetId, base64Data);

            // 🛡️ FOLIA SYNC: Devolvemos la ejecución a la Región del jugador
            p.getScheduler().run(plugin, task -> {
                crossplayUtils.sendMessage(p, "&#55FF55[✓] <bold>GUARDARROPA:</bold> &#E6CCFFArmadura guardada con éxito en el Preset " + presetId + ".");
                p.playSound(p.getLocation(), Sound.BLOCK_SMITHING_TABLE_USE, 1f, 1f);
                p.closeInventory();
            }, null);
        });
    }

    public void guardarPresetSync(String uuid, int presetId, String base64Data) {
        String sql = "INSERT INTO guardarropa (uuid, preset_id, contenido) VALUES (?, ?, ?) ON CONFLICT (uuid, preset_id) DO UPDATE SET contenido = EXCLUDED.contenido;";
        
        try (var conn = dbManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid);
            ps.setInt(2, presetId);
            ps.setString(3, base64Data);
            ps.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().severe("❌ Error BD guardando preset: " + e.getMessage());
        }
    }

    public void equiparPreset(Player p, int presetId) {
        // 🚀 Despachamos a un Hilo Virtual la búsqueda en la Base de Datos
        virtualExecutor.execute(() -> {
            String sql = "SELECT contenido FROM guardarropa WHERE uuid = ? AND preset_id = ?";
            String base64Data = null;

            try (var conn = dbManager.getConnection();
                 var ps = conn.prepareStatement(sql)) {
                ps.setString(1, p.getUniqueId().toString());
                ps.setInt(2, presetId);
                
                try (var rs = ps.executeQuery()) {
                    if (rs.next()) {
                        base64Data = rs.getString("contenido");
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().severe("❌ Error BD recuperando preset: " + e.getMessage());
            }

            final String finalData = base64Data;

            // 🛡️ FOLIA SYNC: Modificación de inventario en la Región del Jugador
            p.getScheduler().run(plugin, task -> {
                if (finalData == null || finalData.isEmpty()) {
                    crossplayUtils.sendMessage(p, "&#FF5555[!] La ranura del Preset " + presetId + " se encuentra vacía.");
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }

                try {
                    var nuevaArmadura = base64Util.itemStackArrayFromBase64(finalData);
                    var armaduraActual = p.getInventory().getArmorContents();

                    int espaciosNecesarios = 0;
                    for (var item : armaduraActual) {
                        if (item != null && !item.isEmpty()) espaciosNecesarios++;
                    }

                    int espaciosLibres = 0;
                    for (var item : p.getInventory().getStorageContents()) {
                        if (item == null || item.isEmpty()) espaciosLibres++;
                    }

                    if (espaciosLibres < espaciosNecesarios) {
                        crossplayUtils.sendMessage(p, "&#FF5555[!] Necesitas " + espaciosNecesarios + " espacios libres en tu inventario para desequiparte tu armadura actual.");
                        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        return;
                    }

                    // Mover armadura actual al inventario
                    for (var item : armaduraActual) {
                        if (item != null && !item.isEmpty()) {
                            p.getInventory().addItem(item);
                        }
                    }

                    // Equipar nueva armadura
                    p.getInventory().setArmorContents(nuevaArmadura);

                    // Borrar preset de la DB para evitar clonaciones (Despacha otro Virtual Thread)
                    borrarPreset(p, presetId);

                    crossplayUtils.sendMessage(p, "&#55FF55[✓] <bold>GUARDARROPA:</bold> &#E6CCFFSe ha equipado el Preset " + presetId + " exitosamente.");
                    p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_DIAMOND, 1f, 1f);
                    p.closeInventory();

                } catch (Exception e) {
                    plugin.getLogger().severe("❌ Error deserializando armadura en Preset: " + e.getMessage());
                }
            }, null);
        });
    }

    private void borrarPreset(Player p, int presetId) {
        virtualExecutor.execute(() -> {
            String sql = "DELETE FROM guardarropa WHERE uuid = ? AND preset_id = ?";
            try (var conn = dbManager.getConnection();
                 var ps = conn.prepareStatement(sql)) {
                ps.setString(1, p.getUniqueId().toString());
                ps.setInt(2, presetId);
                ps.executeUpdate();
            } catch (Exception e) {
                plugin.getLogger().severe("❌ Error BD borrando preset: " + e.getMessage());
            }
        });
    }
}