package me.aeroxis.colecciones.menu;

import me.aeroxis.core.menus.AeroxisMenu;
import me.aeroxis.colecciones.AeroxisColecciones;
import me.aeroxis.colecciones.slayers.SlayerManager;
import me.aeroxis.core.crossplay.CrossplayUtils;
import me.aeroxis.economy.core.EconomyManager;
import me.aeroxis.economy.core.AeroxisAccount; // 🌟 IMPORTANTE: Añadido para el tipo de moneda
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.math.BigDecimal;
import java.util.List;

/**
 * 📚 AeroxisColecciones - Menú de Contratos Slayer (Arquitectura Enterprise Java 21)
 * Rendimiento: Generación Dinámica, Integración AeroxisEconomy y Cero Lag Visual.
 */
public class SlayerMenu extends AeroxisMenu {

    private final AeroxisColecciones plugin;
    private final SlayerManager slayerManager;
    private final CrossplayUtils crossplayUtils;
    private final EconomyManager economyManager;

    private final NamespacedKey actionKey;
    private final NamespacedKey slayerKey;

    public SlayerMenu(Player player, AeroxisColecciones plugin, SlayerManager slayerManager, CrossplayUtils crossplayUtils, EconomyManager economyManager) {
        super(player, crossplayUtils);
        this.plugin = plugin;
        this.slayerManager = slayerManager;
        this.crossplayUtils = crossplayUtils;
        this.economyManager = economyManager;

        this.actionKey = new NamespacedKey(plugin, "action");
        this.slayerKey = new NamespacedKey(plugin, "slayer_id");
    }

    @Override
    public String getMenuName() {
        return "&#FF5555⚔ <bold>CONTRATOS SLAYER</bold>";
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();

        int slot = 10;

        // Verificamos de antemano si el jugador ya tiene una misión
        boolean hasQuest = slayerManager.hasActiveQuest(player.getUniqueId());

        for (var template : slayerManager.getTemplates().values()) {
            if (slot >= 17) break;

            var mat = Material.matchMaterial(template.targetMob() + "_SPAWN_EGG");
            if (mat == null) mat = Material.SKELETON_SKULL;

            var item = new ItemStack(mat);

            // 🌟 COSTO DINÁMICO: 50 Monedas por cada mob que tenga que matar
            double cost = template.requiredKills() * 50.0;

            item.editMeta(meta -> {
                meta.displayName(crossplayUtils.parseCrossplay(player, "&#ff00ff<bold>" + template.name().toUpperCase() + "</bold>"));

                // Estado visual del botón
                String statusText = hasQuest ? "&#FF5555[!] Ya tienes una cacería activa" : "&#FFAA00▶ Haz clic para firmar contrato";

                List<net.kyori.adventure.text.Component> lore = List.of(
                        crossplayUtils.parseCrossplay(player, "&#555555Contrato de Exterminio"),
                        net.kyori.adventure.text.Component.empty(),
                        crossplayUtils.parseCrossplay(player, "&#E6CCFFObjetivo: &#FF5555" + template.targetMob()),
                        crossplayUtils.parseCrossplay(player, "&#E6CCFFKills Requeridas: &#FFAA00" + template.requiredKills()),
                        crossplayUtils.parseCrossplay(player, "&#E6CCFFJefe a Invocar: &#55FF55" + template.bossName()),
                        crossplayUtils.parseCrossplay(player, "&#E6CCFFPrecio: &#55FF55$" + String.format("%,.0f", cost)),
                        net.kyori.adventure.text.Component.empty(),
                        crossplayUtils.parseCrossplay(player, statusText)
                );

                meta.lore(lore);
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);

                meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "start_slayer");
                meta.getPersistentDataContainer().set(slayerKey, PersistentDataType.STRING, template.id());
            });

            inventory.setItem(slot++, item);
        }
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        event.setCancelled(true);

        var item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        var meta = item.getItemMeta();

        if (meta.getPersistentDataContainer().has(actionKey, PersistentDataType.STRING)) {
            String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);

            if ("start_slayer".equals(action)) {

                // 1. Validamos que no tenga misiones activas
                if (slayerManager.hasActiveQuest(player.getUniqueId())) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    crossplayUtils.sendMessage(player, "&#FF5555[x] Debes terminar o cancelar tu cacería actual primero.");
                    player.closeInventory();
                    return;
                }

                String slayerId = meta.getPersistentDataContainer().get(slayerKey, PersistentDataType.STRING);
                var template = slayerManager.getTemplates().get(slayerId);

                if (template == null) return;

                // 🌟 FIX: Uso correcto de la API asíncrona de AeroxisEconomy
                BigDecimal cost = BigDecimal.valueOf(template.requiredKills() * 50.0);

                if (economyManager.hasBalance(player.getUniqueId(), AeroxisAccount.AccountType.PLAYER, AeroxisAccount.Currency.COINS, cost)) {

                    // Descontamos atómicamente de forma asíncrona (el false indica que es un retiro)
                    economyManager.updateBalanceAsync(player.getUniqueId(), AeroxisAccount.AccountType.PLAYER, AeroxisAccount.Currency.COINS, cost, false);

                    player.closeInventory();

                    slayerManager.startQuest(player.getUniqueId(), template.id(), template.targetMob(), template.requiredKills());
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);

                } else {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    crossplayUtils.sendMessage(player, "&#FF5555[x] No tienes suficientes monedas para este contrato.");
                }
            }
        }
    }
}