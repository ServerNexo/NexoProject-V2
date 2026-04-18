package me.nexo.pvp.classes;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.pvp.NexoPvP;
import me.nexo.pvp.config.ConfigManager;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🏛️ NexoPvP - Listener de Clases de Armadura (Arquitectura Enterprise)
 * Rendimiento: Cero Delays, Modificadores Cacheados y Prevención de Spam de Eventos.
 */
@Singleton
public class ArmorClassListener implements Listener {

    private final NexoPvP plugin;
    private final ConfigManager configManager;

    // 🌟 OPTIMIZACIÓN: Caché de la clase activa actual para evitar re-cálculos y spam de sonidos
    private final Map<UUID, String> activeClasses = new ConcurrentHashMap<>();

    private final NamespacedKey classKey;
    private final NamespacedKey healthModKey;
    private final NamespacedKey speedModKey;

    // 🌟 OPTIMIZACIÓN: Reutilizamos los mismos modificadores para no inundar la RAM con "new AttributeModifier()"
    private final AttributeModifier assassinHealthMod;
    private final AttributeModifier assassinSpeedMod;
    private final AttributeModifier inquisitorHealthMod;

    @Inject
    public ArmorClassListener(NexoPvP plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;

        this.classKey = new NamespacedKey("nexoitems", "nexo_class");
        this.healthModKey = new NamespacedKey(plugin, "class_health_modifier");
        this.speedModKey = new NamespacedKey(plugin, "class_speed_modifier");

        // Pre-calculamos los modificadores estáticos (Versión 1.21+)
        this.assassinHealthMod = new AttributeModifier(healthModKey, -0.5, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        this.assassinSpeedMod = new AttributeModifier(speedModKey, 0.4, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        this.inquisitorHealthMod = new AttributeModifier(healthModKey, -0.3, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
    }

    // =========================================================================
    // 🛡️ EVENTOS DE EVALUACIÓN
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArmorChange(PlayerArmorChangeEvent event) {
        // 🌟 FIX: PaperMC dispara este evento cuando el inventario YA SE ACTUALIZÓ.
        // No necesitamos runTask, lo evaluamos en el mismo tick instantáneamente.
        evaluateClassSet(event.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        evaluateClassSet(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // 🌟 FIX MEMORY LEAK: Limpiamos la caché cuando el jugador se va
        activeClasses.remove(event.getPlayer().getUniqueId());
    }

    // =========================================================================
    // ⚙️ MOTOR DE ATRIBUTOS
    // =========================================================================

    private void evaluateClassSet(Player player) {
        PlayerInventory inv = player.getInventory();

        String helmetClass = getClassTag(inv.getHelmet());
        String chestClass = getClassTag(inv.getChestplate());
        String legsClass = getClassTag(inv.getLeggings());
        String bootsClass = getClassTag(inv.getBoots());

        // Lógica: Si no tiene casco, o las 4 piezas no son idénticas, su clase es "NONE"
        String currentClass = "NONE";
        if (helmetClass != null && helmetClass.equals(chestClass) && helmetClass.equals(legsClass) && helmetClass.equals(bootsClass)) {
            currentClass = helmetClass.toUpperCase();
        }

        // 🌟 PREVENCIÓN DE SPAM: Verificamos si la clase realmente cambió
        String previousClass = activeClasses.getOrDefault(player.getUniqueId(), "NONE");

        if (currentClass.equals(previousClass)) {
            return; // No hay cambios, ignoramos para ahorrar CPU y evitar spam
        }

        // Si cambió, actualizamos la memoria RAM y limpiamos atributos
        activeClasses.put(player.getUniqueId(), currentClass);
        clearClassModifiers(player);

        // Aplicamos la nueva clase si existe
        if (!currentClass.equals("NONE")) {
            applyClassBuffs(player, currentClass);
        }
    }

    private void applyClassBuffs(Player player, String className) {
        AttributeInstance healthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        AttributeInstance speedAttr = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);

        switch (className) {
            case "ASSASSIN":
                if (healthAttr != null) healthAttr.addModifier(assassinHealthMod);
                if (speedAttr != null) speedAttr.addModifier(assassinSpeedMod);

                CrossplayUtils.sendActionBar(player, configManager.getMessages().mensajes().pvp().setAsesinoActivo());
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.3f, 2.0f);
                break;

            case "INQUISITOR":
                if (healthAttr != null) healthAttr.addModifier(inquisitorHealthMod);

                CrossplayUtils.sendActionBar(player, configManager.getMessages().mensajes().pvp().setInquisidorActivo());
                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.5f);
                break;
        }
    }

    /**
     * Obtiene el Tag de la armadura evitando crear clones de ItemMeta innecesarios.
     */
    private String getClassTag(ItemStack item) {
        // 🌟 FIX RENDIMIENTO: Verificación rápida antes de clonar el ItemMeta
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        return meta.getPersistentDataContainer().get(classKey, PersistentDataType.STRING);
    }

    private void clearClassModifiers(Player player) {
        AttributeInstance healthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (healthAttr != null) {
            // Removemos basándonos en la llave estática
            for (AttributeModifier mod : healthAttr.getModifiers()) {
                if (mod.getKey().equals(healthModKey)) healthAttr.removeModifier(mod);
            }
        }

        AttributeInstance speedAttr = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (speedAttr != null) {
            for (AttributeModifier mod : speedAttr.getModifiers()) {
                if (mod.getKey().equals(speedModKey)) speedAttr.removeModifier(mod);
            }
        }
    }
}