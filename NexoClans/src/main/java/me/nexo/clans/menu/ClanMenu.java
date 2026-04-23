package me.nexo.clans.menu;

import me.nexo.clans.core.ClanManager;
import me.nexo.clans.core.NexoClan;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import me.nexo.core.user.NexoUser;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 👥 NexoClans - Panel de Control Principal (Arquitectura Enterprise)
 * Rendimiento: Renderizado O(1), Cero Lag I/O, Inyección Pasiva y Construcción Dinámica.
 * Nota: Al ser una GUI por jugador, NO lleva @Singleton.
 */
public class ClanMenu extends NexoMenu {

    private final NexoClan clan;
    private final NexoUser user;

    // 🌟 Sinergia inyectada a través de la fábrica (Comando/Evento que abre el menú)
    private final ClanManager clanManager;
    private final CrossplayUtils crossplayUtils;

    public ClanMenu(Player player, NexoClan clan, NexoUser user, ClanManager clanManager, CrossplayUtils crossplayUtils) {
        super(player);
        this.clan = clan;
        this.user = user;
        this.clanManager = clanManager;
        this.crossplayUtils = crossplayUtils;
    }

    @Override
    public String getMenuName() {
        return "&#FFAA00🛡️ <bold>BASE DE OPERACIONES</bold>";
    }

    @Override
    public int getSlots() {
        return 36;
    }

    @Override
    public void setMenuItems() {
        setFillerGlass(); // Añade el cristal morado de fondo

        boolean isLider = "LIDER".equals(user.getClanRole());
        boolean isOficial = "OFICIAL".equals(user.getClanRole()) || isLider;

        // ==========================================
        // 💎 EL MONOLITO (Slot 13)
        // ==========================================
        var monolith = new ItemStack(Material.BEACON);
        var monoMeta = monolith.getItemMeta();
        if (monoMeta != null) {
            monoMeta.displayName(crossplayUtils.parseCrossplay(player, "&#ff00ff💎 <bold>MONOLITO CENTRAL</bold>"));

            List<net.kyori.adventure.text.Component> monoLore = new ArrayList<>();
            monoLore.add(crossplayUtils.parseCrossplay(player, "&#E6CCFFFacción: &#FFAA00" + clan.getName() + " [" + clan.getTag() + "]"));
            monoLore.add(crossplayUtils.parseCrossplay(player, "&#E6CCFFNivel de Poder: &#55FF55" + clan.getMonolithLevel()));
            monoLore.add(crossplayUtils.parseCrossplay(player, "&#E6CCFFExperiencia: &#ff00ff" + clan.getMonolithExp()));

            if (isLider) {
                monoLore.add(crossplayUtils.parseCrossplay(player, ""));
                monoLore.add(crossplayUtils.parseCrossplay(player, "&#555555[!] Usa /clan sethome para fijar la base"));
                monoLore.add(crossplayUtils.parseCrossplay(player, "&#555555[!] Usa /clan disband para disolver"));
            }
            monoMeta.lore(monoLore);
            monolith.setItemMeta(monoMeta);
        }
        inventory.setItem(13, monolith);

        // ==========================================
        // 💰 TESORERÍA (Slot 20)
        // ==========================================
        var bank = new ItemStack(Material.GOLD_INGOT);
        var bankMeta = bank.getItemMeta();
        if (bankMeta != null) {
            bankMeta.displayName(crossplayUtils.parseCrossplay(player, "&#FFAA00💰 <bold>TESORERÍA DE FACCIÓN</bold>"));

            List<net.kyori.adventure.text.Component> bankLore = new ArrayList<>();
            bankLore.add(crossplayUtils.parseCrossplay(player, "&#E6CCFFFondos Disponibles: &#FFAA00" + clan.getBankBalance().toPlainString() + " ⛃"));
            bankLore.add(crossplayUtils.parseCrossplay(player, ""));
            bankLore.add(crossplayUtils.parseCrossplay(player, "&#55FF55▶ Usa /clan deposit <cant> para aportar"));

            if (isOficial) {
                bankLore.add(crossplayUtils.parseCrossplay(player, "&#FF5555▶ Usa /clan withdraw <cant> para retirar"));
            }
            bankMeta.lore(bankLore);
            bank.setItemMeta(bankMeta);
        }
        inventory.setItem(20, bank);

        // ==========================================
        // ⚔️ FUEGO ALIADO (Slot 22)
        // ==========================================
        var sword = new ItemStack(clan.isFriendlyFire() ? Material.IRON_SWORD : Material.WOODEN_SWORD);
        var swordMeta = sword.getItemMeta();
        if (swordMeta != null) {
            swordMeta.displayName(crossplayUtils.parseCrossplay(player, "&#FF3366⚔ <bold>FUEGO ALIADO</bold>"));
            swordMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES); // Escondemos el daño de la espada

            String status = clan.isFriendlyFire() ? "&#FF3366<bold>RIESGO DE SANGRE ACTIVO</bold>" : "&#55FF55<bold>SEGURO Y APAGADO</bold>";
            List<net.kyori.adventure.text.Component> swordLore = new ArrayList<>();
            swordLore.add(crossplayUtils.parseCrossplay(player, "&#E6CCFFEstado actual: " + status));

            if (isOficial) {
                swordLore.add(crossplayUtils.parseCrossplay(player, ""));
                swordLore.add(crossplayUtils.parseCrossplay(player, "&#FFAA00▶ Haz clic para alternar protección"));
            }
            swordMeta.lore(swordLore);
            sword.setItemMeta(swordMeta);
        }
        inventory.setItem(22, sword);

        // ==========================================
        // 👥 MIEMBROS (Slot 24)
        // ==========================================
        var heads = new ItemStack(Material.PLAYER_HEAD);
        var headsMeta = heads.getItemMeta();
        if (headsMeta != null) {
            headsMeta.displayName(crossplayUtils.parseCrossplay(player, "&#55FF55👥 <bold>LISTA DE MIEMBROS</bold>"));

            String colorRol = isLider ? "&#FF5555" : (isOficial ? "&#FFAA00" : "&#55FF55");
            List<net.kyori.adventure.text.Component> headsLore = new ArrayList<>();
            headsLore.add(crossplayUtils.parseCrossplay(player, "&#E6CCFFTu rango actual: " + colorRol + user.getClanRole()));
            headsLore.add(crossplayUtils.parseCrossplay(player, ""));
            headsLore.add(crossplayUtils.parseCrossplay(player, "&#FFAA00▶ Haz clic para gestionar la facción"));

            headsMeta.lore(headsLore);
            heads.setItemMeta(headsMeta);
        }
        inventory.setItem(24, heads);
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        event.setCancelled(true); // Bloqueo absoluto anti-robos

        int slot = event.getRawSlot();

        // 🛡️ Clic en Fuego Amigo
        if (slot == 22) {
            if ("LIDER".equals(user.getClanRole()) || "OFICIAL".equals(user.getClanRole())) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);
                player.closeInventory();

                // 🌟 FIX: Ejecutamos el toggle asíncrono utilizando el manager inyectado
                clanManager.toggleFriendlyFireAsync(clan, player, !clan.isFriendlyFire());
            }
        }
        // 👥 Clic en Miembros
        else if (slot == 24) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);

            // 🚀 Abrimos el menú de miembros pasándole nuestras dependencias purificadas
            new ClanMembersMenu(player, clan, user, clanManager, crossplayUtils).open();
        }
    }
}