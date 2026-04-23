package me.nexo.items.mecanicas;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.NexoUser;
import me.nexo.core.user.UserManager; // Asumido desde el Core
import me.nexo.items.NexoItems;
import me.nexo.items.dtos.WeaponDTO;
import me.nexo.items.managers.FileManager;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 🎒 NexoItems - Gestor de Habilidades y Artefactos (Arquitectura Enterprise Java 21)
 * Rendimiento: Folia Region Scheduler, Cero Estáticos y Prevención de Ghost Items.
 */
@Singleton
public class InteractListener implements Listener {

    // 🌟 DEPENDENCIAS PROPAGADAS
    private final NexoItems plugin;
    private final FileManager fileManager;
    private final UserManager userManager;
    private final CrossplayUtils crossplayUtils;

    // Caché de enfriamientos
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    // 🌟 LLAVES DESACOPLADAS O(1)
    private final NamespacedKey soulboundKey;
    private final NamespacedKey weaponIdKey;

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public InteractListener(NexoItems plugin, FileManager fileManager, UserManager userManager, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.fileManager = fileManager;
        this.userManager = userManager;
        this.crossplayUtils = crossplayUtils;
        
        // Instanciadas en constructor para total compatibilidad sin abusar de static
        this.soulboundKey = new NamespacedKey("nexoitems", "soulbound");
        this.weaponIdKey = new NamespacedKey("nexoitems", "weapon_id");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void alInteractuar(PlayerInteractEvent event) {
        // 🌟 PAPER NATIVE: Método unificado más rápido y limpio
        if (!event.getAction().isRightClick()) return;

        var jugador = event.getPlayer();
        var arma = jugador.getInventory().getItemInMainHand();

        // 🌟 GHOST-ITEM PROOF
        if (arma == null || arma.isEmpty() || !arma.hasItemMeta()) return;
        var pdc = arma.getItemMeta().getPersistentDataContainer();

        // 🌌 ARTEFACTOS (Hoja del Vacío)
        if (pdc.has(soulboundKey, PersistentDataType.BYTE)) {
            if (arma.getType() == org.bukkit.Material.DIAMOND_SWORD) {
                ejecutarHabilidad(jugador, "traslacion", 40, 3000);
            }
            return;
        }

        // ⚔️ ARMAS RPG
        if (pdc.has(weaponIdKey, PersistentDataType.STRING)) {
            String idArma = pdc.get(weaponIdKey, PersistentDataType.STRING);
            WeaponDTO dto = fileManager.getWeaponDTO(idArma);

            if (dto != null && !dto.habilidadId().equalsIgnoreCase("ninguna")) {
                int costoEnergia = 20;
                int cooldownMs = 2000;

                // Definición de balances de Habilidades usando Switch Expressions de Java 21
                switch (dto.habilidadId().toLowerCase()) {
                    case "quake", "ola", "rafaga" -> { costoEnergia = 20; cooldownMs = 3000; }
                    case "tajo_sanguinario", "agujero_negro" -> { costoEnergia = 40; cooldownMs = 5000; }
                    case "supernova", "juicio_sangre", "corte_umbral" -> { costoEnergia = 70; cooldownMs = 8000; }
                }

                ejecutarHabilidad(jugador, dto.habilidadId(), costoEnergia, cooldownMs);
            }
        }
    }

    private void ejecutarHabilidad(Player jugador, String habilidad, int costoEnergia, int cooldownMs) {
        UUID uuid = jugador.getUniqueId();
        long ahora = System.currentTimeMillis();

        if (cooldowns.containsKey(uuid) && (ahora - cooldowns.get(uuid)) < cooldownMs) {
            long faltan = (cooldownMs - (ahora - cooldowns.get(uuid))) / 1000;
            crossplayUtils.sendActionBar(jugador, "&#FF5555❄ Enfriamiento de Sistema: " + faltan + "s");
            return;
        }

        // 🌟 USO DE DEPENDENCIA INYECTADA (Cero Service Locators)
        NexoUser user = userManager.getUserLocal(uuid);
        if (user == null) {
            crossplayUtils.sendMessage(jugador, "&#FF5555[!] Sincronizando interfaz neuronal. Espera...");
            return;
        }

        int energiaActual = user.getEnergiaMineria();
        if (energiaActual < costoEnergia) {
            crossplayUtils.sendActionBar(jugador, "&#FF5555⚡ Energía Insuficiente (" + costoEnergia + " requeridos)");
            jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
            return;
        }

        boolean exito = false;
        Location loc = jugador.getLocation();

        switch (habilidad.toLowerCase()) {
            // ==========================================
            // 🛡️ HABILIDADES TIER 1
            // ==========================================
            case "quake":
                jugador.getWorld().spawnParticle(Particle.EXPLOSION, loc, 3);
                jugador.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
                for (var e : jugador.getNearbyEntities(5, 3, 5)) {
                    if (e instanceof LivingEntity vivo && e != jugador) {
                        vivo.damage(15.0, jugador);
                        vivo.setVelocity(new Vector(0, 0.8, 0));
                    }
                }
                exito = true;
                break;

            case "ola":
                jugador.getWorld().spawnParticle(Particle.SPLASH, loc.add(0, 1, 0), 100, 2, 0.5, 2, 0.1);
                jugador.playSound(loc, Sound.ENTITY_DOLPHIN_SPLASH, 1f, 1f);
                Vector direccionOla = loc.getDirection().multiply(1.5);
                for (var e : jugador.getNearbyEntities(6, 2, 6)) {
                    if (e instanceof LivingEntity vivo && e != jugador) {
                        vivo.damage(10.0, jugador);
                        vivo.setVelocity(direccionOla);
                    }
                }
                exito = true;
                break;

            case "rafaga":
                jugador.playSound(loc, Sound.ENTITY_ARROW_SHOOT, 1f, 1f);
                for (int i = 0; i < 5; i++) {
                    // 🛡️ FOLIA SYNC: Generar entidades en la Región del jugador
                    jugador.getScheduler().runDelayed(plugin, task -> {
                        var flecha = jugador.launchProjectile(org.bukkit.entity.Arrow.class);
                        flecha.setVelocity(jugador.getLocation().getDirection().multiply(2.5));
                        flecha.setPickupStatus(org.bukkit.entity.AbstractArrow.PickupStatus.DISALLOWED);
                    }, null, i * 3L);
                }
                exito = true;
                break;

            // ==========================================
            // 🔥 HABILIDADES TIER 2
            // ==========================================
            case "tajo_sanguinario":
                jugador.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.5f);
                jugador.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc.add(loc.getDirection().multiply(1.5)).add(0,1,0), 3);

                var polvoRojo = new Particle.DustOptions(org.bukkit.Color.RED, 2.0F);
                jugador.getWorld().spawnParticle(Particle.DUST, loc.add(0,1,0), 30, 1.5, 0.5, 1.5, polvoRojo);

                double curacionTotal = 0;
                for (var e : jugador.getNearbyEntities(4, 2, 4)) {
                    if (e instanceof LivingEntity vivo && e != jugador) {
                        vivo.damage(25.0, jugador);
                        curacionTotal += 5.0; // Se cura 5HP por cada enemigo golpeado
                    }
                }
                if (curacionTotal > 0) {
                    var hpAttr = jugador.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                    if(hpAttr != null) {
                        double maxHp = hpAttr.getValue();
                        jugador.setHealth(Math.min(maxHp, jugador.getHealth() + curacionTotal));
                        jugador.getWorld().spawnParticle(Particle.HEART, jugador.getLocation().add(0, 2, 0), (int) curacionTotal);
                    }
                }
                exito = true;
                break;

            case "agujero_negro":
                Location centro = loc.clone().add(loc.getDirection().multiply(6)).add(0, 1, 0); // Clone para no mutar loc base
                jugador.playSound(centro, Sound.BLOCK_PORTAL_TRAVEL, 0.5f, 2f);
                jugador.getWorld().spawnParticle(Particle.PORTAL, centro, 150, 2, 2, 2, 0.5);

                for (var e : centro.getWorld().getNearbyEntities(centro, 7, 7, 7)) {
                    if (e instanceof LivingEntity vivo && e != jugador) {
                        Vector atraccion = centro.toVector().subtract(vivo.getLocation().toVector()).normalize().multiply(1.2);
                        vivo.setVelocity(atraccion);
                        vivo.damage(15.0, jugador);
                        vivo.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2));
                    }
                }
                exito = true;
                break;

            // ==========================================
            // ☄️ HABILIDADES TIER 3
            // ==========================================
            case "supernova":
                jugador.playSound(loc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1f, 0.8f);
                jugador.getWorld().spawnParticle(Particle.FLAME, loc.add(0, 1, 0), 200, 4, 1, 4, 0.2);
                jugador.getWorld().spawnParticle(Particle.LAVA, loc, 50, 4, 1, 4);

                for (var e : jugador.getNearbyEntities(7, 4, 7)) {
                    if (e instanceof LivingEntity vivo && e != jugador) {
                        vivo.damage(45.0, jugador);
                        vivo.setFireTicks(100);
                        vivo.setVelocity(new Vector(0, 1.2, 0));
                    }
                }
                exito = true;
                break;

            case "juicio_sangre":
                jugador.playSound(loc, Sound.ITEM_TRIDENT_THUNDER, 1f, 1f);
                jugador.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.add(0, 1, 0), 100, 5, 1, 5, 0.1);

                var enemigos = jugador.getNearbyEntities(8, 5, 8);
                if (!enemigos.isEmpty()) {
                    for (var e : enemigos) {
                        if (e instanceof LivingEntity vivo && e != jugador) {
                            jugador.getWorld().strikeLightningEffect(vivo.getLocation());
                            vivo.damage(50.0, jugador);
                        }
                    }
                    exito = true;
                } else {
                    crossplayUtils.sendMessage(jugador, "&#FF5555[!] Sin objetivos hostiles válidos en el rango de Juicio.");
                }
                break;

            case "corte_umbral":
                jugador.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.5f);
                Vector dash = loc.getDirection().normalize().multiply(3.0);
                jugador.setVelocity(dash);

                // 🛡️ FOLIA SYNC: Dañar entidades tras el dash en la región del jugador
                jugador.getScheduler().runDelayed(plugin, task -> {
                    jugador.getWorld().spawnParticle(Particle.LARGE_SMOKE, jugador.getLocation().add(0, 1, 0), 50, 1, 1, 1, 0);
                    for (var e : jugador.getNearbyEntities(3, 2, 3)) {
                        if (e instanceof LivingEntity vivo && e != jugador) {
                            vivo.damage(60.0, jugador);
                            vivo.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 1));
                        }
                    }
                }, null, 5L);
                exito = true;
                break;

            // ==========================================
            // 🌀 HABILIDADES DE ARTEFACTO (Utilidad)
            // ==========================================
            case "traslacion":
                var bloqueMirado = jugador.getTargetBlockExact(15);
                if (bloqueMirado != null && !bloqueMirado.isEmpty() && bloqueMirado.getType().isSolid()) {
                    Location destino = bloqueMirado.getLocation().add(0.5, 1, 0.5);
                    destino.setYaw(jugador.getLocation().getYaw());
                    destino.setPitch(jugador.getLocation().getPitch());

                    jugador.getWorld().spawnParticle(Particle.PORTAL, loc, 30);
                    // Uso seguro de teleport
                    jugador.teleportAsync(destino).thenAccept(success -> {
                        if (success) {
                            jugador.getWorld().spawnParticle(Particle.PORTAL, destino, 30);
                            jugador.playSound(destino, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                        }
                    });
                    exito = true;
                } else {
                    crossplayUtils.sendMessage(jugador, "&#FF5555[!] Destino inválido para salto traslacional.");
                }
                break;
        }

        if (exito) {
            user.setEnergiaMineria(Math.max(0, energiaActual - costoEnergia));
            cooldowns.put(uuid, ahora);
            crossplayUtils.sendActionBar(jugador, "&#00E5FF✨ Habilidad Desplegada: &#FFFFFF" + habilidad.toUpperCase() + " &#555555(-" + costoEnergia + "⚡)");
        }
    }

    // 🧹 Limpieza automática O(1)
    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        cooldowns.remove(event.getPlayer().getUniqueId());
    }
}