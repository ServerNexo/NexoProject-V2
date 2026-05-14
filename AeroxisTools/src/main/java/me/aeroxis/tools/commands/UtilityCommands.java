package me.aeroxis.tools.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.core.crossplay.CrossplayUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Default;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.bukkit.annotation.CommandPermission;

@Singleton
public class UtilityCommands {

    private final CrossplayUtils crossplayUtils;

    // 🌟 Diseño Visual unificado para esta categoría
    private final String PREFIX = "&#00AAFF<bold>NEXO</bold> <dark_gray>»</dark_gray> ";

    @Inject
    public UtilityCommands(CrossplayUtils crossplayUtils) {
        this.crossplayUtils = crossplayUtils;
    }

    // ==========================================
    // 🕊️ SISTEMA DE VUELO
    // ==========================================

    @Command({"fly"})
    @CommandPermission("nexotools.fly")
    public void fly(Player player) {
        boolean isFlying = !player.getAllowFlight();
        player.setAllowFlight(isFlying);
        player.setFlying(isFlying);

        String status = isFlying ? "&#55FF55Activado" : "&#FF5555Desactivado";
        crossplayUtils.sendMessage(player, PREFIX + "&#FFFFFFMotor de vuelo: " + status);
    }

    @Command({"flyspeed", "fspeed"})
    @CommandPermission("nexotools.flyspeed")
    public void flyspeed(Player player, @Default("1") float speed) {
        // Límite de seguridad
        if (speed < 0 || speed > 10) {
            crossplayUtils.sendMessage(player, PREFIX + "&#FF5555❌ La velocidad táctica debe estar entre 0 y 10.");
            return;
        }

        // Bukkit usa floats de 0.0f a 1.0f (0.1f es la velocidad normal)
        float realSpeed = speed / 10f;
        player.setFlySpeed(realSpeed);

        crossplayUtils.sendMessage(player, PREFIX + "&#FFFFFFVelocidad de vuelo calibrada a &#00AAFF" + speed + "&#FFFFFF.");
    }

    // ==========================================
    // ❤️ CURACIÓN Y MODOS
    // ==========================================

    @Command({"heal"})
    @CommandPermission("nexotools.admin")
    public void heal(Player player, @Optional Player target) {
        Player p = target != null ? target : player;

        // 🌟 FIX: Usamos GENERIC_MAX_HEALTH que es el estándar moderno en Java 21 / Paper 1.21+
        p.setHealth(p.getAttribute(Attribute.MAX_HEALTH).getValue());
        p.setFoodLevel(20);
        p.setSaturation(20f); // Le damos saturación extra

        crossplayUtils.sendMessage(p, PREFIX + "&#55FF55❤️ Vitalidad y saturación restauradas al 100%.");

        // Feedback extra si curaste a otra persona
        if (target != null && !target.equals(player)) {
            crossplayUtils.sendMessage(player, PREFIX + "&#FFFFFFHas curado a &#00AAFF" + p.getName() + "&#FFFFFF.");
        }
    }

    @Command({"gamemode", "gm"})
    @CommandPermission("nexotools.admin")
    public void gamemode(Player player, GameMode mode) {
        player.setGameMode(mode);
        // Hacemos el texto más bonito (ej: SURVIVAL -> Survival)
        String modeName = mode.name().substring(0, 1).toUpperCase() + mode.name().substring(1).toLowerCase();

        crossplayUtils.sendMessage(player, PREFIX + "&#FFFFFFProtocolo de simulación: &#FFD700" + modeName);
    }

    // ==========================================
    // 🧰 UTILIDADES DE INVENTARIO
    // ==========================================

    @Command({"trash", "basura"})
    @CommandPermission("nexotools.trash")
    public void trash(Player player) {
        // Menú con componentes modernos
        Component title = crossplayUtils.parseCrossplay(null, "<dark_gray>» <red><bold>INCINERADOR DE MATERIA</bold></red> «");
        Inventory inv = Bukkit.createInventory(null, 36, title);

        player.openInventory(inv);
        crossplayUtils.sendMessage(player, PREFIX + "&#FF5555⚠️ Advertencia: &#AAAAAATodo lo depositado aquí será obliterado al cerrar.");
    }

    @Command({"hat", "sombrero"})
    @CommandPermission("nexotools.hat")
    public void hat(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() == Material.AIR) {
            crossplayUtils.sendMessage(player, PREFIX + "&#FF5555❌ Debes sostener un objeto físico en tu mano principal.");
            return;
        }

        // Hacemos el intercambio de cascos
        ItemStack helmet = player.getInventory().getHelmet();
        player.getInventory().setHelmet(item);
        player.getInventory().setItemInMainHand(helmet);

        crossplayUtils.sendMessage(player, PREFIX + "&#FF55FF✨ Cosmético equipado en la cabeza exitosamente.");
    }
}