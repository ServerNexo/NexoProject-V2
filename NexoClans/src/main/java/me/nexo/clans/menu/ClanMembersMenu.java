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
 * 👥 NexoClans - Menú de Miembros (Arquitectura Enterprise)
 * Rendimiento: Cero Lag I/O, PDC Cacheado, Carga Asíncrona y Dependencias Inyectadas.
 * Nota: Al ser una GUI transitoria, NO lleva @Singleton.
 */
public class ClanMembersMenu extends NexoMenu {

    private final NexoClans plugin; // Requerido para runTask y NamespacedKey
    private final NexoClan clan;
    private final NexoUser user;
    
    // 🌟 Dependencias Inyectadas desde la Fábrica (Menú Principal / Comando)
    private final ClanManager clanManager;
    private final CrossplayUtils crossplayUtils;

    // 🌟 OPTIMIZACIÓN DE RAM: Cacheamos la llave
    private final NamespacedKey memberKey;

    public ClanMembersMenu(Player player, NexoClans plugin, NexoClan clan, NexoUser user, 
                           ClanManager clanManager, CrossplayUtils crossplayUtils) {
        super(player);
        this.plugin = plugin;
        this.clan = clan;
        this.user = user;
        this.clanManager = clanManager;
        this.crossplayUtils = crossplayUtils;

        // Cacheamos la llave al abrir el menú para no instanciarla en el bucle
        this.memberKey = new NamespacedKey(plugin, "member_name");
    }

    @Override
    public String getMenuName() {
        // 🌟 FIX: Texto Hexadecimal Directo (Cero I/O)
        return "&#FFAA00👥 <bold>MIEMBROS DEL CLAN</bold>";
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void setMenuItems() {
        // Ítem temporal holográfico mientras carga la Base de Datos
        var loading = new ItemStack(Material.CLOCK);
        var lMeta = loading.getItemMeta();
        if (lMeta != null) {
            lMeta.displayName(crossplayUtils.parseCrossplay(player, "&#ff00ff<bold>Buscando miembros en el Nexo...</bold>"));
            loading.setItemMeta(lMeta);
        }
        inventory.setItem(22, loading);

        // 🚀 Carga asíncrona segura desde Hilos Virtuales
        clanManager.getMiembrosAsync(clan.getId(), miembros -> {

            // Retornamos al Hilo Principal (Tick Loop) SOLO para modificar la UI gráfica
            Bukkit.getScheduler().runTask(plugin, () -> {

                // Protección Anti-Crash: Verificamos si el jugador cerró el menú en esa fracción de segundo
                if (player.getOpenInventory().getTopInventory() != inventory) return;

                inventory.clear(); // Limpiamos el ítem del reloj

                for (int i = 0; i < miembros.size() && i < 53; i++) {
                    ClanMember m = miembros.get(i);
                    var head = new ItemStack(Material.PLAYER_HEAD);
                    var meta = (SkullMeta) head.getItemMeta();

                    if (meta != null) {
                        meta.setOwningPlayer(Bukkit.getOfflinePlayer(m.uuid()));

                        // Sistema visual de rangos directo
                        String colorRol = "LIDER".equals(m.role()) ? "&#FF5555" : ("OFICIAL".equals(m.role()) ? "&#FFAA00" : "&#55FF55");
                        meta.displayName(crossplayUtils.parseCrossplay(player, colorRol + "<bold>" + m.name() + "</bold>"));

                        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
                        lore.add(crossplayUtils.parseCrossplay(player, "&#E6CCFFRango: " + colorRol + m.role()));
                        lore.add(crossplayUtils.parseCrossplay(player, ""));

                        if ("LIDER".equals(user.getClanRole()) || "OFICIAL".equals(user.getClanRole())) {
                            if (!"LIDER".equals(m.role()) && !m.uuid().equals(player.getUniqueId())) {
                                lore.add(crossplayUtils.parseCrossplay(player, "&#FF5555▶ Clic Derecho para Expulsar"));
                            }
                        }
                        meta.lore(lore);

                        // 🌟 MAGIA PDC: Guardamos el nombre usando la llave cacheada
                        meta.getPersistentDataContainer().set(memberKey, PersistentDataType.STRING, m.name());

                        head.setItemMeta(meta);
                    }
                    inventory.setItem(i, head);
                }

                // Botón de regresar estático
                var back = new ItemStack(Material.ARROW);
                var backMeta = back.getItemMeta();
                if (backMeta != null) {
                    backMeta.displayName(crossplayUtils.parseCrossplay(player, "&#FF5555⬅ Volver al Panel Central"));
                    back.setItemMeta(backMeta);
                }
                inventory.setItem(49, back);
            });
        });
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        event.setCancelled(true); // Bloqueo absoluto anti-robos

        var clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        // Botón de Regresar
        if (clicked.getType() == Material.ARROW && event.getRawSlot() == 49) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 0.8f);

            // 🌟 FIX: Volvemos al menú principal inyectando las herramientas correctas
            new ClanMenu(player, clan, user, clanManager, crossplayUtils).open();
            return;
        }

        // ⚔️ Sistema de Expulsión Seguro O(1)
        if (clicked.getType() == Material.PLAYER_HEAD && event.getClick().isRightClick()) {

            // Validamos contra la llave de RAM estática
            if (clicked.getItemMeta().getPersistentDataContainer().has(memberKey, PersistentDataType.STRING)) {
                String targetName = clicked.getItemMeta().getPersistentDataContainer().get(memberKey, PersistentDataType.STRING);

                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 0.5f);
                player.closeInventory();

                // Ejecución instantánea del comando
                player.performCommand("clan kick " + targetName);
            }
        }
    }
}