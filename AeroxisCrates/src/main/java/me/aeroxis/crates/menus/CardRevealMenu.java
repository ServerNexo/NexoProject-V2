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
import org.bukkit.scheduler.BukkitRunnable;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean; // 🌟 IMPORT NUEVO ANTI-BUGS

@Singleton
public class CardRevealMenu {

    private final AeroxisCrates plugin;
    private final CrateManager crateManager;
    private final ConfigManager configManager;
    private final CrossplayUtils crossplayUtils;
    private final Random random = new Random();

    @Inject
    public CardRevealMenu(AeroxisCrates plugin, CrateManager crateManager, ConfigManager configManager, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.crateManager = crateManager;
        this.configManager = configManager;
        this.crossplayUtils = crossplayUtils;
    }

    public void open(Player player, String crateId) {
        ConfigurationNode crateNode = configManager.getCratesNode().node("crates", crateId);
        if (crateNode.virtual()) {
            crossplayUtils.sendMessage(player, "&#FF5555❌ Este banner ya no está disponible.");
            return;
        }

        crateManager.getKeys(player.getUniqueId(), crateId).thenAccept(keys -> {
            if (keys <= 0) {
                crossplayUtils.sendMessage(player, "&#FF5555❌ No tienes llaves para el Banner: " + crateNode.node("name").getString("Desconocido"));
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                Gui gui = Gui.gui()
                        .title(Component.text("🃏 Revela tu Destino"))
                        .rows(3)
                        .disableAllInteractions()
                        .create();

                // 🌟 FIX ANTI-EXPLOITS: Interruptor de un solo uso
                AtomicBoolean hasClicked = new AtomicBoolean(false);

                GuiItem filler = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, Component.empty(), null);
                gui.getFiller().fill(filler);

                int[] cardSlots = {11, 13, 15};

                for (int slot : cardSlots) {
                    GuiItem hiddenCard = createGuiItem(Material.PAPER,
                            crossplayUtils.parseCrossplay(player, "&#FFD700✨ Carta Misteriosa ✨"),
                            List.of(crossplayUtils.parseCrossplay(player, "&#AAAAAA¡Haz click para revelar tu premio!")));

                    hiddenCard.setAction(event -> {
                        // 🛡️ BLOQUEO ABSOLUTO: Si ya hizo clic, rechazamos cualquier clic adicional en el acto.
                        if (hasClicked.getAndSet(true)) {
                            return;
                        }

                        gui.setDefaultClickAction(e -> e.setCancelled(true));
                        processCardReveal(player, gui, slot, crateId, crateNode, cardSlots);
                    });

                    gui.updateItem(slot, hiddenCard);
                }

                gui.open(player);
            });
        });
    }

    private void processCardReveal(Player player, Gui gui, int clickedSlot, String crateId, ConfigurationNode crateNode, int[] allSlots) {
        crateManager.removeKey(player.getUniqueId(), crateId);

        crateManager.getPity(player.getUniqueId(), crateId).thenAccept(pity -> {
            boolean triggerLegendary = false;
            int newPity = pity + 1;

            if (newPity >= 50) {
                triggerLegendary = true;
                crateManager.resetPity(player.getUniqueId(), crateId);
            } else {
                double chance = 0.01;
                if (newPity > 40) chance += (newPity - 40) * 0.05;
                if (random.nextDouble() < chance) {
                    triggerLegendary = true;
                    crateManager.resetPity(player.getUniqueId(), crateId);
                } else {
                    crateManager.incrementPity(player.getUniqueId(), crateId);
                }
            }

            ConfigurationNode selectedReward = getRandomReward(crateNode.node("rewards"), triggerLegendary);

            if (selectedReward == null) {
                player.sendMessage("§cError interno: No hay premios configurados.");
                return;
            }

            String rewardId = selectedReward.key().toString();
            String rewardName = selectedReward.node("name").getString("&7Premio");
            String command = selectedReward.node("command").getString("");
            boolean broadcast = selectedReward.node("broadcast").getBoolean(false);

            Material rawMat = Material.matchMaterial(selectedReward.node("material").getString("STONE"));
            final Material rewardMat = (rawMat != null) ? rawMat : Material.STONE;

            crateManager.logHistory(player, crateId, rewardId);

            final boolean finalTriggerLegendary = triggerLegendary;
            final String finalCrateName = crateNode.node("name").getString("Banner");

            Bukkit.getScheduler().runTask(plugin, () -> {
                playSuspenseAnimation(player, gui, clickedSlot, allSlots, rewardMat, rewardName, command, broadcast, finalTriggerLegendary, finalCrateName, crateId);
            });
        });
    }

    // 🎬 ANIMACIÓN CINEMÁTICA AAA CON GACHA LOOP
    private void playSuspenseAnimation(Player player, Gui gui, int clickedSlot, int[] allSlots, Material rewardMat, String rewardName, String command, boolean broadcast, boolean isLegendary, String crateName, String crateId) {

        for (int s : allSlots) {
            if (s != clickedSlot) {
                GuiItem lostItem = createGuiItem(Material.COAL, crossplayUtils.parseCrossplay(player, "&#555555(Descartada)"), null);
                gui.updateItem(s, lostItem);
            }
        }

        Material[] tensionColors = {Material.WHITE_STAINED_GLASS_PANE, Material.YELLOW_STAINED_GLASS_PANE, Material.MAGENTA_STAINED_GLASS_PANE};

        new BukkitRunnable() {
            int ticks = 0;
            float pitch = 0.5f;

            @Override
            public void run() {
                if (!player.isOnline() || !player.getOpenInventory().getTopInventory().equals(gui.getInventory())) {
                    deliverReward(player, rewardName, command, broadcast, isLegendary, crateName);
                    this.cancel();
                    return;
                }

                if (ticks < 15) {
                    if (ticks % 3 == 0) {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, pitch);
                        pitch += 0.1f;
                        Material color = tensionColors[random.nextInt(tensionColors.length)];
                        GuiItem revealingItem = createGuiItem(color, Component.text("§k||| §r§e¡Revelando! §k|||"), null);
                        gui.updateItem(clickedSlot, revealingItem);
                    }
                }
                else if (ticks == 15) {
                    player.playSound(player.getLocation(), isLegendary ? Sound.ENTITY_LIGHTNING_BOLT_THUNDER : Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);

                    if (isLegendary) {
                        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                    }

                    GuiItem revealed = createGuiItem(rewardMat,
                            crossplayUtils.parseCrossplay(player, rewardName),
                            List.of(crossplayUtils.parseCrossplay(player, "&#55FF55¡Premio enviado!")));
                    gui.updateItem(clickedSlot, revealed);

                    deliverReward(player, rewardName, command, broadcast, isLegendary, crateName);
                }
                else if (ticks >= 45) {
                    this.cancel();

                    crateManager.getKeys(player.getUniqueId(), crateId).thenAccept(keysLeft -> {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (keysLeft > 0) {
                                open(player, crateId);
                            } else {
                                player.performCommand("crates");
                            }
                        });
                    });
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void deliverReward(Player player, String rewardName, String command, boolean broadcast, boolean isLegendary, String crateName) {
        if (!command.isEmpty()) {
            String parsedCommand = command.replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsedCommand);
        }

        if (broadcast && isLegendary) {
            Bukkit.broadcast(crossplayUtils.parseCrossplay(null,
                    "&#FFD700<bold>NEXO</bold> <dark_gray>»</dark_gray> &#00AAFF" + player.getName() +
                            " &#FFFFFFha obtenido " + rewardName + " &#FFFFFFen el " + crateName + "!"));
        }
    }

    private ConfigurationNode getRandomReward(ConfigurationNode rewardsNode, boolean forceLegendary) {
        List<ConfigurationNode> possibleRewards = new ArrayList<>();
        int totalWeight = 0;

        for (Map.Entry<Object, ? extends ConfigurationNode> entry : rewardsNode.childrenMap().entrySet()) {
            ConfigurationNode node = entry.getValue();
            boolean isLegendary = node.node("is_legendary").getBoolean(false);

            if (forceLegendary && !isLegendary) continue;

            possibleRewards.add(node);
            totalWeight += node.node("weight").getInt(1);
        }

        if (possibleRewards.isEmpty() && forceLegendary) {
            return getRandomReward(rewardsNode, false);
        }

        if (possibleRewards.isEmpty()) return null;

        int randomPoint = random.nextInt(totalWeight);
        int currentWeight = 0;

        for (ConfigurationNode node : possibleRewards) {
            currentWeight += node.node("weight").getInt(1);
            if (randomPoint < currentWeight) {
                return node;
            }
        }
        return possibleRewards.get(possibleRewards.size() - 1);
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