package me.nexo.pvp.pasivas;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.skill.Skills;
import dev.aurelium.auraskills.api.user.SkillsUser;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.NexoUser;
import me.nexo.core.user.UserManager;
import me.nexo.pvp.NexoPvP;
import me.nexo.pvp.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🏛️ NexoPvP - Gestor de Pasivas (Arquitectura Enterprise)
 * Cero static API calls. Inyección de Dependencias directa.
 */
@Singleton
public class PasivasManager {

    // 💉 PILAR 1: Dependencias inyectadas puras
    private final NexoPvP plugin;
    private final UserManager userManager;
    private final ConfigManager configManager;
    private final CrossplayUtils crossplayUtils; // 🌟 FIX: Inyección del utilitario

    public final Map<UUID, Long> cdUltimaBatalla = new ConcurrentHashMap<>();
    public final Map<UUID, Long> ultimoTroncoRoto = new ConcurrentHashMap<>();
    public final Map<UUID, Long> invulnerablesUltimaBatalla = new ConcurrentHashMap<>();

    @Inject
    public PasivasManager(NexoPvP plugin, UserManager userManager, ConfigManager configManager, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.userManager = userManager;
        this.configManager = configManager;
        this.crossplayUtils = crossplayUtils;
        
        iniciarTareasPeriodicas();
    }

    public int getNivel(Player p, dev.aurelium.auraskills.api.skill.Skill skill) {
        // 🚀 Búsqueda directa en memoria inyectada (Mucho más rápido que NexoAPI.getInstance())
        NexoUser nexoUser = userManager.getUserOrNull(p.getUniqueId());
        if (nexoUser != null) {
            if (skill == Skills.FIGHTING) return nexoUser.getCombateNivel();
            if (skill == Skills.MINING) return nexoUser.getMineriaNivel();
            if (skill == Skills.FARMING) return nexoUser.getAgriculturaNivel();
        }

        try {
            // Llamada estática permitida por ser un plugin externo fuera del ecosistema Guice
            SkillsUser user = AuraSkillsApi.get().getUser(p.getUniqueId());
            if (user != null) return user.getSkillLevel(skill);
        } catch (Exception ignored) {}
        return 0;
    }

    public int calcularCostoEnergia(Player p, int costoBase) {
        if (getNivel(p, Skills.ENCHANTING) >= 50) {
            return (int) (costoBase * 0.90);
        }
        return costoBase;
    }

    private void iniciarTareasPeriodicas() {
        // Reloj Rápido (1 segundo) - Debe correr en el Main Thread porque modifica PotionEffects
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long currentTime = System.currentTimeMillis();
            
            for (Player p : Bukkit.getOnlinePlayers()) {
                UUID id = p.getUniqueId();

                if (p.getLocation().getY() < 0 && getNivel(p, Skills.MINING) >= 10) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 300, 0, false, false, false));
                }

                if (invulnerablesUltimaBatalla.containsKey(id)) {
                    if (currentTime > invulnerablesUltimaBatalla.get(id)) {
                        invulnerablesUltimaBatalla.remove(id);

                        // 🌟 FIX: Uso inyectado de CrossplayUtils
                        crossplayUtils.sendMessage(p, configManager.getMessages().mensajes().pvp().escudoAgotado());
                    }
                }
            }
        }, 20L, 20L);

        // Reloj Lento (3 segundos) - Crecimiento de Cultivos
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (getNivel(p, Skills.FARMING) >= 50) {
                    NexoUser user = userManager.getUserOrNull(p.getUniqueId());
                    if (user == null) continue;

                    int energia = user.getEnergiaMineria();
                    int costo = calcularCostoEnergia(p, 5);

                    if (energia >= costo) {
                        Block centro = p.getLocation().getBlock();
                        boolean aplico = false;

                        buscarCultivo:
                        for (int x = -5; x <= 5; x++) {
                            for (int z = -5; z <= 5; z++) {
                                Block b = centro.getRelative(x, 0, z);
                                if (b.getBlockData() instanceof Ageable cultivo && cultivo.getAge() < cultivo.getMaximumAge()) {
                                    b.applyBoneMeal(BlockFace.UP);
                                    p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, b.getLocation(), 5);
                                    aplico = true;
                                    break buscarCultivo;
                                }
                            }
                        }
                        if (aplico) {
                            user.setEnergiaMineria(energia - costo);
                            p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 2f);
                        }
                    }
                }
            }
        }, 60L, 60L);
    }
}