package me.nexo.items.artefactos;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.NexoUser;
import me.nexo.core.user.UserManager; // Asumido desde el Core
import me.nexo.items.NexoItems;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Item;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 🎒 NexoItems - Manager de Habilidades (Arquitectura Enterprise Java 21)
 * Rendimiento: Folia Entity/Region Schedulers, Thread-Safe Maps y Cero Estáticos.
 */
@Singleton
public class ArtefactoManager {

    // 🌟 DEPENDENCIAS PROPAGADAS
    private final NexoItems plugin;
    private final UserManager userManager;
    private final CrossplayUtils crossplayUtils;

    private final Map<String, ArtefactoStrategy> estrategias = new HashMap<>();
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    public final Set<UUID> invulnerables = ConcurrentHashMap.newKeySet();
    public final Set<UUID> alasActivas = ConcurrentHashMap.newKeySet();

    // 💉 PILAR 1: Inyección Estricta
    @Inject
    public ArtefactoManager(NexoItems plugin, UserManager userManager, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.userManager = userManager;
        this.crossplayUtils = crossplayUtils;
        
        registrarEstrategias();
    }

    public boolean procesarUso(Player p, ArtefactoDTO dto) {
        UUID uuid = p.getUniqueId();
        long ahora = System.currentTimeMillis();

        var playerCds = cooldowns.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());

        if (playerCds.containsKey(dto.id()) && playerCds.get(dto.id()) > ahora) {
            double restante = (playerCds.get(dto.id()) - ahora) / 1000.0;
            crossplayUtils.sendActionBar(p, "&#8b0000❄ Enfriamiento Táctico: " + String.format("%.1f", restante) + "s");
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return false;
        }

        NexoUser user = userManager.getUserLocal(uuid);
        if (user == null) {
            crossplayUtils.sendMessage(p, "&#8b0000[!] Sincronizando datos neuronales. Espera...");
            return false;
        }

        int maxEnergia = 100 + ((user.getNexoNivel() - 1) * 20) + user.getEnergiaExtraAccesorios();
        int energiaActual = user.getEnergiaMineria();
        int costoFinal = dto.cost();

        if (dto.id().equals("orbe_sobrecarga")) {
            costoFinal = (int) (maxEnergia * (dto.cost() / 100.0));
        }

        if (energiaActual < costoFinal) {
            crossplayUtils.sendActionBar(p, "&#8b0000⚡ Energía Insuficiente (" + energiaActual + "/" + costoFinal + ")");
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
            return false;
        }

        ArtefactoStrategy estrategia = estrategias.get(dto.id());
        if (estrategia == null) {
            crossplayUtils.sendMessage(p, "&#8b0000[!] Error de Firmware: Esta habilidad aún no está programada.");
            return false;
        }

