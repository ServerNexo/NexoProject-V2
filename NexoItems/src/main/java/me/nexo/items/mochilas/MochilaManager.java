package me.nexo.items.mochilas;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.items.NexoItems;
import me.nexo.core.utils.Base64Util;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Singleton
public class MochilaManager {

    private final NexoItems plugin;

    // 🛡️ PATRÓN ENTERPRISE: InventoryHolder Personalizado (Guarda el ID de la mochila adentro)
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

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public MochilaManager(NexoItems plugin) {
        this.plugin = plugin;
    }

    public void abrirMochila(Player p, int id) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            NexoCore nexoCore = (NexoCore) Bukkit.getPluginManager().getPlugin("NexoCore");
            if (nexoCore == null || nexoCore.getDatabaseManager() == null) {
                CrossplayUtils.sendMessage(p, "&#FF5555[!] Error crítico: Enlace caído con la Base de Datos Central.");
                return;
            }

            String base64Data = null;
            String sql = "SELECT contenido FROM mochilas WHERE uuid = ? AND mochila_id = ?";

            try (Connection conn = nexoCore.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, p.getUniqueId().toString());
                ps.setInt(2, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    base64Data = rs.getString("contenido");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            String finalData = base64Data;

            Bukkit.getScheduler().runTask(plugin, () -> {
                // 🌟 FIX: Creación de inventario Inhackeable con Component
                net.kyori.adventure.text.Component titulo = CrossplayUtils.parseCrossplay(p, "&#555555<bold>»</bold> &#00E5FFMochila Virtual #" + id);
                MochilaHolder holder = new MochilaHolder(id, titulo);
                Inventory inv = holder.getInventory();

                if (finalData != null && !finalData.isEmpty()) {
                    ItemStack[] items = Base64Util.itemStackArrayFromBase64(finalData);
                    inv.setContents(items);
                }

                p.openInventory(inv);
                p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f);
            });
        });
    }

    public void guardarMochila(Player p, int id, Inventory inv) {
        ItemStack[] contents = inv.getContents();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String base64Data = Base64Util.itemStackArrayToBase64(contents);
            guardarMochilaSync(p.getUniqueId().toString(), id, base64Data);
        });
    }

    public void guardarMochilaSync(String uuid, int id, String base64Data) {
        NexoCore nexoCore = (NexoCore) Bukkit.getPluginManager().getPlugin("NexoCore");
        if (nexoCore == null || nexoCore.getDatabaseManager() == null) return;

        String sql = "INSERT INTO mochilas (uuid, mochila_id, contenido) VALUES (?, ?, ?) ON CONFLICT (uuid, mochila_id) DO UPDATE SET contenido = EXCLUDED.contenido;";
        try (Connection conn = nexoCore.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid);
            ps.setInt(2, id);
            ps.setString(3, base64Data);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}