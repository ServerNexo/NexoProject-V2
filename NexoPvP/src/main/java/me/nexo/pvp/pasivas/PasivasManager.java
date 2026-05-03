package me.nexo.pvp.pasivas;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.ability.CustomAbility;
import dev.aurelium.auraskills.api.registry.NamespacedId;
import dev.aurelium.auraskills.api.registry.NamespacedRegistry;
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
 * Sistema de Progresión Estricto: Tiers 15, 30, 50, 75.
 * Registra todas las 28 habilidades custom en AuraSkills.
 */
@Singleton
public class PasivasManager {

    private final NexoPvP plugin;
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
    public PasivasManager(NexoPvP plugin, UserManager userManager, ConfigManager configManager, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.userManager = userManager;
        this.configManager = configManager;
        this.crossplayUtils = crossplayUtils;

        // 🌟 FIX: El registro y las tareas se movieron al método 'initialize'
        // para evitar que el hilo asíncrono o Bukkit rompan la construcción.
    }

    /**
     * 🌟 FIX THIS-ESCAPE: Inicia el motor de pasivas de forma segura.
     * DEBE llamarse desde la clase principal (NexoPvP) en el onEnable().
     */
    public void initialize() {
        registrarHabilidadesAuraSkills();
        iniciarTareasPeriodicas();
    }

    private void registrarHabilidadesAuraSkills() {
        try {
            NamespacedRegistry registry = AuraSkillsApi.get().useRegistry("nexo", plugin.getDataFolder());

            // ⛏️ Minería
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("nexo", "vision_nocturna")).build()); // Nvl 15
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("nexo", "resistencia_termica")).build()); // Nvl 30
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("nexo", "explosion_cadena")).build()); // Nvl 50
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("nexo", "fiebre_oro")).build()); // Nvl 75

            // 🪓 Forrajero
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("nexo", "cosecha_manzanas")).build()); // Nvl 15
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("nexo", "postura_inamovible")).build()); // Nvl 30
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("nexo", "furia_lenador")).build()); // Nvl 75

            // 🌾 Agricultura
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("nexo", "pies_ligeros")).build()); // Nvl 15
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("nexo", "zanahoria_dorada")).build()); // Nvl 30
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("nexo", "crecimiento_magico")).build()); // Nvl 50
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("nexo", "cosecha_divina")).build()); // Nvl 75

            // 🎣 Pescadería
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("nexo", "recuperacion_acuatica")).build()); // Nvl 15
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("nexo", "pesca_cuantica")).build()); // Nvl 30
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("nexo", "gracia_delfin")).build()); // Nvl 50
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("nexo", "llamada_poseidon")).build()); // Nvl 75

            // ⚔️ Lucha
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("nexo", "robo_vida")).build()); // Nvl 15
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("nexo", "ejecucion")).build()); // Nvl 30
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("nexo", "ultima_batalla")).build()); // Nvl 50
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("nexo", "golpe_sismico")).build()); // Nvl 75

            // 🔮 Encantamiento
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("nexo", "boost_xp")).build()); // Nvl 15
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("nexo", "retencion")).build()); // Nvl 30
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("nexo", "descuento_energia")).build()); // Nvl 50
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("nexo", "aura_sabiduria")).build()); // Nvl 75

            // 🧪 Alquimia
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("nexo", "breweo_extra")).build()); // Nvl 15
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("nexo", "potenciador_pociones")).build()); // Nvl 30
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("nexo", "metabolismo_magico")).build()); // Nvl 50
            registry.registerAbility(CustomAbility.builder(NamespacedId.of("nexo", "transmutacion_vital")).build()); // Nvl 75

            plugin.getLogger().info("✅ 28 Habilidades custom integradas en el registro de AuraSkills.");
        } catch (Exception e) {
            plugin.getLogger().warning("⚠️ No se pudo registrar pasivas en AuraSkills: " + e.getMessage());
        }
    }

    public int getNivel(Player p, dev.aurelium.auraskills.api.skill.Skill skill) {
        NexoUser nexoUser = userManager.getUserOrNull(p.getUniqueId());
        if (nexoUser != null) {
            if (skill == Skills.FIGHTING) return nexoUser.getCombateNivel();
            if (skill == Skills.MINING) return nexoUser.getMineriaNivel();
            if (skill == Skills.FARMING) return nexoUser.getAgriculturaNivel();
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