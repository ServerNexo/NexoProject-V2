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
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Singleton
public class CratesMainMenu {

    private final AeroxisCrates plugin;
    private final CrateManager crateManager;
    private final ConfigManager configManager;
    private final CrossplayUtils crossplayUtils;
    private final CardRevealMenu cardRevealMenu;

    @Inject
    public CratesMainMenu(AeroxisCrates plugin, CrateManager crateManager, ConfigManager configManager, CrossplayUtils crossplayUtils, CardRevealMenu cardRevealMenu) {
        this.plugin = plugin;
        this.crateManager = crateManager;
        this.configManager = configManager;
        this.crossplayUtils = crossplayUtils;
        this.cardRevealMenu = cardRevealMenu;
    }

    public void open(Player player) {
        ConfigurationNode cratesNode = configManager.getCratesNode().node("crates");
        
        if (cratesNode.virtual() || cratesNode.childrenMap().isEmpty()) {
            crossplayUtils.sendMessage(player, "&#FF5555❌ No hay banners configurados en el servidor.");
            return;
        }

        // 🌟 HILO ASÍNCRONO: Consultamos la base de datos para todas las cajas sin dar lag
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Gui gui = Gui.gui()
                    .title(Component.text("🌟 Banners de Aeroxis"))
                    .rows(3)
                    .disableAllInteractions()
                    .create();

            GuiItem filler = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, Component.empty(), null);
            gui.getFiller().fill(filler);

            int slot = 11; // Empezamos a centrar los banners en la fila 2

            for (Map.Entry<Object, ? extends ConfigurationNode> entry : cratesNode.childrenMap().entrySet()) {
                String crateId = entry.getKey().toString();
                ConfigurationNode node = entry.getValue();

                String name = node.node("name").getString("&#FFD700" + crateId);
                Material mat = Material.matchMaterial(node.node("material").getString("CHEST"));
                if (mat == null) mat = Material.CHEST;

                // Bloqueamos el hilo asíncrono para obtener los datos de este jugador (.join())
                int keys = crateManager.getKeys(player.getUniqueId(), crateId).join();
                int pity = crateManager.getPity(player.getUniqueId(), crateId).join();

                List<Component> lore = new ArrayList<>();
                lore.add(Component.empty());
                lore.add(crossplayUtils.parseCrossplay(player, "&#AAAAAALlaves en billetera: &#00AAFF" + keys));
                lore.add(crossplayUtils.parseCrossplay(player, "&#AAAAAALegendario en: &#FF5555" + pity + "/50"));
                lore.add(Component.empty());
                
                if (keys > 0) {
                    lore.add(crossplayUtils.parseCrossplay(player, "&#55FF55▶ Haz clic para abrir el banner"));
                } else {
                    lore.add(crossplayUtils.parseCrossplay(player, "&#FF5555▶ No tienes llaves suficientes"));
                }

                GuiItem bannerItem = createGuiItem(mat, crossplayUtils.parseCrossplay(player, name), lore);
                
                bannerItem.setAction(event -> {
                    if (keys > 0) {
                        cardRevealMenu.open(player, crateId);
                    } else {
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    }
                });

                if (slot > 15) break; // Límite visual por ahora (3-5 banners)
                gui.setItem(slot++, bannerItem);
            }

            // 🌟 VOLVEMOS AL HILO PRINCIPAL: Abrimos el menú al jugador
            Bukkit.getScheduler().runTask(plugin, () -> gui.open(player));
        });
    }

    // FIX NATIVO: Creador de ítems seguro para Paper 1.21+
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