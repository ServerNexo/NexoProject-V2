package me.aeroxis.pvp.mechanics;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.api.user.SkillsUser;
import me.aeroxis.core.crossplay.CrossplayUtils;
import me.aeroxis.core.user.AeroxisUser;
import me.aeroxis.core.user.UserManager;
import me.aeroxis.core.user.UserRepository;
import me.aeroxis.economy.core.EconomyManager; // 🌟 Sinergia inyectada
import me.aeroxis.economy.core.AeroxisAccount;
import me.aeroxis.pvp.AeroxisPvP;
import me.aeroxis.pvp.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🏛️ AeroxisPvP - Penalización de Muerte (Arquitectura Enterprise)
 * Rendimiento: Promesas Aplanadas, Constantes O(1), Prevención de Dupe de XP e Inyección Directa.
 */
@Singleton
public class DeathPenaltyListener implements Listener {

    private final AeroxisPvP plugin;
    private final UserManager userManager;
    private final UserRepository userRepository;
    private final ConfigManager configManager;

    // 🌟 Sinergia de Módulos Inyectada (Cero "Bukkit.getPluginManager().getPlugin(...)")
    private final CrossplayUtils crossplayUtils;
    private final EconomyManager economyManager;

    // 🌟 OPTIMIZACIÓN ZERO-GARBAGE
    private static final BigDecimal PENALTY_RATE = new BigDecimal("0.05");

    private final boolean hasAuraSkills;

    // Caché efímera ultrarrápida para efectos inmersivos de resurrección
    private final ConcurrentHashMap<UUID, Boolean> protectedRespawnCache = new ConcurrentHashMap<>();

    // 💉 PILAR 1: Inyección de Dependencias
    @Inject
    public DeathPenaltyListener(AeroxisPvP plugin, UserManager userManager, UserRepository userRepository,
                                ConfigManager configManager, CrossplayUtils crossplayUtils,
                                EconomyManager economyManager) {
        this.plugin = plugin;
        this.userManager = userManager;
        this.userRepository = userRepository;
        this.configManager = configManager;
        this.crossplayUtils = crossplayUtils;
        this.economyManager = economyManager;

        // AuraSkills es externo, se mantiene la comprobación estándar
        this.hasAuraSkills = Bukkit.getPluginManager().isPluginEnabled("AuraSkills");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        // 🌟 REGLA DE COMPATIBILIDAD INTER-MÓDULOS:
        // Si NexoDungeons u otro minijuego ya protegió el inventario, ignoramos el castigo.
        if (event.getKeepInventory()) return;

        Player player = event.getEntity();
        AeroxisUser user = userManager.getUserOrNull(player.getUniqueId());

        if (user == null) return;

        // 🌟 LECTURA DE PROTECCIÓN DIVINA
        boolean hasProtection = user.hasActiveBlessing("VOID_BLESSING") || user.isVoidBlessingActive();

        if (hasProtection) {
            // 🛡️ PROTECCIÓN DIVINA: Mantiene todo intacto
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.getDrops().clear();
            event.setDroppedExp(0);

            // Guardamos al jugador para el efecto visual al revivir
            protectedRespawnCache.put(player.getUniqueId(), true);

            crossplayUtils.sendMessage(player, configManager.getMessages().mensajes().penalizaciones().muerteProtegida());

            // Consumir la bendición de 1 uso
            if (user.hasActiveBlessing("VOID_BLESSING")) {
                user.removeBlessing("VOID_BLESSING");
                userRepository.saveBlessings(user); // SQL Asíncrono de AeroxisCore manejado por Virtual Threads
            }

        } else {
            // 🩸 HARDCORE PENALTY (Filosofía RPG Moderada)

            // 1. Pérdida de Niveles Vanilla (10%)
            int currentLevel = player.getLevel();
            event.setNewLevel(Math.max(0, currentLevel - (int)(currentLevel * 0.10)));

            // 🛑 FIX DUPE: Evitamos que la experiencia perdida caiga al suelo
            event.setDroppedExp(0);

            // 2. Pérdida de 8% de XP de Profesiones (AuraSkills) de forma Asíncrona
            if (hasAuraSkills) {
                CompletableFuture.runAsync(() -> {
                    try {
                        SkillsUser skillsUser = AuraSkillsApi.get().getUser(player.getUniqueId());
                        if (skillsUser != null) {
                            for (Skill skill : AuraSkillsApi.get().getGlobalRegistry().getSkills()) {
                                double currentXp = skillsUser.getSkillXp(skill);
                                if (currentXp > 0) {
                                    double xpLost = currentXp * 0.08;
                                    skillsUser.addSkillXp(skill, -xpLost);
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                });
            }

            // 3. 💸 PENALIZACIÓN ECONÓMICA ASÍNCRONA (5% del Balance Total)
            economyManager.getAccountAsync(player.getUniqueId(), AeroxisAccount.AccountType.PLAYER)
                    .thenCompose(account -> {
                        if (account != null && account.getCoins() != null && account.getCoins().compareTo(BigDecimal.ZERO) > 0) {
                            var loss = account.getCoins().multiply(PENALTY_RATE).setScale(2, RoundingMode.HALF_UP);

                            // Ejecutamos el débito y pasamos la pérdida al siguiente eslabón
                            return economyManager.updateBalanceAsync(player.getUniqueId(), AeroxisAccount.AccountType.PLAYER, AeroxisAccount.Currency.COINS, loss, false)
                                    .thenApply(success -> success ? loss : null);
                        }
                        return CompletableFuture.completedFuture(null);
                    })
                    .thenAccept(loss -> {
                        // Este bloque solo se ejecuta si todo lo anterior fue exitoso
                        if (loss != null) {
                            String msgCobro = configManager.getMessages().mensajes().penalizaciones().cobroResurreccion().replace("%amount%", loss.toPlainString());
                            crossplayUtils.sendMessage(player, msgCobro);
                        }
                    });

            crossplayUtils.sendMessage(player, configManager.getMessages().mensajes().penalizaciones().perdidaProgreso());
            crossplayUtils.sendMessage(player, configManager.getMessages().mensajes().penalizaciones().consejoBendicion());
        }

        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 1.0f, 0.5f);
    }

    // ==========================================
    // ✨ INMERSIÓN VISUAL (HILO DE REGIÓN)
    // ==========================================
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        if (protectedRespawnCache.remove(player.getUniqueId()) != null) {
            // Las partículas se lanzan síncronamente al mundo (Folia-Ready)
            Bukkit.getRegionScheduler().runDelayed(plugin, event.getRespawnLocation(), task -> {
                if (!player.isOnline()) return;

                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 0.5f);
                player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, player.getLocation().add(0, 1, 0), 40, 0.5, 1.0, 0.5, 0.05);
            }, 5L); // Pequeño retraso para que el cliente renderice
        }
    }
}