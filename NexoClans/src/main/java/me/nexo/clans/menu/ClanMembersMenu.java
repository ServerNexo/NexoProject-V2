package me.nexo.clans.menu;

import me.nexo.clans.NexoClans;
import me.nexo.clans.core.ClanManager;
import me.nexo.clans.core.ClanMember;
import me.nexo.clans.core.NexoClan;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import me.nexo.core.user.NexoUser;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * 👥 NexoClans - Menú de Miembros (Arquitectura Enterprise Java 21)
 * Rendimiento: Cero Lag I/O, PlayerProfile Nativo, Folia Sync y Dependencias Inyectadas.
 */
public class ClanMembersMenu extends NexoMenu {

    private final NexoClans plugin;
    private final NexoClan clan;
    private final NexoUser user;

    private final ClanManager clanManager;
    private final CrossplayUtils crossplayUtils;

    private final NamespacedKey memberKey;

    public ClanMembersMenu(Player player, NexoClans plugin, NexoClan clan, NexoUser user,
                           ClanManager clanManager, CrossplayUtils crossplayUtils) {

        // 🌟 FIX ERROR SUPER: Pasamos la dependencia inyectada a la clase Padre
        super(player, crossplayUtils);
        /* 💡 NOTA: Si tu clase NexoMenu.java original no tiene CrossplayUtils en su
           constructor, debes ir a NexoCore y añadirlo:
           `public NexoMenu(Player player, CrossplayUtils crossplayUtils) { ... }` */

        this.plugin = plugin;
        this.clan = clan;
        this.user = user;
        this.clanManager = clanManager;
        this.crossplayUtils = crossplayUtils;

        this.memberKey = new NamespacedKey(plugin, "member_name");
    }

    @Override
    public String getMenuName() {
        return "&#FFAA00👥 <bold>MIEMBROS DEL CLAN</bold>";
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void setMenuItems() {
        var loading = new ItemStack(Material.CLOCK);
        loading.editMeta(meta ->
                meta.displayName(crossplayUtils.parseCrossplay(player, "&#ff00ff<bold>Buscando miembros en el Nexo...</bold>"))
        );
        inventory.setItem(22, loading);

        // 🚀 Carga asíncrona segura desde Hilos Virtuales
        clanManager.getMiembrosAsync(clan.getId(), miembros -> {

            // 🛡️ FOLIA SYNC (Regla 3): Retornamos a la región del jugador, NO al hilo global
            player.getScheduler().run(plugin, task -> {

                if (player.getOpenInventory().getTopInventory() != inventory) return;

                inventory.clear();

                for (int i = 0; i < miembros.size() && i < 53; i++) {
                    ClanMember m = miembros.get(i);
                    var head = new ItemStack(Material.PLAYER_HEAD);

                    head.editMeta(SkullMeta.class, meta -> {
                        // 🌟 ZERO-LAG (Regla 3): Evita Bukkit.getOfflinePlayer (que bloquea el disco duro).
                        // Creamos un PlayerProfile nativo en Paper para resolución instantánea O(1).
                        meta.setPlayerProfile(Bukkit.createProfile(m.uuid(), m.name()));

                        String colorRol = "LIDER".equals(m.role()) ? "&#FF5555" : ("OFICIAL".equals(m.role()) ? "&#FFAA00" : "&#55FF55");
                        meta.displayName(crossplayUtils.parseCrossplay(player, colorRol + "<bold>" + m.name() + "</bold>"));

                        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
                        lore.add(crossplayUtils.parseCrossplay(player, "&#E6CCFFRango: " + colorRol + m.role()));
                        lore.add(net.kyori.adventure.text.Component.empty());

                        if ("LIDER".equals(user.getClanRole()) || "OFICIAL".equals(user.getClanRole())) {
                            if (!"LIDER".equals(m.role()) && !m.uuid().equals(player.getUniqueId())) {
                                lore.add(crossplayUtils.parseCrossplay(player, "&#FF5555▶ Clic Derecho para Expulsar"));
                            }
                        }
                        meta.lore(lore);

                        meta.getPersistentDataContainer().set(memberKey, PersistentDataType.STRING, m.name());
                    });

                    inventory.setItem(i, head);
                }

                var back = new ItemStack(Material.ARROW);
                back.editMeta(meta ->
                        meta.displayName(crossplayUtils.parseCrossplay(player, "&#FF5555⬅ Volver al Panel Central"))
                );
                inventory.setItem(49, back);

            }, null);
        });
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        event.setCancelled(true);

        var clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        if (clicked.getType() == Material.ARROW && event.getRawSlot() == 49) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 0.8f);

            // 🌟 FIX CRÍTICO: Pasamos la variable 'plugin' al crear el ClanMenu
            new ClanMenu(player, plugin, clan, user, clanManager, crossplayUtils).open();
            return;
        }

        // ⚔️ Sistema de Expulsión Seguro O(1)
        if (clicked.getType() == Material.PLAYER_HEAD && event.getClick().isRightClick()) {
            var pdc = clicked.getItemMeta().getPersistentDataContainer();
            if (pdc.has(memberKey, PersistentDataType.STRING)) {
                String targetName = pdc.get(memberKey, PersistentDataType.STRING);

                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 0.5f);
                player.closeInventory();

                player.performCommand("clan kick " + targetName);
            }
        }
    }
}