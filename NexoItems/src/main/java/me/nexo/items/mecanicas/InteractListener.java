package me.nexo.items.mecanicas;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.NexoAPI;
import me.nexo.core.user.NexoUser;
import me.nexo.items.NexoItems;
import me.nexo.items.dtos.WeaponDTO;
import me.nexo.items.managers.FileManager;
import me.nexo.items.managers.ItemManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * 🎒 NexoItems - Gestor de Habilidades y Artefactos (Arquitectura Enterprise)
 */
@Singleton
public class InteractListener implements Listener {

    private final NexoItems plugin;
    private final FileManager fileManager;

    // Caché de enfriamientos
    private final HashMap<UUID, Long> cooldowns = new HashMap<>();

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public InteractListener(NexoItems plugin) {
        this.plugin = plugin;
        this.fileManager = plugin.getFileManager();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void alInteractuar(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player jugador = event.getPlayer();
        ItemStack arma = jugador.getInventory().getItemInMainHand();

        if (arma.getType() == org.bukkit.Material.AIR || !arma.hasItemMeta()) return;
        var pdc = arma.getItemMeta().getPersistentDataContainer();

        // 🌌 ARTEFACTOS (Hoja del Vacío)
        if (pdc.has(ItemManager.llaveSoulbound, PersistentDataType.BYTE)) {
            if (arma.getType() == org.bukkit.Material.DIAMOND_SWORD) {
                ejecutarHabilidad(jugador, "traslacion", 40, 3000);
            }
            return;
        }

        // ⚔️ ARMAS RPG
        if (pdc.has(ItemManager.llaveWeaponId, PersistentDataType.STRING)) {
            String idArma = pdc.get(ItemManager.llaveWeaponId, PersistentDataType.STRING);
            WeaponDTO dto = fileManager.getWeaponDTO(idArma);

            if (dto != null && !dto.habilidadId().equalsIgnoreCase("ninguna")) {
                int costoEnergia = 20;
                int cooldownMs = 2000;

                // Definición de balances de Habilidades
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
            CrossplayUtils.sendActionBar(jugador, "&#FF5555❄ Enfriamiento de Sistema: " + faltan + "s");
            return;
        }

        NexoUser user = NexoAPI.getInstance().getUserLocal(uuid);
        if (user == null) {
            CrossplayUtils.sendMessage(jugador, "&#FF5555[!] Sincronizando interfaz neuronal. Espera...");
            return;
        }

        int energiaActual = user.getEnergiaMineria();
        if (energiaActual < costoEnergia) {
            CrossplayUtils.sendActionBar(jugador, "&#FF5555⚡ Energía Insuficiente (" + costoEnergia + " requeridos)");
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
                for (Entity e : jugador.getNearbyEntities(5, 3, 5)) {
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
                for (Entity e : jugador.getNearbyEntities(6, 2, 6)) {
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
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        org.bukkit.entity.Arrow flecha = jugador.launchProjectile(org.bukkit.entity.Arrow.class);
                        flecha.setVelocity(jugador.getLocation().getDirection().multiply(2.5));
                        flecha.setPickupStatus(org.bukkit.entity.AbstractArrow.PickupStatus.DISALLOWED);
                    }, i * 3L);
                }
                exito = true;
                break;

            // ==========================================
            // 🔥 HABILIDADES TIER 2
            // ==========================================
            case "tajo_sanguinario":
                jugador.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.5f);
                jugador.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc.add(loc.getDirection().multiply(1.5)).add(0,1,0), 3);

                // 🌟 FIX de Partículas para 1.20+
                Particle.DustOptions polvoRojo = new Particle.DustOptions(org.bukkit.Color.RED, 2.0F);
                jugador.getWorld().spawnParticle(Particle.DUST, loc.add(0,1,0), 30, 1.5, 0.5, 1.5, polvoRojo);

                double curacionTotal = 0;
                for (Entity e : jugador.getNearbyEntities(4, 2, 4)) {
                    if (e instanceof LivingEntity vivo && e != jugador) {
                        vivo.damage(25.0, jugador);
                        curacionTotal += 5.0; // Se cura 5HP por cada enemigo golpeado
                    }
                }
                if (curacionTotal > 0) {
                    AttributeInstance hpAttr = jugador.getAttribute(Attribute.MAX_HEALTH);
                    if(hpAttr != null) {
                        double maxHp = hpAttr.getValue();
                        jugador.setHealth(Math.min(maxHp, jugador.getHealth() + curacionTotal));
                        jugador.getWorld().spawnParticle(Particle.HEART, jugador.getLocation().add(0, 2, 0), (int) curacionTotal);
                    }
                }
                exito = true;
                break;

            case "agujero_negro":
                Location centro = loc.add(loc.getDirection().multiply(6)).add(0, 1, 0);
                jugador.playSound(centro, Sound.BLOCK_PORTAL_TRAVEL, 0.5f, 2f);
                jugador.getWorld().spawnParticle(Particle.PORTAL, centro, 150, 2, 2, 2, 0.5);

                for (Entity e : centro.getWorld().getNearbyEntities(centro, 7, 7, 7)) {
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

                for (Entity e : jugador.getNearbyEntities(7, 4, 7)) {
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

                List<Entity> enemigos = jugador.getNearbyEntities(8, 5, 8);
                if (!enemigos.isEmpty()) {
                    for (Entity e : enemigos) {
                        if (e instanceof LivingEntity vivo && e != jugador) {
                            jugador.getWorld().strikeLightningEffect(vivo.getLocation());
                            vivo.damage(50.0, jugador);
                        }
                    }
                    exito = true;
                } else {
                    CrossplayUtils.sendMessage(jugador, "&#FF5555[!] Sin objetivos hostiles válidos en el rango de Juicio.");
                }
                break;

            case "corte_umbral":
                jugador.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.5f);
                Vector dash = loc.getDirection().normalize().multiply(3.0);
                jugador.setVelocity(dash);

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    jugador.getWorld().spawnParticle(Particle.LARGE_SMOKE, jugador.getLocation().add(0, 1, 0), 50, 1, 1, 1, 0);
                    for (Entity e : jugador.getNearbyEntities(3, 2, 3)) {
                        if (e instanceof LivingEntity vivo && e != jugador) {
                            vivo.damage(60.0, jugador);
                            vivo.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 1));
                        }
                    }
                }, 5L);
                exito = true;
                break;

            // ==========================================
            // 🌀 HABILIDADES DE ARTEFACTO (Utilidad)
            // ==========================================
            case "traslacion":
                Block bloqueMirado = jugador.getTargetBlockExact(15);
                if (bloqueMirado != null && bloqueMirado.getType().isSolid()) {
                    Location destino = bloqueMirado.getLocation().add(0.5, 1, 0.5);
                    destino.setYaw(jugador.getLocation().getYaw());
                    destino.setPitch(jugador.getLocation().getPitch());

                    jugador.getWorld().spawnParticle(Particle.PORTAL, loc, 30);
                    jugador.teleport(destino);
                    jugador.getWorld().spawnParticle(Particle.PORTAL, destino, 30);
                    jugador.playSound(destino, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                    exito = true;
                } else {
                    CrossplayUtils.sendMessage(jugador, "&#FF5555[!] Destino inválido para salto traslacional.");
                }
                break;
        }

        if (exito) {
            user.setEnergiaMineria(Math.max(0, energiaActual - costoEnergia)); // Asumo que "EnergiaMineria" es el stat global de Maná
            cooldowns.put(uuid, ahora);
            CrossplayUtils.sendActionBar(jugador, "&#00E5FF✨ Habilidad Desplegada: &#FFFFFF" + habilidad.toUpperCase() + " &#555555(-" + costoEnergia + "⚡)");
        }
    }

    // 🧹 Limpieza automática para que el mapa no se llene de basura en la memoria
    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        cooldowns.remove(event.getPlayer().getUniqueId());
    }
}