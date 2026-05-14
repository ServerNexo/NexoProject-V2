package me.aeroxis.minions.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.nexomc.nexo.api.NexoItems;
import me.aeroxis.core.crossplay.CrossplayUtils;
import me.aeroxis.minions.config.ConfigManager;
import me.aeroxis.minions.data.MinionDNA;
import me.aeroxis.minions.data.MinionKeys;
import me.aeroxis.minions.data.TiersConfig;
import me.aeroxis.minions.data.UpgradesConfig;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Default;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 🤖 AeroxisMinions - Comando Principal (Arquitectura Enterprise Fase 3)
 * Rendimiento: Cero dependencias estáticas, I/O inyectado y Omni-Minion adaptado.
 */
@Singleton
@Command({"minion", "minions"})
@CommandPermission("nexominions.admin")
public class ComandoMinion {

    private final ConfigManager configManager;
    private final TiersConfig tiersConfig;
    private final UpgradesConfig upgradesConfig;
    private final CrossplayUtils crossplayUtils;

    // 💉 PILAR 1: Inyección Directa
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
        configManager.reloadMessages();
        tiersConfig.cargarConfig();
        upgradesConfig.cargarConfig();
        crossplayUtils.sendMessage(player, configManager.getMessages().comandos().reloadExito());
    }

    // 🌟 FASE 3: El Omni-Minion. Ya no pedimos el 'MinionType' en el comando.
    @Subcommand("give")
    public void giveMinion(Player sender, Player target, @Default("1") int tier) {
        if (target == null) {
            crossplayUtils.sendMessage(sender, configManager.getMessages().comandos().jugadorOffline());
            return;
        }

        if (tier < 1 || tier > 12) {
            crossplayUtils.sendMessage(sender, configManager.getMessages().comandos().nivelInvalido());
            return;
        }

        // 🌟 IMPORTANTE: Reemplaza "omni_minion" por la ID del ítem (huevo) que uses en el config de Nexo.
        var itemFactory = NexoItems.itemFromId("omni_minion");
        if (itemFactory == null) {
            crossplayUtils.sendMessage(sender, "&#FF5555[x] El ID 'omni_minion' no existe en NexoItems. Verifica tu Resource Pack.");
            return;
        }

        var minionItem = itemFactory.build();

        if (minionItem == null || minionItem.isEmpty()) {
            crossplayUtils.sendMessage(sender, configManager.getMessages().comandos().materiaVacia());
            return;
        }

        var meta = minionItem.getItemMeta();
        if (meta != null) {
            String nombre = configManager.getMessages().comandos().itemNombre()
                    .replace("%type%", "Omni-Minion")
                    .replace("%tier%", String.valueOf(tier));

            meta.displayName(crossplayUtils.parseCrossplay(null, nombre));

            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            for (String line : configManager.getMessages().comandos().itemLore()) {
                lore.add(crossplayUtils.parseCrossplay(null, line));
            }
            meta.lore(lore);

            // =======================================================
            // 🧬 INYECCIÓN DE ADN FASE 3 (OMNI-MINION)
            // Por defecto, nacen farmeando COBBLESTONE.
            // =======================================================
            MinionDNA unplacedDna = MinionDNA.createBase(new UUID(0, 0), "COBBLESTONE", tier);
            meta.getPersistentDataContainer().set(MinionKeys.DNA_KEY, MinionKeys.DNA_TYPE, unplacedDna);

            minionItem.setItemMeta(meta);
        } else {
            crossplayUtils.sendMessage(sender, configManager.getMessages().comandos().falloNbt());
            return;
        }

        target.getInventory().addItem(minionItem);
        crossplayUtils.sendMessage(sender, configManager.getMessages().comandos().invocacionAprobada()
                .replace("%type%", "Omni-Minion")
                .replace("%tier%", String.valueOf(tier))
                .replace("%target%", target.getName()));
        crossplayUtils.sendMessage(target, configManager.getMessages().comandos().pactoForjado());
    }
}