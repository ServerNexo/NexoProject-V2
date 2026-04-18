package me.nexo.minions.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.nexomc.nexo.api.NexoItems;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.minions.NexoMinions;
import me.nexo.minions.config.ConfigManager;
import me.nexo.minions.data.MinionKeys;
import me.nexo.minions.data.MinionType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Default;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.util.List;
import java.util.stream.Collectors; // 🌟 IMPORTACIÓN AÑADIDA

/**
 * 🤖 NexoMinions - Comando Principal (Arquitectura Enterprise)
 * Lamp auto-completa <Jugador> y <Tipo> y <Nivel> por nosotros.
 */
@Singleton
@Command({"minion", "minions"})
@CommandPermission("nexominions.admin")
public class ComandoMinion {

    private final NexoMinions plugin;
    private final ConfigManager configManager;

    @Inject
    public ComandoMinion(NexoMinions plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    @Subcommand("reload")
    public void reload(Player player) {
        configManager.reloadMessages();
        plugin.getTiersConfig().cargarConfig();
        plugin.getUpgradesConfig().cargarConfig();
        CrossplayUtils.sendMessage(player, configManager.getMessages().comandos().reloadExito());
    }

    @Subcommand("give")
    public void giveMinion(Player sender, Player target, MinionType type, @Default("1") int tier) {
        if (target == null) {
            CrossplayUtils.sendMessage(sender, configManager.getMessages().comandos().jugadorOffline());
            return;
        }

        if (tier < 1 || tier > 12) {
            CrossplayUtils.sendMessage(sender, configManager.getMessages().comandos().nivelInvalido());
            return;
        }

        var itemFactory = NexoItems.itemFromId(type.getNexoModelID());
        if (itemFactory == null) {
            CrossplayUtils.sendMessage(sender, configManager.getMessages().comandos().selloNoExiste().replace("%id%", type.getNexoModelID()));
            return;
        }

        ItemStack minionItem = itemFactory.build();

        if (minionItem == null || minionItem.getType().isAir()) {
            CrossplayUtils.sendMessage(sender, configManager.getMessages().comandos().materiaVacia());
            return;
        }

        ItemMeta meta = minionItem.getItemMeta();
        if (meta != null) {
            String nombre = configManager.getMessages().comandos().itemNombre()
                    .replace("%type%", type.getDisplayName())
                    .replace("%tier%", String.valueOf(tier));

            meta.displayName(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(nombre));

            // 🌟 CORRECCIÓN APLICADA AQUÍ: Usamos Collectors.toList() para compatibilidad absoluta
            List<net.kyori.adventure.text.Component> lore = configManager.getMessages().comandos().itemLore().stream()
                    .map(line -> net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(line))
                    .collect(Collectors.toList());
            meta.lore(lore);

            meta.getPersistentDataContainer().set(MinionKeys.TYPE, PersistentDataType.STRING, type.name());
            meta.getPersistentDataContainer().set(MinionKeys.TIER, PersistentDataType.INTEGER, tier);

            minionItem.setItemMeta(meta);
        } else {
            CrossplayUtils.sendMessage(sender, configManager.getMessages().comandos().falloNbt());
            return;
        }

        target.getInventory().addItem(minionItem);
        CrossplayUtils.sendMessage(sender, configManager.getMessages().comandos().invocacionAprobada()
                .replace("%type%", type.getDisplayName())
                .replace("%tier%", String.valueOf(tier))
                .replace("%target%", target.getName()));
        CrossplayUtils.sendMessage(target, configManager.getMessages().comandos().pactoForjado());
    }
}