package me.nexo.minions.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.nexomc.nexo.api.NexoItems;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.minions.config.ConfigManager;
import me.nexo.minions.data.MinionKeys;
import me.nexo.minions.data.MinionType;
import me.nexo.minions.data.TiersConfig;
import me.nexo.minions.data.UpgradesConfig;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Default;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.util.ArrayList;
import java.util.List;

/**
 * 🤖 NexoMinions - Comando Principal (Arquitectura Enterprise)
 * Rendimiento: Cero dependencias estáticas, I/O inyectado y Compatibilidad Crossplay.
 * Nota: Lamp (Revxrsal) inyecta este comando nativamente en el CommandMap.
 */
@Singleton
@Command({"minion", "minions"})
@CommandPermission("nexominions.admin")
public class ComandoMinion {

    private final ConfigManager configManager;
    private final TiersConfig tiersConfig;
    private final UpgradesConfig upgradesConfig;
    private final CrossplayUtils crossplayUtils; // 🌟 Sinergia inyectada

    // 💉 PILAR 1: Inyección Directa (Se elimina NexoMinions plugin, ya no hace falta)
    @Inject
    public ComandoMinion(ConfigManager configManager, TiersConfig tiersConfig,
                         UpgradesConfig upgradesConfig, CrossplayUtils crossplayUtils) {
        this.configManager = configManager;
        this.tiersConfig = tiersConfig;
        this.upgradesConfig = upgradesConfig;
        this.crossplayUtils = crossplayUtils;
    }

    @Subcommand("reload")
    public void reload(Player player) {
        // 🌟 Recarga limpia usando las dependencias inyectadas (Sin getters legacy)
        configManager.reloadMessages();
        tiersConfig.cargarConfig();
        upgradesConfig.cargarConfig();
        crossplayUtils.sendMessage(player, configManager.getMessages().comandos().reloadExito());
    }

    @Subcommand("give")
    public void giveMinion(Player sender, Player target, MinionType type, @Default("1") int tier) {
        if (target == null) {
            crossplayUtils.sendMessage(sender, configManager.getMessages().comandos().jugadorOffline());
            return;
        }

        if (tier < 1 || tier > 12) {
            crossplayUtils.sendMessage(sender, configManager.getMessages().comandos().nivelInvalido());
            return;
        }

        var itemFactory = NexoItems.itemFromId(type.getNexoModelID());
        if (itemFactory == null) {
            crossplayUtils.sendMessage(sender, configManager.getMessages().comandos().selloNoExiste().replace("%id%", type.getNexoModelID()));
            return;
        }

        var minionItem = itemFactory.build();

        // 🌟 PAPER 1.21 FIX: isEmpty() previene bugs de stacks fantasmas
        if (minionItem == null || minionItem.isEmpty()) {
            crossplayUtils.sendMessage(sender, configManager.getMessages().comandos().materiaVacia());
            return;
        }

        var meta = minionItem.getItemMeta();
        if (meta != null) {
            String nombre = configManager.getMessages().comandos().itemNombre()
                    .replace("%type%", type.getDisplayName())
                    .replace("%tier%", String.valueOf(tier));

            // 🌟 FIX: Uso de CrossplayUtils en vez de serializers estáticos
            meta.displayName(crossplayUtils.parseCrossplay(null, nombre));

            // 🌟 FIX ERROR toList(): Bucle for nativo, 100% a prueba de errores de compilador
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            for (String line : configManager.getMessages().comandos().itemLore()) {
                lore.add(crossplayUtils.parseCrossplay(null, line));
            }
            meta.lore(lore);

            meta.getPersistentDataContainer().set(MinionKeys.TYPE, PersistentDataType.STRING, type.name());
            meta.getPersistentDataContainer().set(MinionKeys.TIER, PersistentDataType.INTEGER, tier);

            minionItem.setItemMeta(meta);
        } else {
            crossplayUtils.sendMessage(sender, configManager.getMessages().comandos().falloNbt());
            return;
        }

        target.getInventory().addItem(minionItem);
        crossplayUtils.sendMessage(sender, configManager.getMessages().comandos().invocacionAprobada()
                .replace("%type%", type.getDisplayName())
                .replace("%tier%", String.valueOf(tier))
                .replace("%target%", target.getName()));
        crossplayUtils.sendMessage(target, configManager.getMessages().comandos().pactoForjado());
    }
}