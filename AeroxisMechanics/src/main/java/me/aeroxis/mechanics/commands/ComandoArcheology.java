package me.aeroxis.mechanics.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.mechanics.AeroxisMechanics;
import me.aeroxis.mechanics.archeology.ArcheologyManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Default;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

@Singleton
@Command({"archeology", "arquelogia"})
@CommandPermission("nexomechanics.admin")
public class ComandoArcheology {

    private final ArcheologyManager archeologyManager;
    private final AeroxisMechanics plugin; // 🌟 VARIABLE AÑADIDA

    @Inject
    public ComandoArcheology(ArcheologyManager archeologyManager, AeroxisMechanics plugin) {
        this.archeologyManager = archeologyManager;
        this.plugin = plugin; // 🌟 INYECTADO POR GUICE
    }

    @Subcommand("test")
    public void poblarZonaTest(Player player, @Default("20") int radio, @Default("5") int cantidad) {
        Location loc = player.getLocation();

        // Ejecutamos el motor de tesoros INVISIBLES (Estilo Hypixel)
        archeologyManager.generateHiddenTreasures(loc, radio, cantidad);

        player.sendMessage("§e[!] §fSe han ocultado §e" + cantidad + " §ftesoros en un radio de " + radio + " bloques.");
        player.sendMessage("§7(Saca tu brújula para rastrearlos y haz clic derecho al bloque para desenterrarlos)");
    }

    @Subcommand("clear")
    public void limpiarZonaTest(Player player) {
        archeologyManager.cleanupAllSpots();
        player.sendMessage("§a[!] §fSe ha vaciado la memoria de tesoros ocultos.");
    }

    // 🌟 NUEVO COMANDO PARA ACTUALIZAR RECOMPENSAS EN VIVO
    @Subcommand("reload")
    public void recargarConfig(Player player) {
        plugin.reloadConfig();
        player.sendMessage("§a[!] §fSe han recargado las recompensas y configuraciones de Arqueología desde el config.yml.");
    }

    @Subcommand("debug")
    public void debugItem(Player player) {
        org.bukkit.inventory.ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() == org.bukkit.Material.AIR || !item.hasItemMeta()) {
            player.sendMessage("§c[!] No tienes un ítem válido en la mano.");
            return;
        }

        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        player.sendMessage("§e=== 🔍 RAYOS X DEL ÍTEM ===");

        // 1. Verificamos cómo lee el nombre internamente
        if (meta.hasDisplayName() && meta.displayName() != null) {
            String plainName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(meta.displayName());
            player.sendMessage("§fNombre Plano: §a" + plainName);
        } else {
            player.sendMessage("§fNombre Plano: §cNinguno");
        }

        // 2. Escaneamos TODOS los NBT (PersistentDataContainer)
        player.sendMessage("§fEtiquetas Ocultas (PDC):");
        java.util.Set<org.bukkit.NamespacedKey> keys = meta.getPersistentDataContainer().getKeys();

        if (keys.isEmpty()) {
            player.sendMessage("§c- No tiene ninguna etiqueta PDC.");
        } else {
            for (org.bukkit.NamespacedKey key : keys) {
                player.sendMessage("§7- §b" + key.getNamespace() + ":" + key.getKey());
            }
        }
        player.sendMessage("§e=======================");
    }
}