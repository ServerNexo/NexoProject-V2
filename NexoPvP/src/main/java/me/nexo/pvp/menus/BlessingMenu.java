package me.nexo.pvp.menus;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import me.nexo.core.user.NexoUser;
import me.nexo.core.user.UserManager;
import me.nexo.core.user.UserRepository;
import me.nexo.economy.NexoEconomy;
import me.nexo.economy.core.NexoAccount;
import me.nexo.pvp.NexoPvP;
import me.nexo.pvp.config.ConfigManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public class BlessingMenu extends NexoMenu {

    // 💉 PILAR 3: Dependencias inyectadas desde el comando
    private final ConfigManager configManager;
    private final UserManager userManager;
    private final UserRepository userRepository;
    private final NexoPvP plugin;

    public BlessingMenu(Player player, ConfigManager configManager, UserManager userManager, UserRepository userRepository, NexoPvP plugin) {
        super(player);
        this.configManager = configManager;
        this.userManager = userManager;
        this.userRepository = userRepository;
        this.plugin = plugin;
    }

    @Override
    public String getMenuName() {
        // 💡 PILAR 2: Lectura Type-Safe devolviendo el String crudo
        return configManager.getMessages().menus().templo().titulo();
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();

        // 🌟 BENDICIÓN ESTÁNDAR (Economía In-Game)
        ItemStack standardBless = new ItemStack(Material.GOLD_NUGGET);
        ItemMeta metaStd = standardBless.getItemMeta();
        if (metaStd != null) {
            metaStd.displayName(CrossplayUtils.parseCrossplay(player, configManager.getMessages().menus().templo().items().bendicionMenor().nombre()));

            List<net.kyori.adventure.text.Component> loreStd = configManager.getMessages().menus().templo().items().bendicionMenor().lore().stream()
                    .map(line -> CrossplayUtils.parseCrossplay(player, line))
                    .collect(Collectors.toList());

            metaStd.lore(loreStd);
            metaStd.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "buy_bless_coins");
            standardBless.setItemMeta(metaStd);
        }

        // 🌟 BENDICIÓN PREMIUM (Monetización)
        ItemStack premiumBless = new ItemStack(Material.NETHER_STAR);
        ItemMeta metaPrem = premiumBless.getItemMeta();
        if (metaPrem != null) {
            metaPrem.displayName(CrossplayUtils.parseCrossplay(player, configManager.getMessages().menus().templo().items().bendicionPremium().nombre()));

            List<net.kyori.adventure.text.Component> lorePrem = configManager.getMessages().menus().templo().items().bendicionPremium().lore().stream()
                    .map(line -> CrossplayUtils.parseCrossplay(player, line))
                    .collect(Collectors.toList());

            metaPrem.lore(lorePrem);
            metaPrem.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "buy_bless_premium");
            premiumBless.setItemMeta(metaPrem);
        }

        inventory.setItem(11, standardBless);
        inventory.setItem(15, premiumBless);
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        event.setCancelled(true);

        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        NamespacedKey actionKey = new NamespacedKey(plugin, "action");

        if (!meta.getPersistentDataContainer().has(actionKey, PersistentDataType.STRING)) return;
        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);

        NexoUser user = userManager.getUserOrNull(player.getUniqueId());
        if (user == null) return;

        if (user.hasActiveBlessing("VOID_BLESSING")) {
            CrossplayUtils.sendMessage(player, configManager.getMessages().mensajes().errores().bendicionActiva());
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.closeInventory();
            return;
        }

        player.closeInventory();

        if (action.equals("buy_bless_coins")) {
            // 🌟 COMPRA CON MONEDAS (Asíncrona)
            BigDecimal cost = new BigDecimal("50000");
            NexoEconomy eco = NexoEconomy.getPlugin(NexoEconomy.class);

            eco.getEconomyManager().updateBalanceAsync(player.getUniqueId(), NexoAccount.AccountType.PLAYER, NexoAccount.Currency.COINS, cost, false).thenAccept(success -> {
                if (success) {
                    aplicarBendicion(player, user, "VOID_BLESSING", configManager.getMessages().mensajes().exito().compraMenor());
                } else {
                    CrossplayUtils.sendMessage(player, configManager.getMessages().mensajes().errores().sinMonedas());
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }
            });

        } else if (action.equals("buy_bless_premium")) {
            // 🌟 COMPRA CON GEMAS
            int gemCost = 150;
            if (user.getGems() >= gemCost) {
                user.removeGems(gemCost);
                aplicarBendicion(player, user, "VOID_BLESSING", configManager.getMessages().mensajes().exito().compraPremium());
            } else {
                CrossplayUtils.sendMessage(player, configManager.getMessages().mensajes().errores().sinGemas());
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        }
    }

    private void aplicarBendicion(Player player, NexoUser user, String blessingId, String successMsg) {
        user.addBlessing(blessingId);

        // 🚀 Guardado asíncrono y seguro a la BD
        userRepository.saveBlessings(user);

        CrossplayUtils.sendMessage(player, successMsg);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);
    }
}