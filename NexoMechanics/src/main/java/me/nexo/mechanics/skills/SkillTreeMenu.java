package me.nexo.mechanics.skills;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import me.nexo.core.user.NexoUser;
import me.nexo.core.user.UserManager;
import me.nexo.mechanics.NexoMechanics;
import me.nexo.mechanics.config.ConfigManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.permissions.PermissionAttachment;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ⚙️ NexoMechanics - Menú de Árbol de Habilidades (Arquitectura Enterprise)
 * Nota: Los permisos se gestionan a través de una sinergia entre UserManager y Attachments.
 */
public class SkillTreeMenu extends NexoMenu {

    private final NexoMechanics plugin;
    private final ConfigManager configManager;
    private final UserManager userManager;
    private final CrossplayUtils crossplayUtils;

    // 🌟 OPTIMIZACIÓN: Llaves persistentes cacheadas para evitar recolectores de basura (GC)
    private final NamespacedKey keyAction;
    private final NamespacedKey keyPerm;
    private final NamespacedKey keyCost;

    // Registro de permisos (Mantenido por instancia o inyectado por un SessionManager)
    private final Map<UUID, PermissionAttachment> sessionPermissions;

    public SkillTreeMenu(Player player, 
                         NexoMechanics plugin, 
                         ConfigManager configManager, 
                         UserManager userManager,
                         CrossplayUtils crossplayUtils,
                         Map<UUID, PermissionAttachment> sessionPermissions) {
        super(player);
        this.plugin = plugin;
        this.configManager = configManager;
        this.userManager = userManager;
        this.crossplayUtils = crossplayUtils;
        this.sessionPermissions = sessionPermissions;

        this.keyAction = new NamespacedKey(plugin, "action");
        this.keyPerm = new NamespacedKey(plugin, "node_perm");
        this.keyCost = new NamespacedKey(plugin, "node_cost");
    }

    @Override
    public String getMenuName() {
        return configManager.getMessages().menus().skillTree().titulo();
    }

    @Override
    public int getSlots() {
        return 45;
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();

        var user = userManager.getUserOrNull(player.getUniqueId());
        int kp = (user != null) ? user.getKnowledgePoints() : 0;

        // 🌟 ÍTEM DE INFORMACIÓN DEL JUGADOR
        var infoName = configManager.getMessages().menus().skillTree().items().info().nombre()
                .replace("%kp%", String.valueOf(kp));
        var infoLore = configManager.getMessages().menus().skillTree().items().info().lore();
        setItem(4, Material.ENCHANTED_BOOK, infoName, infoLore);

        // 🌟 NODO DE EJEMPLO (Minería)
        var miningNode = new ItemStack(Material.DIAMOND_PICKAXE);
        var miningMeta = miningNode.getItemMeta();
        
        if (miningMeta != null) {
            var nodeName = configManager.getMessages().menus().skillTree().nodos().mineria().nombre();
            miningMeta.displayName(crossplayUtils.parseCrossplay(player, nodeName));

            var lore = configManager.getMessages().menus().skillTree().nodos().mineria().lore().stream()
                    .map(line -> crossplayUtils.parseCrossplay(player, line.replace("%cost%", "5")))
                    .toList(); // Java 16+ toList() es más eficiente

            miningMeta.lore(lore);

            var pdc = miningMeta.getPersistentDataContainer();
            pdc.set(keyAction, PersistentDataType.STRING, "unlock_node");
            pdc.set(keyPerm, PersistentDataType.STRING, "nexo.skills.mining.1");
            pdc.set(keyCost, PersistentDataType.INTEGER, 5);
            
            miningNode.setItemMeta(miningMeta);
        }
        inventory.setItem(22, miningNode);
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        event.setCancelled(true);

        var item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        var meta = item.getItemMeta();
        var pdc = meta.getPersistentDataContainer();

        if (pdc.has(keyAction, PersistentDataType.STRING)) {
            var action = pdc.get(keyAction, PersistentDataType.STRING);

            if ("unlock_node".equals(action)) {
                var perm = pdc.get(keyPerm, PersistentDataType.STRING);
                var cost = pdc.get(keyCost, PersistentDataType.INTEGER);

                if (perm != null && cost != null) {
                    desbloquearNodo(player, perm, cost);
                    setMenuItems();
                }
            }
        }
    }

    private void desbloquearNodo(Player p, String permiso, int costo) {
        var user = userManager.getUserOrNull(p.getUniqueId());

        if (user == null) {
            crossplayUtils.sendMessage(p, configManager.getMessages().mensajes().errores().sincronizacionIncompleta());
            return;
        }

        if (user.getKnowledgePoints() >= costo) {
            user.removeKnowledgePoints(costo);

            // Gestión de permisos mediante Inyección de Sesión
            var attachment = sessionPermissions.computeIfAbsent(p.getUniqueId(),
                    k -> p.addAttachment(plugin));

            attachment.setPermission(permiso, true);

            crossplayUtils.sendMessage(p, configManager.getMessages().mensajes().exito().nodoDesbloqueado());
            crossplayUtils.sendMessage(p, configManager.getMessages().mensajes().exito().puntosRestantes()
                    .replace("%kp%", String.valueOf(user.getKnowledgePoints())));
            
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        } else {
            crossplayUtils.sendMessage(p, configManager.getMessages().mensajes().errores().conocimientoInsuficiente());
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }
}