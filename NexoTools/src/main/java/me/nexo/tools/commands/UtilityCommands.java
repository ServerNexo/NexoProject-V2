package me.nexo.tools.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.bukkit.annotation.CommandPermission;

@Singleton
public class UtilityCommands {

    private final CrossplayUtils crossplayUtils;

    @Inject
    public UtilityCommands(CrossplayUtils crossplayUtils) {
        this.crossplayUtils = crossplayUtils;
    }

    @Command({"fly"})
    @CommandPermission("nexotools.fly")
    public void fly(Player player) {
        boolean isFlying = !player.getAllowFlight();
        player.setAllowFlight(isFlying);
        player.setFlying(isFlying);
        crossplayUtils.sendMessage(player, isFlying ? "&#55FF55🕊️ Vuelo activado." : "&#FF5555🕊️ Vuelo desactivado.");
    }

    @Command({"heal"})
    @CommandPermission("nexotools.admin")
    public void heal(Player player, @Optional Player target) {
        Player p = target != null ? target : player;
        p.setHealth(p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
        p.setFoodLevel(20);
        crossplayUtils.sendMessage(p, "&#55FF55❤️ Has sido curado.");
    }

    @Command({"gamemode", "gm"})
    @CommandPermission("nexotools.admin")
    public void gamemode(Player player, GameMode mode) {
        player.setGameMode(mode);
        crossplayUtils.sendMessage(player, "&#55FF55🎮 Modo de juego actualizado a " + mode.name() + ".");
    }

    @Command({"trash", "basura"})
    @CommandPermission("nexotools.trash")
    public void trash(Player player) {
        Inventory inv = Bukkit.createInventory(null, 36, Component.text("🗑️ Basurero (Se destruirá)"));
        player.openInventory(inv);
        crossplayUtils.sendMessage(player, "&#AAAAAA[!] Todo lo que dejes aquí será destruido al cerrar.");
    }

    @Command({"hat", "sombrero"})
    @CommandPermission("nexotools.hat")
    public void hat(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            crossplayUtils.sendMessage(player, "&#FF5555❌ Debes tener un ítem en la mano.");
            return;
        }
        ItemStack helmet = player.getInventory().getHelmet();
        player.getInventory().setHelmet(item);
        player.getInventory().setItemInMainHand(helmet);
        crossplayUtils.sendMessage(player, "&#55FF55🎩 ¡Lindo sombrero!");
    }
}