package me.aeroxis.pvp.pasivas;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.ability.CustomAbility;
import dev.aurelium.auraskills.api.registry.NamespacedId;
import dev.aurelium.auraskills.api.registry.NamespacedRegistry;
import dev.aurelium.auraskills.api.skill.Skills;
import dev.aurelium.auraskills.api.user.SkillsUser;
import me.aeroxis.core.crossplay.CrossplayUtils;
import me.aeroxis.core.user.AeroxisUser;
import me.aeroxis.core.user.UserManager;
import me.aeroxis.pvp.AeroxisPvP;
import me.aeroxis.pvp.config.ConfigManager;
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
 * 🏛️ AeroxisPvP - Gestor de Pasivas (Arquitectura Enterprise)
 * Sistema de Progresión Estricto: Tiers 15, 30, 50, 75.
 * Registra todas las 28 habilidades custom en AuraSkills.
 */
@Singleton
public class PasivasManager {

    private final AeroxisPvP plugin;
    private final UserManager userManager;
    private final ConfigManager configManager;
    private final CrossplayUtils crossplayUtils;

    // ⏳ Cooldowns de Supervivencia
    public final Map<UUID, Long> cdUltimaBatalla = new ConcurrentHashMap<>();
    public final Map<UUID, Long> ultimoTroncoRoto = new ConcurrentHashMap<>();
    public final Map<UUID, Long> invulnerablesUltimaBatalla = new ConcurrentHashMap<>();

    // ⏳ Cooldowns de Habilidades Activas (Tier 4 - Nivel 75)
    public final Map<UUID, Long> cdFiebreOro = new ConcurrentHashMap<>();
    public final Map<UUID, Long> cdFuriaLenador = new ConcurrentHashMap<>();
    public final Map<UUID, Long> cdCosechaDivina = new ConcurrentHashMap<>();
    public final Map<UUID, Long> cdPoseidon = new ConcurrentHashMap<>();
    public final Map<UUID, Long> cdGolpeSismico = new ConcurrentHashMap<>();
    public final Map<UUID, Long> cdTransmutacion = new ConcurrentHashMap<>();

    @Inject
    public PasivasManager(AeroxisPvP plugin, UserManager userManager, ConfigManager configManager, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.userManager = userManager;
        this.configManager = configManager;
        this.crossplayUtils = crossplayUtils;

        // 🌟 FIX: El registro y las tareas se movieron al método 'initialize'
        // para evitar que el hilo asíncrono o Bukkit rompan la construcción.
    }

    /**
     * 🌟 FIX THIS-ESCAPE: Inicia el motor de pasivas de forma segura.
     * DEBE llamarse desde la clase principal (AeroxisPvP) en el onEnable().
     */
    public void initialize() {
        registrarHabilidadesAuraSkills();
        iniciarTareasPeriodicas();
    }

