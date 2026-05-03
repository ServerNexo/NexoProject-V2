package me.nexo.core.visuals;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

/**
 * 👁️ NexoCore - Motor de Tiers Visuales para Mobs Custom (Arquitectura Enterprise)
 * Rendimiento: Cero Runnable Loops. Solo se activa bajo demanda para Bosses y Némesis.
 */
@Singleton
public class MobVisualManager implements Listener {

    private final NexoCore plugin;
    private final CrossplayUtils crossplayUtils;
    private final NamespacedKey holoKey;
    private final NamespacedKey tierKey;

    @Inject
    public MobVisualManager(NexoCore plugin, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.crossplayUtils = crossplayUtils;

        this.holoKey = new NamespacedKey(plugin, "is_mob_holo");
        this.tierKey = new NamespacedKey(plugin, "mob_tier");
    }

    // ==========================================
    // 🧬 API MANUAL (Solo para Mobs Custom)
    // ==========================================
    /**
     * Llama a este método desde tus clases de Bosses o Némesis al momento de generarlos.
     */
    public void attachCustomHologram(LivingEntity mob, int tier) {
        if (!mob.isValid() || mob.isDead()) return;

        // Guardamos el tier en el mob por si necesitamos leerlo después
        mob.getPersistentDataContainer().set(tierKey, PersistentDataType.INTEGER, tier);

        double maxHealth = mob.getAttribute(Attribute.MAX_HEALTH) != null ? mob.getAttribute(Attribute.MAX_HEALTH).getValue() : 20.0;

        TextDisplay display = mob.getWorld().spawn(mob.getLocation(), TextDisplay.class, holo -> {
            holo.setBillboard(TextDisplay.Billboard.CENTER);
            holo.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            holo.setShadowed(true);

            // TextDisplay interpolation setting (Ayuda a reducir el ghosting visual del cliente)
            holo.setTeleportDuration(0);

            holo.setPersistent(false);
            holo.getPersistentDataContainer().set(holoKey, PersistentDataType.BYTE, (byte) 1);

            // Altura dinámica calculada
            float alturaDinamica = (float) mob.getHeight() + 0.4f;

            Transformation trans = holo.getTransformation();
            trans.getTranslation().set(new Vector3f(0, alturaDinamica, 0));
            holo.setTransformation(trans);
        });

        mob.addPassenger(display);
        actualizarTexto(mob, display, maxHealth, maxHealth, tier);
    }

    // ==========================================
    // ⚔️ ACTUALIZACIÓN DINÁMICA DE DAÑO (Automático)
    // ==========================================
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMobDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity mob) || mob instanceof Player) return;

        // Solo procesamos si el mob es un Jefe Custom (tiene el holograma)
        if (!hasHologram(mob)) return;

        mob.getScheduler().runDelayed(plugin, task -> {
            if (mob.isValid() && !mob.isDead()) {
                procesarActualizacion(mob, mob.getHealth());
            }
        }, null, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMobHeal(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof LivingEntity mob) || mob instanceof Player) return;

        if (!hasHologram(mob)) return;

        mob.getScheduler().runDelayed(plugin, task -> {
            if (mob.isValid() && !mob.isDead()) {
                procesarActualizacion(mob, mob.getHealth());
            }
        }, null, 1L);
    }

    private boolean hasHologram(LivingEntity mob) {
        for (Entity passenger : mob.getPassengers()) {
            if (passenger instanceof TextDisplay display && display.getPersistentDataContainer().has(holoKey)) {
                return true;
            }
        }
        return false;
    }

    private void procesarActualizacion(LivingEntity mob, double currentHealth) {
        for (Entity passenger : mob.getPassengers()) {
            if (passenger instanceof TextDisplay display && display.getPersistentDataContainer().has(holoKey)) {
                int tier = mob.getPersistentDataContainer().getOrDefault(tierKey, PersistentDataType.INTEGER, 1);
                double maxHealth = mob.getAttribute(Attribute.MAX_HEALTH) != null ? mob.getAttribute(Attribute.MAX_HEALTH).getValue() : 20.0;

                actualizarTexto(mob, display, currentHealth, maxHealth, tier);
                break;
            }
        }
    }

    // ==========================================
    // 🎨 RENDERIZADO VISUAL
    // ==========================================
    private void actualizarTexto(LivingEntity mob, TextDisplay display, double currentHealth, double maxHealth, int tier) {
        String colorVida = currentHealth > (maxHealth * 0.5) ? "&#55FF55" : (currentHealth > (maxHealth * 0.2) ? "&#FFAA00" : "&#FF5555");
        String vidaTexto = String.format("%.1f", currentHealth) + " / " + String.format("%.1f", maxHealth);

        Component nombreComp = mob.customName() != null
                ? mob.customName()
                : Component.translatable(mob.getType().translationKey());

        Component textoFinal = crossplayUtils.parseCrossplay(null, "&#E6CCFF[Lv." + tier + "] ")
                .append(nombreComp)
                .append(Component.newline())
                .append(crossplayUtils.parseCrossplay(null, colorVida + "❤ " + vidaTexto));

        display.text(textoFinal);
    }

    // ==========================================
    // 🧹 LIMPIEZA DE MEMORIA RAM
    // ==========================================
    @EventHandler
    public void onMobRemove(EntityRemoveEvent event) {
        if (!(event.getEntity() instanceof LivingEntity mob) || mob instanceof Player) return;

        for (Entity passenger : mob.getPassengers()) {
            if (passenger instanceof TextDisplay display && display.getPersistentDataContainer().has(holoKey)) {
                display.remove();
            }
        }
    }
}