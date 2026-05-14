package me.aeroxis.pvp.menus;

import me.aeroxis.core.crossplay.CrossplayUtils;
import me.aeroxis.core.menus.AeroxisMenu;
import me.aeroxis.core.user.AeroxisUser;
import me.aeroxis.core.user.UserManager;
import me.aeroxis.core.user.UserRepository;
import me.aeroxis.economy.core.EconomyManager;
import me.aeroxis.economy.core.AeroxisAccount;
import me.aeroxis.pvp.AeroxisPvP;
import me.aeroxis.pvp.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.math.BigDecimal;

/**
 * 🏛️ AeroxisPvP - Menú de Bendiciones (Arquitectura Enterprise)
 * Cero estáticos. Cero fugas de memoria. Thread-Safe para la API de Bukkit.
 * Nota: NO lleva @Singleton porque es una instancia por jugador.
 */
public class BlessingMenu extends AeroxisMenu {

    // 💉 PILAR 1: Dependencias inyectadas desde el Comando/Factory
    private final ConfigManager configManager;
    private final UserManager userManager;
    private final UserRepository userRepository;
    private final AeroxisPvP plugin;
    private final CrossplayUtils crossplayUtils;
    private final EconomyManager economyManager; // 🌟 Sinergia inyectada con AeroxisEconomy

    public BlessingMenu(Player player, ConfigManager configManager, UserManager userManager,
                        UserRepository userRepository, AeroxisPvP plugin,
                        CrossplayUtils crossplayUtils, EconomyManager economyManager) {
        super(player, crossplayUtils); // 🌟 Pasamos el CrossplayUtils a la superclase
        this.configManager = configManager;
        this.userManager = userManager;
        this.userRepository = userRepository;
        this.plugin = plugin;
        this.crossplayUtils = crossplayUtils;
        this.economyManager = economyManager;
    }

    @Override
    public String getMenuName() {
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
        var standardBless = new ItemStack(Material.GOLD_NUGGET);
        var metaStd = standardBless.getItemMeta();
        if (metaStd != null) {
            metaStd.displayName(crossplayUtils.parseCrossplay(player, configManager.getMessages().menus().templo().items().bendicionMenor().nombre()));

            var loreStd = configManager.getMessages().menus().templo().items().bendicionMenor().lore().stream()
                    .map(line -> crossplayUtils.parseCrossplay(player, line))
                    .toList(); // 🚀 Optimización Java 16+

            metaStd.lore(loreStd);
            metaStd.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "buy_bless_coins");
            standardBless.setItemMeta(metaStd);
        }

        // 🌟 BENDICIÓN PREMIUM (Monetización)
        var premiumBless = new ItemStack(Material.NETHER_STAR);
        var metaPrem = premiumBless.getItemMeta();
        if (metaPrem != null) {
            metaPrem.displayName(crossplayUtils.parseCrossplay(player, configManager.getMessages().menus().templo().items().bendicionPremium().nombre()));

            var lorePrem = configManager.getMessages().menus().templo().items().bendicionPremium().lore().stream()
                    .map(line -> crossplayUtils.parseCrossplay(player, line))
                    .toList();

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

        var item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        var meta = item.getItemMeta();
        var actionKey = new NamespacedKey(plugin, "action");

        if (!meta.getPersistentDataContainer().has(actionKey, PersistentDataType.STRING)) return;
        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);

        AeroxisUser user = userManager.getUserOrNull(player.getUniqueId());
        if (user == null) return;

        if (user.hasActiveBlessing("VOID_BLESSING")) {
            crossplayUtils.sendMessage(player, configManager.getMessages().mensajes().errores().bendicionActiva());
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.closeInventory();
            return;
        }

        player.closeInventory();

        if ("buy_bless_coins".equals(action)) {
            // 🌟 COMPRA CON MONEDAS (Asíncrona y Segura)
            var cost = new BigDecimal("50000");

            economyManager.updateBalanceAsync(player.getUniqueId(), AeroxisAccount.AccountType.PLAYER, AeroxisAccount.Currency.COINS, cost, false).thenAccept(success -> {
                // 🛡️ FIX CRÍTICO: Regresamos al Main Thread para manipular la API de Bukkit (Sonidos/Mensajes visuales)
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (success) {
                        aplicarBendicion(player, user, "VOID_BLESSING", configManager.getMessages().mensajes().exito().compraMenor());
                    } else {
                        crossplayUtils.sendMessage(player, configManager.getMessages().mensajes().errores().sinMonedas());
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    }
                });
            });

        } else if ("buy_bless_premium".equals(action)) {
            // 🌟 COMPRA CON GEMAS (Síncrona en RAM)
            int gemCost = 150;

            // 🌟 FIX: Restaurado el nombre original que usaste en AeroxisUser (getGems() y removeGems())
            if (user.getGems() >= gemCost) {
                user.removeGems(gemCost);
                aplicarBendicion(player, user, "VOID_BLESSING", configManager.getMessages().mensajes().exito().compraPremium());
            } else {
                crossplayUtils.sendMessage(player, configManager.getMessages().mensajes().errores().sinGemas());
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        }
    }

    private void aplicarBendicion(Player player, AeroxisUser user, String blessingId, String successMsg) {
        user.addBlessing(blessingId);

        // 🚀 Guardado asíncrono gestionado internamente por el UserRepository (Virtual Threads)
        userRepository.saveBlessings(user);

        crossplayUtils.sendMessage(player, successMsg);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);
    }
}