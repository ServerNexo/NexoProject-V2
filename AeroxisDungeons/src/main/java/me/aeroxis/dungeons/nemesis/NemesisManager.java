package me.aeroxis.dungeons.nemesis;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.core.AeroxisCore;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * ☠️ AeroxisDungeons - Nemesis Manager
 * Gestiona el ciclo de vida, la mutación y el almacenamiento en PDC de los monstruos.
 */
@Singleton
public class NemesisManager {

    private final AeroxisCore core;
    private final NamespacedKey NEMESIS_KEY;
    private final NamespacedKey LOOT_KEY;
    private final NamespacedKey KILLS_KEY;

    @Inject
    public NemesisManager(AeroxisCore core) {
        this.core = core;
        // Llaves para el PersistentDataContainer (PDC)
        this.NEMESIS_KEY = new NamespacedKey("nexo", "is_nemesis");
        this.LOOT_KEY = new NamespacedKey("nexo", "nemesis_loot");
        this.KILLS_KEY = new NamespacedKey("nexo", "nemesis_kills");
    }

    /**
     * Muta a un monstruo ordinario convirtiéndolo en un Némesis.
     */
    public void mutateMob(LivingEntity mob, String playerName, String serializedLoot) {

        // 1. Marcar como Némesis Activo
        mob.getPersistentDataContainer().set(NEMESIS_KEY, PersistentDataType.STRING, "ACTIVE");

        // 2. Guardar el Loot robado serializado (Base64) en su ADN
        mob.getPersistentDataContainer().set(LOOT_KEY, PersistentDataType.STRING, serializedLoot);

        // 3. Sistema Anti-Exploits: Registrar cuántas kills lleva este Némesis
        int currentKills = mob.getPersistentDataContainer().getOrDefault(KILLS_KEY, PersistentDataType.INTEGER, 0);
        mob.getPersistentDataContainer().set(KILLS_KEY, PersistentDataType.INTEGER, currentKills + 1);

        // 4. Persistencia Forzada (Evita que el mob desaparezca si el jugador se aleja)
        mob.setRemoveWhenFarAway(false);
        mob.setPersistent(true);

        // 5. Mutación Visual (Nombre en rojo, negrita, usando MiniMessage)
        mob.customName(MiniMessage.miniMessage().deserialize("<red><bold>☠ Némesis de " + playerName + "</bold></red>"));
        mob.setCustomNameVisible(true);

        // 6. Mutación de Stats (Doble Vida Máxima)
        AttributeInstance healthAttr = mob.getAttribute(Attribute.MAX_HEALTH);
        if (healthAttr != null) {
            // Multiplicamos su vida base x2
            double newMaxHealth = healthAttr.getBaseValue() * 2.0;
            healthAttr.setBaseValue(newMaxHealth);
            mob.setHealth(newMaxHealth); // Lo curamos al máximo
        }

        // Mutación de Stats (Aumento de Daño)
        AttributeInstance damageAttr = mob.getAttribute(Attribute.ATTACK_DAMAGE);
        if (damageAttr != null) {
            damageAttr.setBaseValue(damageAttr.getBaseValue() * 1.5);
        }
    }

    /**
     * Verifica si una entidad es un Némesis.
     */
    public boolean isNemesis(LivingEntity mob) {
        return mob.getPersistentDataContainer().has(NEMESIS_KEY, PersistentDataType.STRING);
    }

    /**
     * Devuelve la llave necesaria para extraer el botín.
     */
    public NamespacedKey getLootKey() {
        return LOOT_KEY;
    }

    /**
     * Devuelve cuántos jugadores ha matado este Némesis (Útil para evitar granjas tóxicas).
     */
    public int getKillCount(LivingEntity mob) {
        return mob.getPersistentDataContainer().getOrDefault(KILLS_KEY, PersistentDataType.INTEGER, 0);
    }
}