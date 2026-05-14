package me.aeroxis.pvp.classes;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.core.crossplay.CrossplayUtils;
import me.aeroxis.pvp.AeroxisPvP;
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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🏛️ AeroxisPvP - Listener de Trinidad RPG (Arquitectura Enterprise)
 * Rendimiento: Modificadores Cacheados (NamespacedKey), Prevención de Spam y Balance AAA.
 */
@Singleton
public class ArmorClassListener implements Listener {

    private final ArmorWeightManager weightManager;
    private final CrossplayUtils crossplayUtils;

    // 🌟 OPTIMIZACIÓN: Caché para evitar re-cálculos si el jugador se pone la misma ropa
    private final Map<UUID, ArmorWeightManager.ArmorClass> activeClasses = new ConcurrentHashMap<>();

    private final NamespacedKey healthModKey;
    private final NamespacedKey speedModKey;
    private final NamespacedKey kbModKey;

    // ⚖️ BALANCE COMPETITIVO (Atributos inmutables en RAM)
    private final AttributeModifier lightSpeedMod;
    private final AttributeModifier lightHealthMod;

    private final AttributeModifier heavySpeedMod;
    private final AttributeModifier heavyHealthMod;
    private final AttributeModifier heavyKbMod;

    @Inject
    public ArmorClassListener(AeroxisPvP plugin, ArmorWeightManager weightManager, CrossplayUtils crossplayUtils) {
        this.weightManager = weightManager;
        this.crossplayUtils = crossplayUtils;

        this.healthModKey = new NamespacedKey(plugin, "class_health_mod");
        this.speedModKey = new NamespacedKey(plugin, "class_speed_mod");
        this.kbModKey = new NamespacedKey(plugin, "class_kb_mod");

        // ☁️ SETUP LIGERO: +15% Velocidad, -10% Vida
        this.lightSpeedMod = new AttributeModifier(speedModKey, 0.15, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        this.lightHealthMod = new AttributeModifier(healthModKey, -0.10, AttributeModifier.Operation.MULTIPLY_SCALAR_1);

        // 🛡️ SETUP PESADO: -10% Velocidad, +20% Vida, +50% Resistencia al Empuje
        this.heavySpeedMod = new AttributeModifier(speedModKey, -0.10, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        this.heavyHealthMod = new AttributeModifier(healthModKey, 0.20, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        this.heavyKbMod = new AttributeModifier(kbModKey, 0.50, AttributeModifier.Operation.ADD_NUMBER);
    }

    // =========================================================================
    // 🛡️ EVENTOS DE EVALUACIÓN
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArmorChange(PlayerArmorChangeEvent event) {
        evaluateClassSet(event.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        evaluateClassSet(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        activeClasses.remove(event.getPlayer().getUniqueId());
    }

    // =========================================================================
    // ⚙️ MOTOR DE ATRIBUTOS
    // =========================================================================

    private void evaluateClassSet(Player player) {
        ArmorWeightManager.ArmorClass currentClass = weightManager.calculatePlayerClass(player);
        ArmorWeightManager.ArmorClass previousClass = activeClasses.getOrDefault(player.getUniqueId(), null);

        // PREVENCIÓN DE SPAM: Solo actualizamos si cambió realmente de peso de armadura
        if (currentClass == previousClass) return;

        activeClasses.put(player.getUniqueId(), currentClass);
        clearClassModifiers(player);

        applyClassBuffs(player, currentClass);
    }

    private void applyClassBuffs(Player player, ArmorWeightManager.ArmorClass armorClass) {
        AttributeInstance healthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        AttributeInstance speedAttr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        AttributeInstance kbAttr = player.getAttribute(Attribute.KNOCKBACK_RESISTANCE);

        switch (armorClass) {
            case LIGHT:
                if (speedAttr != null) speedAttr.addModifier(lightSpeedMod);
                if (healthAttr != null) healthAttr.addModifier(lightHealthMod);

                crossplayUtils.sendActionBar(player, "&#55FF55☁ Postura de Asesino Ligero");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, 1.5f);
                break;

            case MEDIUM:
                // El Luchador Medio no tiene modificadores, es la base del juego.
                crossplayUtils.sendActionBar(player, "&#FFAA00⚖ Postura de Luchador Equilibrado");
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 0.8f, 1.0f);
                break;

            case HEAVY:
                if (speedAttr != null) speedAttr.addModifier(heavySpeedMod);
                if (healthAttr != null) healthAttr.addModifier(heavyHealthMod);
                if (kbAttr != null) kbAttr.addModifier(heavyKbMod);

                crossplayUtils.sendActionBar(player, "&#FF5555🛡 Postura de Tanque Pesado");
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1.0f, 0.5f);
                break;
        }
    }

    /**
     * Limpia los modificadores antiguos usando las llaves seguras (NamespaceKey)
     * para no interferir con otros plugins (ej. anillos de AeroxisItems).
     */
    private void clearClassModifiers(Player player) {
        AttributeInstance healthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (healthAttr != null) healthAttr.getModifiers().stream().filter(m -> m.getKey().equals(healthModKey)).forEach(healthAttr::removeModifier);

        AttributeInstance speedAttr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttr != null) speedAttr.getModifiers().stream().filter(m -> m.getKey().equals(speedModKey)).forEach(speedAttr::removeModifier);

        AttributeInstance kbAttr = player.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (kbAttr != null) kbAttr.getModifiers().stream().filter(m -> m.getKey().equals(kbModKey)).forEach(kbAttr::removeModifier);
    }
}