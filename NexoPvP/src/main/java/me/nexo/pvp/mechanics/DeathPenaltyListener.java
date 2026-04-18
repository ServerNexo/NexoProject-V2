package me.nexo.pvp.mechanics;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.api.user.SkillsUser;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.NexoUser;
import me.nexo.core.user.UserManager;
import me.nexo.core.user.UserRepository;
import me.nexo.economy.NexoEconomy;
import me.nexo.economy.core.NexoAccount;
import me.nexo.pvp.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.CompletableFuture;

/**
 * 🏛️ NexoPvP - Penalización de Muerte (Arquitectura Enterprise)
 * Rendimiento: Promesas Aplanadas (Evita Callback Hell), Constantes O(1) y Prevención de Dupe de XP.
 */
@Singleton
public class DeathPenaltyListener implements Listener {

    private final UserManager userManager;
    private final UserRepository userRepository;
    private final ConfigManager configManager;

    // 🌟 OPTIMIZACIÓN ZERO-GARBAGE: Constante estática para no crear un nuevo objeto matemático por cada muerte
    private static final BigDecimal PENALTY_RATE = new BigDecimal("0.05");

    // Caché de integraciones para no leer el PluginManager en cada muerte
    private final boolean hasAuraSkills;
    private final boolean hasNexoEconomy;

    @Inject
    public DeathPenaltyListener(UserManager userManager, UserRepository userRepository, ConfigManager configManager) {
        this.userManager = userManager;
        this.userRepository = userRepository;
        this.configManager = configManager;

        this.hasAuraSkills = Bukkit.getPluginManager().isPluginEnabled("AuraSkills");
        this.hasNexoEconomy = Bukkit.getPluginManager().isPluginEnabled("NexoEconomy");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        NexoUser user = userManager.getUserOrNull(player.getUniqueId());

        if (user == null) return;

        // 🌟 LECTURA DE PROTECCIÓN DIVINA
        boolean hasProtection = user.hasActiveBlessing("VOID_BLESSING") || user.isVoidBlessingActive();

        if (hasProtection) {
            // 🛡️ PROTECCIÓN DIVINA: Mantiene todo intacto
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.getDrops().clear();
            event.setDroppedExp(0);

            CrossplayUtils.sendMessage(player, configManager.getMessages().mensajes().penalizaciones().muerteProtegida());

            // Consumir la bendición de 1 uso
            if (user.hasActiveBlessing("VOID_BLESSING")) {
                user.removeBlessing("VOID_BLESSING");
                userRepository.saveBlessings(user); // SQL Asíncrono de NexoCore
            }

        } else {
            // 🩸 HARDCORE PENALTY (Filosofía RPG Moderada)

            // 1. Pérdida de Niveles Vanilla (10%)
            int currentLevel = player.getLevel();
            event.setNewLevel(Math.max(0, currentLevel - (int)(currentLevel * 0.10)));

            // 🛑 FIX DUPE: Evitamos que la experiencia perdida caiga al suelo para que no la vuelvan a recoger
            event.setDroppedExp(0);

            // 2. Pérdida de 8% de XP de Profesiones (AuraSkills)
            if (hasAuraSkills) {
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
            }

            // 3. 💸 PENALIZACIÓN ECONÓMICA ASÍNCRONA (5% del Balance Total)
            if (hasNexoEconomy) {
                NexoEconomy ecoPlugin = (NexoEconomy) Bukkit.getPluginManager().getPlugin("NexoEconomy");

                if (ecoPlugin != null) {
                    // 🌟 FIX ARQUITECTURA: Aplanamos las Promesas (CompletableFuture Chaining)
                    // para evitar el "Callback Hell" y asegurar la liberación de memoria.
                    ecoPlugin.getEconomyManager().getAccountAsync(player.getUniqueId(), NexoAccount.AccountType.PLAYER)
                            .thenCompose(account -> {
                                if (account != null && account.getCoins() != null && account.getCoins().compareTo(BigDecimal.ZERO) > 0) {
                                    BigDecimal loss = account.getCoins().multiply(PENALTY_RATE).setScale(2, RoundingMode.HALF_UP);

                                    // Ejecutamos el débito y pasamos la pérdida al siguiente eslabón
                                    return ecoPlugin.getEconomyManager()
                                            .updateBalanceAsync(player.getUniqueId(), NexoAccount.AccountType.PLAYER, NexoAccount.Currency.COINS, loss, false)
                                            .thenApply(success -> success ? loss : null);
                                }
                                return CompletableFuture.completedFuture(null);
                            })
                            .thenAccept(loss -> {
                                // Este bloque solo se ejecuta si todo lo anterior fue exitoso
                                if (loss != null) {
                                    String msgCobro = configManager.getMessages().mensajes().penalizaciones().cobroResurreccion().replace("%amount%", loss.toPlainString());
                                    CrossplayUtils.sendMessage(player, msgCobro);
                                }
                            });
                }
            }

            CrossplayUtils.sendMessage(player, configManager.getMessages().mensajes().penalizaciones().perdidaProgreso());
            CrossplayUtils.sendMessage(player, configManager.getMessages().mensajes().penalizaciones().consejoBendicion());
        }

        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 1.0f, 0.5f);
    }
}