    private void registrarHabilidadesAuraSkills() {
        try {
            NamespacedRegistry registry = AuraSkillsApi.get().useRegistry("aeroxis", plugin.getDataFolder());

            // ⛏️ Minería
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("aeroxis", "vision_nocturna")).build()); // Nvl 15
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("aeroxis", "resistencia_termica")).build()); // Nvl 30
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("aeroxis", "explosion_cadena")).build()); // Nvl 50
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("aeroxis", "fiebre_oro")).build()); // Nvl 75

            // 🪓 Forrajero
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("aeroxis", "cosecha_manzanas")).build()); // Nvl 15
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("aeroxis", "postura_inamovible")).build()); // Nvl 30
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("aeroxis", "furia_lenador")).build()); // Nvl 75

            // 🌾 Agricultura
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("aeroxis", "pies_ligeros")).build()); // Nvl 15
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("aeroxis", "zanahoria_dorada")).build()); // Nvl 30
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("aeroxis", "crecimiento_magico")).build()); // Nvl 50
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("aeroxis", "cosecha_divina")).build()); // Nvl 75

            // 🎣 Pescadería
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("aeroxis", "recuperacion_acuatica")).build()); // Nvl 15
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("aeroxis", "pesca_cuantica")).build()); // Nvl 30
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("aeroxis", "gracia_delfin")).build()); // Nvl 50
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("aeroxis", "llamada_poseidon")).build()); // Nvl 75

            // ⚔️ Lucha
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("aeroxis", "robo_vida")).build()); // Nvl 15
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("aeroxis", "ejecucion")).build()); // Nvl 30
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("aeroxis", "ultima_batalla")).build()); // Nvl 50
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("aeroxis", "golpe_sismico")).build()); // Nvl 75

            // 🔮 Encantamiento
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("aeroxis", "boost_xp")).build()); // Nvl 15
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("aeroxis", "retencion")).build()); // Nvl 30
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("aeroxis", "descuento_energia")).build()); // Nvl 50
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("aeroxis", "aura_sabiduria")).build()); // Nvl 75

            // 🧪 Alquimia
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("aeroxis", "breweo_extra")).build()); // Nvl 15
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("aeroxis", "potenciador_pociones")).build()); // Nvl 30
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("aeroxis", "metabolismo_magico")).build()); // Nvl 50
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("aeroxis", "transmutacion_vital")).build()); // Nvl 75

            plugin.getLogger().info("✅ 28 Habilidades custom integradas en el registro de AuraSkills.");
        } catch (Exception e) {
            plugin.getLogger().warning("⚠️ No se pudo registrar pasivas en AuraSkills: " + e.getMessage());
        }
    }

    public int getNivel(Player p, dev.aurelium.auraskills.api.skill.Skill skill) {
        AeroxisUser aeroxisUser = userManager.getUserOrNull(p.getUniqueId());
        if (aeroxisUser != null) {
            if (skill == Skills.FIGHTING) return aeroxisUser.getCombateNivel();
            if (skill == Skills.MINING) return aeroxisUser.getMineriaNivel();
            if (skill == Skills.FARMING) return aeroxisUser.getAgriculturaNivel();
        }
        try {
            SkillsUser user = AuraSkillsApi.get().getUser(p.getUniqueId());
            if (user != null) return user.getSkillLevel(skill);
        } catch (Exception ignored) {}
        return 0;
    }

    public int calcularCostoEnergia(Player p, int costoBase) {
        // Tier 3 Encantamiento (50): Descuento de energía
        if (getNivel(p, Skills.ENCHANTING) >= 50) return (int) (costoBase * 0.90);
        return costoBase;
    }

    private void iniciarTareasPeriodicas() {
        // Reloj Rápido (1 segundo)
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long currentTime = System.currentTimeMillis();

            for (Player p : Bukkit.getOnlinePlayers()) {
                UUID id = p.getUniqueId();

                // ⛏️ Tier 1 Minería (15) - Visión Nocturna
                if (p.getLocation().getY() < 0 && getNivel(p, Skills.MINING) >= 15) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 300, 0, false, false, false));
                }

                // 🧪 Tier 3 Alquimia (50) - Metabolismo Mágico
                if (getNivel(p, Skills.ALCHEMY) >= 50) {
                    p.removePotionEffect(PotionEffectType.POISON);
                    p.removePotionEffect(PotionEffectType.WITHER);
                }

                if (invulnerablesUltimaBatalla.containsKey(id)) {
                    if (currentTime > invulnerablesUltimaBatalla.get(id)) {
                        invulnerablesUltimaBatalla.remove(id);
                        crossplayUtils.sendMessage(p, configManager.getMessages().mensajes().pvp().escudoAgotado());
                    }
                }
            }
        }, 20L, 20L);

        // Reloj Lento (3 segundos) - Crecimiento de Cultivos Tier 3 (50)
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (getNivel(p, Skills.FARMING) >= 50) {
                    AeroxisUser user = userManager.getUserOrNull(p.getUniqueId());
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