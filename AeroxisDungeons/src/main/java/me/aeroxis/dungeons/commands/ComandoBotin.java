package me.aeroxis.dungeons.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.core.crossplay.CrossplayUtils;
import me.aeroxis.dungeons.mechanics.AbyssBackpackManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.util.List;

/**
 * 🎒 AeroxisDungeons - Visor de Botín de Sesión
 * Comando totalmente aislado de NexoItems (/mochila) para evitar choques en Lamp.
 */
@Singleton
@Command({"botin", "saqueo", "abyssloot"}) // Nombres alternativos seguros
@CommandPermission("aeroxis.dungeons.player")
public class ComandoBotin {

    private final AbyssBackpackManager backpackManager;
    private final CrossplayUtils crossplayUtils;

    @Inject
    public ComandoBotin(AbyssBackpackManager backpackManager, CrossplayUtils crossplayUtils) {
        this.backpackManager = backpackManager;
        this.crossplayUtils = crossplayUtils;
    }

    public void ejecutar(Player player) {
        String worldName = player.getWorld().getName().toLowerCase();
        
        // Solo pueden abrirlo dentro del Abismo
        if (!worldName.startsWith("dungeon_") && !worldName.startsWith("inst_")) {
            crossplayUtils.sendMessage(player, "&#FF5555[!] El botín de sesión solo se puede ver dentro de El Abismo.");
            return;
        }

        List<ItemStack> loot = backpackManager.viewLoot(player.getUniqueId());

        if (loot.isEmpty()) {
            crossplayUtils.sendMessage(player, "&#FFaaaaTu bolsa de botín está vacía. ¡Mata monstruos o mina recursos!");
            return;
        }

        // Creamos una GUI de 54 slots (Solo lectura)
        var title = MiniMessage.miniMessage().deserialize("<#9146FF><bold>🎒 BOTÍN EXTRAÍBLE</bold></color>");
        Inventory gui = Bukkit.createInventory(player, 54, title);

        // Llenamos la GUI con los ítems de la RAM
        for (int i = 0; i < Math.min(loot.size(), 54); i++) {
            gui.setItem(i, loot.get(i));
        }

        player.openInventory(gui);
        player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f);
        crossplayUtils.sendMessage(player, "&#E6CCFF[!] Esto es una vista previa. Debes extraer con vida para llevarlos a tu inventario real.");
    }
}