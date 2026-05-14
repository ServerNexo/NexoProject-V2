package me.aeroxis.crates.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.core.crossplay.CrossplayUtils;
import me.aeroxis.crates.config.ConfigManager;
import me.aeroxis.crates.managers.CrateManager;
import me.aeroxis.crates.menus.CardRevealMenu;
import me.aeroxis.crates.menus.CratesMainMenu;
import me.aeroxis.crates.menus.CratesHistoryMenu; // 🌟 IMPORT NUEVO
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Default;
import revxrsal.commands.annotation.DefaultFor;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;
import revxrsal.commands.annotation.AutoComplete;

@Singleton
@Command({"crates", "caja", "cajas"})
public class ComandoCrates {

    private final CrateManager crateManager;
    private final CardRevealMenu cardRevealMenu;
    private final CratesMainMenu cratesMainMenu;
    private final CratesHistoryMenu cratesHistoryMenu; // 🌟 NUEVO
    private final CrossplayUtils crossplayUtils;
    private final ConfigManager configManager;
    private final String PREFIX = "&#FFD700<bold>Aeroxis</bold> <dark_gray>»</dark_gray> ";

    @Inject
    public ComandoCrates(CrateManager crateManager, CardRevealMenu cardRevealMenu, CratesMainMenu cratesMainMenu, CratesHistoryMenu cratesHistoryMenu, CrossplayUtils crossplayUtils, ConfigManager configManager) {
        this.crateManager = crateManager;
        this.cardRevealMenu = cardRevealMenu;
        this.cratesMainMenu = cratesMainMenu;
        this.cratesHistoryMenu = cratesHistoryMenu; // 🌟 INYECTADO
        this.crossplayUtils = crossplayUtils;
        this.configManager = configManager;
    }

    @DefaultFor({"~"})
    public void openMenu(Player player) {
        cratesMainMenu.open(player);
    }

    @Subcommand("open")
    @AutoComplete("@crates")
    public void openCrate(Player player, @Default("divina") String crateId) {
        cardRevealMenu.open(player, crateId);
    }

    // 🌟 COMANDO DE HISTORIAL (TRANSPARENCIA)
    @Subcommand({"history", "historial"})
    public void openHistory(Player player) {
        cratesHistoryMenu.open(player);
    }

    @Subcommand("givekey")
    @CommandPermission("nexocrates.admin")
    @AutoComplete("* @crates *")
    public void giveKey(Player sender, Player target, String crateId, @Default("1") int amount) {
        crateManager.addKeys(target.getUniqueId(), crateId, amount);

        crossplayUtils.sendMessage(sender, PREFIX + "&#55FF55Has enviado " + amount + " llave(s) de '" + crateId + "' a " + target.getName());
        crossplayUtils.sendMessage(target, PREFIX + "&#FF55FF✨ ¡Has recibido " + amount + " llave(s) para la caja " + crateId + "!");
        crossplayUtils.sendMessage(target, "&#AAAAAAUsa &#FFFFFF/crates &#AAAAAApara usarla.");
    }

    @Subcommand("keyall")
    @CommandPermission("aeroxiscrates.admin")
    @AutoComplete("@crates *")
    public void keyAll(Player sender, String crateId, @Default("1") int amount) {
        int count = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            crateManager.addKeys(p.getUniqueId(), crateId, amount);
            crossplayUtils.sendMessage(p, PREFIX + "&#FF55FF🎁 ¡" + sender.getName() + " ha regalado " + amount + " llave(s) de " + crateId + " a todo el servidor!");
            crossplayUtils.sendMessage(p, "&#AAAAAAUsa &#FFFFFF/crates &#AAAAAApara probar tu suerte.");
            count++;
        }
        crossplayUtils.sendMessage(sender, PREFIX + "&#55FF55Has entregado llaves a " + count + " jugador(es) en línea.");
    }

    @Subcommand("keys")
    @AutoComplete("@crates")
    public void checkKeys(Player player, @Default("divina") String crateId) {
        crateManager.getKeys(player.getUniqueId(), crateId).thenAccept(keys -> {
            crossplayUtils.sendMessage(player, PREFIX + "&#FFFFFFTienes &#00AAFF" + keys + " &#FFFFFFllaves para el banner: &#FFD700" + crateId);
        });
    }

    @Subcommand("pity")
    @AutoComplete("@crates")
    public void checkPity(Player player, @Default("divina") String crateId) {
        crateManager.getPity(player.getUniqueId(), crateId).thenAccept(pity -> {
            int remaining = 50 - pity;
            crossplayUtils.sendMessage(player, PREFIX + "&#FFFFFFPity Actual: &#FF5555" + pity + "/50");
            crossplayUtils.sendMessage(player, "&#AAAAAAEstás a &#55FF55" + remaining + " &#AAAAAAaperturas de un Legendario Garantizado.");
        });
    }

    @Subcommand("reload")
    @CommandPermission("aeroxiscrates.admin")
    public void reloadConfig(Player player) {
        configManager.reloadConfigs();
        crossplayUtils.sendMessage(player, PREFIX + "&#55FF55¡El archivo crates.yml ha sido recargado con éxito!");
    }
}