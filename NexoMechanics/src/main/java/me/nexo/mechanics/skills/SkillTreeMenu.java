package me.nexo.mechanics.skills;

import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import me.nexo.core.user.NexoUser;
import me.nexo.mechanics.NexoMechanics;
import me.nexo.mechanics.config.ConfigManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * ⚙️ NexoMechanics - Menú de Árbol de Habilidades (Arquitectura Enterprise)
 */
public class SkillTreeMenu extends NexoMenu {

    private final NexoMechanics plugin;
    private final ConfigManager configManager;

    // Registro de permisos temporales de la sesión actual
    private static final Map<UUID, PermissionAttachment> perms = new HashMap<>();

    public SkillTreeMenu(Player player, NexoMechanics plugin) {
        super(player);
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager(); // 💡 Acceso a memoria RAM segura
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
        setFillerGlass(); // Fondo de la Arquitectura Omega

        // 🌟 Desacoplado con el Service Locator (Sin estáticos duros)
        NexoUser user = JavaPlugin.getPlugin(NexoCore.class).getUserManager().getUserOrNull(player.getUniqueId());
        int kp = user != null ? user.getKnowledgePoints() : 0;

        // 🌟 ÍTEM DE INFORMACIÓN DEL JUGADOR
        String infoName = configManager.getMessages().menus().skillTree().items().info().nombre().replace("%kp%", String.valueOf(kp));
        List<String> infoLore = configManager.getMessages().menus().skillTree().items().info().lore();
        setItem(4, Material.ENCHANTED_BOOK, infoName, infoLore);

        // 🌟 NODO DE EJEMPLO (Minería)
        ItemStack miningNode = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemMeta miningMeta = miningNode.getItemMeta();
        if (miningMeta != null) {
            String nodeName = configManager.getMessages().menus().skillTree().nodos().mineria().nombre();
            miningMeta.displayName(CrossplayUtils.parseCrossplay(player, nodeName));

            List<net.kyori.adventure.text.Component> lore = configManager.getMessages().menus().skillTree().nodos().mineria().lore().stream()
                    .map(line -> CrossplayUtils.parseCrossplay(player, line.replace("%cost%", "5")))
                    .collect(Collectors.toList());

            miningMeta.lore(lore);

            // MAGIA PDC: Guardamos el permiso a dar y cuánto cuesta
            miningMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "unlock_node");
            miningMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "node_perm"), PersistentDataType.STRING, "nexo.skills.mining.1");
            miningMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "node_cost"), PersistentDataType.INTEGER, 5);
            miningNode.setItemMeta(miningMeta);
        }
        inventory.setItem(22, miningNode);
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        event.setCancelled(true); // Bloqueo absoluto anti-robos

        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        NamespacedKey actionKey = new NamespacedKey(plugin, "action");

        if (meta.getPersistentDataContainer().has(actionKey, PersistentDataType.STRING)) {
            String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);

            if (action.equals("unlock_node")) {
                String perm = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "node_perm"), PersistentDataType.STRING);
                Integer cost = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "node_cost"), PersistentDataType.INTEGER);

                if (perm != null && cost != null) {
                    desbloquearNodo(player, perm, cost);
                    // Actualizamos la interfaz al instante para que vea cómo se restaron sus KP
                    setMenuItems();
                }
            }
        }
    }

    // ==========================================
    // 🧠 LÓGICA DE DESBLOQUEO PURIFICADA
    // ==========================================
    private void desbloquearNodo(Player p, String permiso, int costo) {
        NexoUser user = JavaPlugin.getPlugin(NexoCore.class).getUserManager().getUserOrNull(p.getUniqueId());

        if (user == null) {
            CrossplayUtils.sendMessage(p, configManager.getMessages().mensajes().errores().sincronizacionIncompleta());
            return;
        }

        if (user.getKnowledgePoints() >= costo) {
            user.removeKnowledgePoints(costo);

            PermissionAttachment attachment = perms.computeIfAbsent(p.getUniqueId(),
                    k -> p.addAttachment(JavaPlugin.getPlugin(NexoCore.class)));

            attachment.setPermission(permiso, true);

            CrossplayUtils.sendMessage(p, configManager.getMessages().mensajes().exito().nodoDesbloqueado());
            CrossplayUtils.sendMessage(p, configManager.getMessages().mensajes().exito().puntosRestantes().replace("%kp%", String.valueOf(user.getKnowledgePoints())));
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);

        } else {
            CrossplayUtils.sendMessage(p, configManager.getMessages().mensajes().errores().conocimientoInsuficiente());
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    // Limpieza de memoria (Ejecutado al desconectarse el jugador en otro listener)
    public static void limpiarCachePermisos(UUID uuid) {
        perms.remove(uuid);
    }
}