        if (estrategia.ejecutar(p, dto)) {
            user.setEnergiaMineria(Math.max(0, energiaActual - costoFinal));

            if (dto.type() != ArtefactoDTO.HabilidadType.TOGGLE) {
                playerCds.put(dto.id(), ahora + (dto.cooldown() * 1000L));
            }
            return true;
        }
        return false;
    }

    public void limpiarCooldowns(UUID uuid) {
        cooldowns.remove(uuid);
    }

    /**
     * 💡 Nota del Arquitecto: Para una migración 100% limpia a futuro, estas lambdas deberían
     * separarse en sus propias clases (Ej. `GanchoCobreStrategy`) e inyectarse vía MapBinder en Guice.
     * Por ahora, se refactorizó su lógica interna para cumplir con Java 21 y Folia.
     */
    private void registrarEstrategias() {

        estrategias.put("gancho_cobre", (p, dto) -> {
            p.setVelocity(p.getLocation().getDirection().multiply(1.8).setY(1.2));
            p.playSound(p.getLocation(), Sound.ITEM_CROSSBOW_SHOOT, 1f, 1f);
            return true;
        });

        estrategias.put("totem_crecimiento", (p, dto) -> {
            Block centro = p.getLocation().getBlock();
            int aplicados = 0;
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    Block b = centro.getRelative(x, 0, z);
                    if (b.getBlockData() instanceof Ageable) {
                        b.applyBoneMeal(BlockFace.UP);
                        aplicados++;
                    }
                }
            }
            p.playSound(p.getLocation(), Sound.ITEM_BONE_MEAL_USE, 1f, 0.8f);
            p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, p.getLocation(), 30, 2, 0.5, 2);
            return aplicados > 0;
        });

        estrategias.put("iman_chatarra", (p, dto) -> {
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 2f);
            p.getNearbyEntities(15, 15, 15).forEach(e -> {
                if (e instanceof Item) {
                    Vector pull = p.getLocation().toVector().subtract(e.getLocation().toVector()).normalize().multiply(1.5);
                    e.setVelocity(pull);
                }
            });
            return true;
        });

        estrategias.put("hoja_vacio", (p, dto) -> {
            RayTraceResult ray = p.getWorld().rayTraceBlocks(p.getEyeLocation(), p.getEyeLocation().getDirection(), 8, FluidCollisionMode.NEVER, true);
            Location target = (ray != null && ray.getHitBlock() != null)
                    ? ray.getHitBlock().getLocation()
                    : p.getLocation().add(p.getLocation().getDirection().multiply(8));

            target.setYaw(p.getYaw());
            target.setPitch(p.getPitch());

            p.teleportAsync(target);
            p.playSound(p.getLocation(), Sound.ITEM_CHORUS_FRUIT_TELEPORT, 1f, 1f);
            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0));
            return true;
        });

        estrategias.put("vara_florbifida", (p, dto) -> {
            RayTraceResult ray = p.getWorld().rayTraceBlocks(p.getEyeLocation(), p.getEyeLocation().getDirection(), 15, FluidCollisionMode.NEVER, true);
            Location impacto = (ray != null && ray.getHitBlock() != null) ? ray.getHitBlock().getLocation() : p.getLocation().add(p.getLocation().getDirection().multiply(15));

            p.getWorld().spawnParticle(Particle.HEART, impacto, 20, 2, 1, 2);
            p.playSound(impacto, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 0.5f);

            // 🌟 FIX PAPER 1.21: Attribute.MAX_HEALTH
            var pAttr = p.getAttribute(Attribute.MAX_HEALTH);
            if (pAttr != null) {
                double healAmount = pAttr.getValue() * 0.05;
                p.setHealth(Math.min(pAttr.getValue(), p.getHealth() + healAmount));

                impacto.getWorld().getNearbyEntities(impacto, 4, 4, 4).forEach(e -> {
                    if (e instanceof Player ally && ally != p) {
                        var aAttr = ally.getAttribute(Attribute.MAX_HEALTH);
                        if (aAttr != null) ally.setHealth(Math.min(aAttr.getValue(), ally.getHealth() + healAmount));
                    }
                });
            }
            return true;
        });

        estrategias.put("cetro_glacial", (p, dto) -> {
            p.playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 1f);
            p.getWorld().spawnParticle(Particle.SNOWFLAKE, p.getLocation().add(0,1,0), 100, 2, 1, 2, 0.1);

            p.getNearbyEntities(6, 4, 6).forEach(e -> {
                if (e instanceof Monster mob && p.hasLineOfSight(e)) {
                    mob.setAware(false);
                    mob.getWorld().spawnParticle(Particle.SNOWFLAKE, mob.getLocation(), 20);
                    // 🛡️ FOLIA SYNC: Entity Scheduler
                    mob.getScheduler().runDelayed(plugin, task -> mob.setAware(true), null, 60L);
                }
            });
            return true;
        });

        estrategias.put("pico_enano", (p, dto) -> {
            Block centro = p.getTargetBlockExact(5);
            if (centro == null || centro.isEmpty() || !centro.getType().isSolid()) return false;

            p.playSound(centro.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1f, 0.5f);
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        Block b = centro.getRelative(x, y, z);
                        String typeName = b.getType().name();
                        if (typeName.contains("STONE") || typeName.contains("ORE")) {
                            b.breakNaturally(p.getInventory().getItemInMainHand(), true, true);
                        }
                    }
                }
            }
            return true;
        });

        estrategias.put("orbe_sobrecarga", (p, dto) -> {
            Location spawnLoc = p.getLocation().add(0, 2, 0);
            
            // Spawn Native Consumer de Paper
            p.getWorld().spawn(spawnLoc, ArmorStand.class, orbe -> {
                orbe.setInvisible(true);
                orbe.setGravity(false);
                orbe.setMarker(true);
                orbe.getEquipment().setHelmet(new ItemStack(org.bukkit.Material.BEACON));
                
                p.playSound(spawnLoc, Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1f);

                AtomicInteger tiempo = new AtomicInteger(30);
                
                // 🛡️ FOLIA SYNC: Entity Scheduler recurrente
                orbe.getScheduler().runAtFixedRate(plugin, task -> {
                    if (tiempo.get() <= 0 || orbe.isDead() || !orbe.isValid()) {
                        orbe.remove();
                        task.cancel();
                        return;
                    }
                    orbe.setRotation(orbe.getYaw() + 10, 0);
                    orbe.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, orbe.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.01);

                    orbe.getNearbyEntities(10, 10, 10).forEach(e -> {
                        if (e instanceof Player ally) {
                            ally.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 40, 0, false, false, false));
                        }
                    });
                    tiempo.decrementAndGet();
                }, null, 1L, 20L); // Retraso 1, Periodo 20 ticks
            });
            return true;
        });

        estrategias.put("capa_espectral", (p, dto) -> {
            invulnerables.add(p.getUniqueId());
            p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 80, 0, false, false, true));
            p.playSound(p.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1f, 1f);
            p.getWorld().spawnParticle(Particle.LARGE_SMOKE, p.getLocation(), 50, 0.5, 1, 0.5, 0.05);

            // 🛡️ FOLIA SYNC: Player Scheduler
            p.getScheduler().runDelayed(plugin, task -> {
                if (invulnerables.remove(p.getUniqueId())) {
                    crossplayUtils.sendMessage(p, "&#E6CCFFLa Capa Espectral se ha desvanecido. Tu firma vuelve a ser visible.");
                    p.playSound(p.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1f, 1f);
                }
            }, null, 80L);
            return true;
        });

        estrategias.put("reloj_astral", (p, dto) -> {
            limpiarCooldowns(p.getUniqueId());
            p.playSound(p.getLocation(), Sound.BLOCK_BELL_RESONATE, 1f, 1.5f);
            crossplayUtils.sendMessage(p, "&#00f5ff✨ <bold>PARADOJA TEMPORAL:</bold> &#E6CCFFTus sistemas de enfriamiento han sido reseteados.");
            return true;
        });

        estrategias.put("alas_nexo", (p, dto) -> {
            UUID uuid = p.getUniqueId();
            if (alasActivas.contains(uuid)) {
                alasActivas.remove(uuid);
                p.setAllowFlight(false);
                p.setFlying(false);
                crossplayUtils.sendMessage(p, "&#8b0000[!] Alas del Nexo desactivadas. Guardando rotores.");
                
                var playerCds = cooldowns.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
                playerCds.put(dto.id(), System.currentTimeMillis() + (dto.cooldown() * 1000L));
                return false;
            } else {
                alasActivas.add(uuid);
                p.setAllowFlight(true);
                crossplayUtils.sendMessage(p, "&#00f5ff[✓] Alas del Nexo en línea. Drenando " + dto.cost() + " ⚡/s.");

                // 🛡️ FOLIA SYNC: Player Scheduler recurrente
                p.getScheduler().runAtFixedRate(plugin, task -> {
                    if (!p.isOnline() || !alasActivas.contains(uuid)) {
                        task.cancel();
                        return;
                    }

                    NexoUser user = userManager.getUserLocal(uuid);
                    if (user == null) {
                        task.cancel();
                        return;
                    }

                    int energiaActual = user.getEnergiaMineria();

                    if (energiaActual < dto.cost()) {
                        alasActivas.remove(uuid);
                        p.setAllowFlight(false);
                        p.setFlying(false);
                        crossplayUtils.sendMessage(p, "&#8b0000[!] ⚡ Reservas de energía críticas. Alas desactivadas por seguridad.");
                        
                        var playerCds = cooldowns.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
                        playerCds.put(dto.id(), System.currentTimeMillis() + (dto.cooldown() * 1000L));
                        task.cancel();
                        return;
                    }

                    user.setEnergiaMineria(energiaActual - dto.cost());
                    p.getWorld().spawnParticle(Particle.WAX_ON, p.getLocation(), 2);
                }, null, 20L, 20L); // Retraso 20, Periodo 20 ticks
                
                return true;
            }
        });
    }
}