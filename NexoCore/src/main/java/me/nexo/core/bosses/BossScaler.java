package me.nexo.core.bosses;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * ⚖️ NexoBossScaler - Ajusta las estadísticas del Jefe según el Gear Score del grupo.
 */
public class BossScaler {

    private final JavaPlugin plugin;

    public BossScaler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Aplica los modificadores nativos de Paper al Jefe.
     * @param boss La entidad física del Jefe.
     * @param groupGearScore El promedio del nivel de equipo de los jugadores en la mazmorra.
     */
    public void scaleAttributes(Mob boss, int groupGearScore) {
        // Cálculo matemático simple: 10% más de vida y daño por cada 10 puntos de Gear Score.
        double multiplier = 1.0 + (groupGearScore / 100.0);

        // 🛡️ Escalar Vida Máxima
        var maxHealthAttr = boss.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttr != null) {
            double newHealth = maxHealthAttr.getBaseValue() * multiplier;
            maxHealthAttr.setBaseValue(newHealth);
            boss.setHealth(newHealth); // Curamos al jefe a su nueva vida máxima
        }

        // ⚔️ Escalar Daño de Ataque
        var attackAttr = boss.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attackAttr != null) {
            NamespacedKey key = new NamespacedKey(plugin, "nexo_boss_damage");
            // Limpiamos modificadores viejos si existen
            attackAttr.getModifiers().forEach(mod -> {
                if (mod.getKey().equals(key)) attackAttr.removeModifier(mod);
            });
            
            // Aplicamos el nuevo daño extra (Operación ADD_SCALAR añade un porcentaje)
            AttributeModifier damageModifier = new AttributeModifier(
                    key,
                    multiplier - 1.0, 
                    AttributeModifier.Operation.ADD_SCALAR
            );
            attackAttr.addModifier(damageModifier);
        }
    }
}