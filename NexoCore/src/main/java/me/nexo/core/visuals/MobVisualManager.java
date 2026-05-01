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
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

/**
 * 👁️ NexoCore - Motor de Tiers Visuales para Mobs (Arquitectura Enterprise)
 * Rendimiento: Cero Runnable Loops, Movimiento Nativo (Passenger) y Text Displays O(1).
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
    // 🧬 GENERACIÓN DEL HOLOGRAMA (SPAWN)
    // ==========================================
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMobSpawn(CreatureSpawnEvent event) {
        LivingEntity mob = event.getEntity();
        if (mob instanceof Player) return;

        mob.getScheduler().run(plugin, task -> {
            crearHolograma(mob);
        }, null);
    }

    private void crearHolograma(LivingEntity mob) {
        if (!mob.isValid() || mob.isDead()) return;

        int tier = mob.getPersistentDataContainer().getOrDefault(tierKey, PersistentDataType.INTEGER, 1);
        double maxHealth = mob.getAttribute(Attribute.MAX_HEALTH) != null ? mob.getAttribute(Attribute.MAX_HEALTH).getValue() : 20.0;

        TextDisplay display = mob.getWorld().spawn(mob.getLocation(), TextDisplay.class, holo -> {
            holo.setBillboard(TextDisplay.Billboard.CENTER);
            holo.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            holo.setShadowed(true);

            // 🌟 BUG 1 FIX: Evita que los hologramas se guarden en el disco y queden flotando en reinicios
            holo.setPersistent(false);

            holo.getPersistentDataContainer().set(holoKey, PersistentDataType.BYTE, (byte) 1);

            // 🌟 BUG 2 FIX: Como el holograma ya es pasajero, ya está en la cabeza del mob.
            // Solo necesitamos un micro-ajuste de 0.35 para que no se meta en su modelo.
            Transformation trans = holo.getTransformation();
            trans.getTranslation().set(new Vector3f(0, 0.35f, 0));
            holo.setTransformation(trans);
        });

        mob.addPassenger(display);
        actualizarTexto(mob, display, maxHealth, maxHealth, tier);
    }

    // ==========================================
    // ⚔️ ACTUALIZACIÓN DINÁMICA DE DAÑO
    // ==========================================
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMobDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity mob) || mob instanceof Player) return;

        // 🌟 FIX: Esperamos 1 tick (runDelayed) para que Minecraft aplique todo el daño real,
        // escudos, armaduras y cálculos de otros plugins. Así leemos la vida EXACTA real.
        mob.getScheduler().runDelayed(plugin, task -> {
            if (mob.isValid() && !mob.isDead()) {
                procesarActualizacion(mob, mob.getHealth());
            }
        }, null, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMobHeal(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof LivingEntity mob) || mob instanceof Player) return;

        // 🌟 FIX: Igual con la curación, leemos el resultado 1 tick después.
        mob.getScheduler().runDelayed(plugin, task -> {
            if (mob.isValid() && !mob.isDead()) {
                procesarActualizacion(mob, mob.getHealth());
            }
        }, null, 1L);
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

        // 🌟 BUG 3 FIX: Traducción Nativa del Cliente
        Component nombreComp = mob.customName() != null
                ? mob.customName()
                : Component.translatable(mob.getType().translationKey());

        // Ensamblamos el componente respetando los colores y la traducción nativa
        Component textoFinal = crossplayUtils.parseCrossplay(null, "&#E6CCFF[Lv." + tier + "] ")
                .append(nombreComp)
                .append(Component.newline())
                .append(crossplayUtils.parseCrossplay(null, colorVida + "❤ " + vidaTexto));

        display.text(textoFinal);
    }

    // ==========================================
    // 🧹 LIMPIEZA DE MEMORIA RAM
    // ==========================================
    // 🌟 BUG 1 FIX (Parte 2): Usamos EntityRemoveEvent en lugar de EntityDeathEvent
    // Esto captura cuando el mob muere, pero TAMBIÉN cuando despawnea o el chunk se descarga.
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