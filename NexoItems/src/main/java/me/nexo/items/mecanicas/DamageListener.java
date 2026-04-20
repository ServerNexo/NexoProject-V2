package me.nexo.items.mecanicas;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.NexoAPI;
import me.nexo.core.user.NexoUser;
import me.nexo.core.utils.NexoColor;
import me.nexo.items.NexoItems;
import me.nexo.items.dtos.ArmorDTO;
import me.nexo.items.dtos.EnchantDTO;
import me.nexo.items.dtos.WeaponDTO;
import me.nexo.items.managers.FileManager;
import me.nexo.items.managers.ItemManager;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * 🎒 NexoItems - Motor de Combate RPG (Arquitectura Enterprise O(1))
 * Cero creación de ItemMeta en memoria. Lectura nativa directa.
 */
@Singleton
public class DamageListener implements Listener {

    private final NexoItems plugin;
    private final FileManager fileManager;
    // 🌟 FIX: Declaramos el ItemManager inyectado
    private final ItemManager itemManager;

    private final NamespacedKey keyEvasion, keyEspinosa, keyEjecutor, keyCazador, keyVeneno, keyVampirismo;

    @Inject
    public DamageListener(NexoItems plugin, ItemManager itemManager) { // 🌟 FIX: Inyectamos ItemManager
        this.plugin = plugin;
        this.fileManager = plugin.getFileManager();
        this.itemManager = itemManager; // 🌟 FIX: Guardamos la instancia

        this.keyEvasion = new NamespacedKey(plugin, "nexo_enchant_evasion");
        this.keyEspinosa = new NamespacedKey(plugin, "nexo_enchant_coraza_espinosa");
        this.keyEjecutor = new NamespacedKey(plugin, "nexo_enchant_ejecutor");
        this.keyCazador = new NamespacedKey(plugin, "nexo_enchant_cazador");
        this.keyVeneno = new NamespacedKey(plugin, "nexo_enchant_veneno");
        this.keyVampirismo = new NamespacedKey(plugin, "nexo_enchant_vampirismo");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void alPegar(EntityDamageByEntityEvent event) {

        // ==========================================
        // 🛡️ LÓGICA DE DEFENSA
        // ==========================================
        if (event.getEntity() instanceof Player victima) {
            double probEvasion = 0.0;
            double reflejoEspinosa = 0.0;
            double defensaExtra = 0.0;

            for (ItemStack armor : victima.getInventory().getArmorContents()) {
                if (armor == null || armor.isEmpty()) continue; // 🌟 1.21.4 estándar

                // 🚀 LECTURA O(1): Leemos el Custom Data sin clonar el ItemMeta
                var pdc = armor.getPersistentDataContainer();
                if (pdc.isEmpty()) continue;

                // 🌟 FIX: Cambiamos ItemManager estático a inyectado
                if (pdc.has(itemManager.llaveArmaduraId, PersistentDataType.STRING)) {
                    ArmorDTO dto = fileManager.getArmorDTO(pdc.get(itemManager.llaveArmaduraId, PersistentDataType.STRING));
                    if (dto != null) defensaExtra += (dto.vidaExtra() / 10.0);
                }

                if (pdc.has(keyEvasion, PersistentDataType.INTEGER)) {
                    EnchantDTO ench = fileManager.getEnchantDTO("evasion");
                    if (ench != null) probEvasion += ench.getValorPorNivel(pdc.get(keyEvasion, PersistentDataType.INTEGER));
                }

                if (pdc.has(keyEspinosa, PersistentDataType.INTEGER)) {
                    EnchantDTO ench = fileManager.getEnchantDTO("coraza_espinosa");
                    if (ench != null) reflejoEspinosa += ench.getValorPorNivel(pdc.get(keyEspinosa, PersistentDataType.INTEGER));
                }
            }

            if (probEvasion > 0 && Math.random() * 100 <= probEvasion) {
                event.setCancelled(true);
                CrossplayUtils.sendMessage(victima, "&#00E5FF<bold>¡EVASIÓN PERFECTA!</bold>");
                victima.playSound(victima.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 2f);
                return;
            }

            double danioReducido = Math.max(1.0, event.getDamage() - defensaExtra);
            event.setDamage(danioReducido);

            if (reflejoEspinosa > 0 && event.getDamager() instanceof LivingEntity atacante) {
                double danioDevuelto = danioReducido * (reflejoEspinosa / 100.0);
                atacante.damage(danioDevuelto, victima);
            }
        }

        // ==========================================
        // ⚔️ LÓGICA DE ATAQUE
        // ==========================================
        if (event.getDamager() instanceof Player jugador && event.getEntity() instanceof LivingEntity target) {

            ItemStack arma = jugador.getInventory().getItemInMainHand();
            if (arma.isEmpty()) return;

            // 🚀 LECTURA O(1): Evitamos arma.getItemMeta()
            var pdc = arma.getPersistentDataContainer();

            // 🌟 FIX: Cambiamos ItemManager estático a inyectado
            if (pdc.has(itemManager.llaveWeaponId, PersistentDataType.STRING)) {
                String idArma = pdc.get(itemManager.llaveWeaponId, PersistentDataType.STRING);
                WeaponDTO dto = fileManager.getWeaponDTO(idArma);

                if (dto != null) {
                    NexoUser user = NexoAPI.getInstance().getUserLocal(jugador.getUniqueId());
                    String claseJugador = user != null ? user.getClaseJugador() : "Ninguna";
                    int nivelCombate = user != null ? user.getCombateNivel() : 1;

                    if (!dto.claseRequerida().equalsIgnoreCase("Cualquiera") && !dto.claseRequerida().equalsIgnoreCase(claseJugador)) {
                        CrossplayUtils.sendMessage(jugador, "&#FF5555[!] Incompatibilidad Neural: Tu clase (" + claseJugador + ") no puede empuñar este activo.");
                        jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                        event.setDamage(1.0);
                        return;
                    }

                    if (nivelCombate < dto.nivelRequerido()) {
                        event.setCancelled(true);
                        CrossplayUtils.sendMessage(jugador, "&#FF5555[!] Activo Bloqueado. Requiere Autorización de Combate Nivel " + dto.nivelRequerido());
                        jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                        return;
                    }

                    double dañoFinal = event.getDamage();

                    // 🌟 FIX: Cambiamos ItemManager estático a inyectado
                    int nivelEvolucion = pdc.getOrDefault(itemManager.llaveNivelEvolucion, PersistentDataType.INTEGER, 1);
                    dañoFinal *= (1.0 + (nivelEvolucion * 0.05));

                    // 🌟 FIX: Cambiamos ItemManager estático a inyectado
                    int prestigio = pdc.getOrDefault(itemManager.llaveWeaponPrestige, PersistentDataType.INTEGER, 0);
                    if (prestigio > 0 && dto.permitePrestigio()) {
                        dañoFinal += (dañoFinal * (prestigio * dto.multiPrestigio()));
                    }

                    // 🪄 ENCANTAMIENTOS OFENSIVOS
                    if (pdc.has(keyEjecutor, PersistentDataType.INTEGER)) {
                        // 🌟 FIX API 1.21.2+: Cambiado de GENERIC_MAX_HEALTH a MAX_HEALTH
                        AttributeInstance maxHealthAttr = target.getAttribute(Attribute.MAX_HEALTH);
                        if (maxHealthAttr != null && (target.getHealth() / maxHealthAttr.getValue()) <= 0.20) {
                            EnchantDTO ench = fileManager.getEnchantDTO("ejecutor");
                            if (ench != null) dañoFinal += (dañoFinal * (ench.getValorPorNivel(pdc.get(keyEjecutor, PersistentDataType.INTEGER)) / 100.0));
                        }
                    }

                    if (pdc.has(keyCazador, PersistentDataType.INTEGER) && target instanceof Monster) {
                        EnchantDTO ench = fileManager.getEnchantDTO("cazador");
                        if (ench != null) dañoFinal += (dañoFinal * (ench.getValorPorNivel(pdc.get(keyCazador, PersistentDataType.INTEGER)) / 100.0));
                    }

                    if (pdc.has(keyVeneno, PersistentDataType.INTEGER)) {
                        EnchantDTO ench = fileManager.getEnchantDTO("veneno");
                        if (ench != null) {
                            int duracion = (int) (ench.getValorPorNivel(pdc.get(keyVeneno, PersistentDataType.INTEGER)) * 20);
                            target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, duracion, 0, false, false, false));
                        }
                    }

                    // 🌟 Kyori Adventure (Adiós ChatColor de Bukkit obsoleto)
                    String elementoLimpio = PlainTextComponentSerializer.plainText().serialize(NexoColor.parse(dto.elemento())).toUpperCase();
                    String nombreMob = target.customName() != null ? PlainTextComponentSerializer.plainText().serialize(target.customName()).toUpperCase() : "";
                    double multElemental = 1.0;

                    if (elementoLimpio.contains("FUEGO") || elementoLimpio.contains("MAGMA") || elementoLimpio.contains("SOLAR")) {
                        target.setFireTicks(60);
                        if (nombreMob.contains("[HIELO]")) multElemental = 2.0;
                    } else if (elementoLimpio.contains("HIELO") || elementoLimpio.contains("AGUA")) {
                        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, false, false, false));
                        if (nombreMob.contains("[FUEGO]")) multElemental = 2.0;
                    } else if (elementoLimpio.contains("RAYO") || elementoLimpio.contains("TORMENTA")) {
                        if (Math.random() <= 0.15) target.getWorld().strikeLightningEffect(target.getLocation());
                        if (nombreMob.contains("[AGUA]")) multElemental = 2.0;
                    }

                    dañoFinal *= multElemental;

                    if (multElemental > 1.0) {
                        CrossplayUtils.sendMessage(jugador, "&#55FF55<bold>¡GOLPE CRÍTICO ELEMENTAL!</bold>");
                    }

                    event.setDamage(dañoFinal);

                    // 6. VAMPIRISMO
                    if (pdc.has(keyVampirismo, PersistentDataType.INTEGER)) {
                        EnchantDTO ench = fileManager.getEnchantDTO("vampirismo");
                        AttributeInstance playerMaxHealth = jugador.getAttribute(Attribute.MAX_HEALTH);

                        if (ench != null && playerMaxHealth != null) {
                            double cura = dañoFinal * (ench.getValorPorNivel(pdc.get(keyVampirismo, PersistentDataType.INTEGER)) / 100.0);
                            jugador.setHealth(Math.min(playerMaxHealth.getValue(), jugador.getHealth() + cura));
                            jugador.getWorld().spawnParticle(Particle.HEART, jugador.getLocation().add(0, 1, 0), 1);
                        }
                    }
                }
            }
        }
    }
}