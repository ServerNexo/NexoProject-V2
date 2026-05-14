package me.aeroxis.crates.menus;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.aeroxis.core.crossplay.CrossplayUtils;
import me.aeroxis.crates.AeroxisCrates;
import me.aeroxis.crates.config.ConfigManager;
import me.aeroxis.crates.managers.CrateManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.spongepowered.configurate.ConfigurationNode;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Singleton
public class CratesHistoryMenu {

    private final AeroxisCrates plugin;
    private final CrateManager crateManager;
    private final ConfigManager configManager;
    private final CrossplayUtils crossplayUtils;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    @Inject
    public CratesHistoryMenu(AeroxisCrates plugin, CrateManager crateManager, ConfigManager configManager, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.crateManager = crateManager;
        this.configManager = configManager;
        this.crossplayUtils = crossplayUtils;
    }

    public void open(Player player) {
        crossplayUtils.sendMessage(player, "&#FFAA00[⏳] Desencriptando registros del Nexo...");

        // Solicitamos los últimos 28 registros asíncronamente
        crateManager.getPlayerHistory(player.getUniqueId(), 28).thenAccept(history -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Gui gui = Gui.gui()
                        .title(Component.text("📜 Historial de Aperturas"))
                        .rows(5)
                        .disableAllInteractions()
                        .create();

                GuiItem filler = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, Component.empty(), null);
                gui.getFiller().fillBorder(filler); // Llenamos solo los bordes

                if (history.isEmpty()) {
                    GuiItem empty = createGuiItem(Material.BARRIER, 
                            crossplayUtils.parseCrossplay(player, "&#FF5555Sin registros"), 
                            List.of(crossplayUtils.parseCrossplay(player, "&#AAAAAAAún no has abierto ninguna caja.")));
                    gui.setItem(22, empty);
                } else {
                    ConfigurationNode cratesNode = configManager.getCratesNode().node("crates");
                    
                    for (CrateManager.CrateHistoryEntry entry : history) {
                        ConfigurationNode bannerNode = cratesNode.node(entry.crateId());
                        ConfigurationNode rewardNode = bannerNode.node("rewards", entry.rewardId());

                        // Cruzamos los datos SQL con el archivo YML
                        String bannerName = bannerNode.virtual() ? "&#FFD700Banner " + entry.crateId() : bannerNode.node("name").getString("Banner");
                        String rewardName = rewardNode.virtual() ? entry.rewardId() : rewardNode.node("name").getString("Premio Desconocido");
                        Material mat = Material.matchMaterial(rewardNode.virtual() ? "PAPER" : rewardNode.node("material").getString("PAPER"));
                        if (mat == null) mat = Material.PAPER;

                        String dateStr = dateFormat.format(new Date(entry.timestamp()));

                        List<Component> lore = new ArrayList<>();
                        lore.add(crossplayUtils.parseCrossplay(player, "&#AAAAAABanner: " + bannerName));
                        lore.add(crossplayUtils.parseCrossplay(player, "&#AAAAAAFecha: &#E6CCFF" + dateStr));

                        GuiItem historyItem = createGuiItem(mat, crossplayUtils.parseCrossplay(player, rewardName), lore);
                        gui.addItem(historyItem); // Los coloca en orden en el centro libre
                    }
                }

                gui.open(player);
            });
        });
    }

    private GuiItem createGuiItem(Material material, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (name != null) meta.displayName(name);
            if (lore != null) meta.lore(lore);
            item.setItemMeta(meta);
        }
        return new GuiItem(item);
    }
}