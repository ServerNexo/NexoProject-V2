package me.nexo.dungeons.bosses;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.dungeons.NexoDungeons;
import me.nexo.dungeons.listeners.LootProtectionListener;
import me.nexo.economy.core.EconomyManager;
import me.nexo.economy.core.NexoAccount;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * 🏰 NexoDungeons - Distribuidor de Recompensas de Jefes (Arquitectura Enterprise)
 * Rendimiento: Folia RegionScheduler, Dependencias Inyectadas (Cero Service Locators) y Streams.
 */
@Singleton
public class LootDistributor {

    private final NexoDungeons plugin;
    private final LootProtectionListener lootProtectionListener;
    
    // 🌟 Sinergias inyectadas desde el Core/Economía (Adiós NexoAPI)
    private final EconomyManager economyManager;
    private final CrossplayUtils crossplayUtils;

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public LootDistributor(NexoDungeons plugin, LootProtectionListener lootProtectionListener,
                           EconomyManager economyManager, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.lootProtectionListener = lootProtectionListener;
        this.economyManager = economyManager;
        this.crossplayUtils = crossplayUtils;
    }

    public void distributeLoot(String bossInternalName, Map<UUID, Double> damageMap, Location deathLoc) {

        // 🌟 JAVA 21 NATIVO: .toList() inmutable
        var ranking = damageMap.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .toList();

        double totalDamage = ranking.stream().mapToDouble(Map.Entry::getValue).sum();

        int rank = 1;
        for (var entry : ranking) {
            var p = Bukkit.getPlayer(entry.getKey());
            if (p == null || !p.isOnline()) continue;

            double percentage = (entry.getValue() / totalDamage) * 100;
            final int finalRank = rank;

            // 🌟 FOLIA NATIVE: Las modificaciones físicas (dropear items) DEBEN ir al RegionScheduler
            Bukkit.getRegionScheduler().run(plugin, deathLoc, task -> {
                crossplayUtils.sendMessage(p, "&#555555--------------------------------");
                crossplayUtils.sendMessage(p, "&#ff00ff🐉 <bold>TITÁN DERROTADO:</bold> &#E6CCFF" + bossInternalName.toUpperCase());
                crossplayUtils.sendMessage(p, "&#E6CCFFDaño infligido: &#FF5555" + String.format("%.0f", entry.getValue()) + " &#555555(" + String.format("%.1f", percentage) + "%)");

                if (finalRank == 1) {
                    crossplayUtils.sendMessage(p, "&#FFAA00🏆 <bold>PRIMER LUGAR:</bold> &#E6CCFF¡Has infligido el mayor daño!");
                    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                    entregarItemSeguro(p, generarRecompensa("MITICO", bossInternalName), deathLoc);

                    // 🌟 Economía inyectada, segura y asíncrona
                    if (economyManager != null) {
                        economyManager.updateBalanceAsync(p.getUniqueId(), NexoAccount.AccountType.PLAYER, NexoAccount.Currency.COINS, BigDecimal.valueOf(1000), true);
                    }
                } else if (finalRank <= 3) {
                    crossplayUtils.sendMessage(p, "&#55FF55🥈 <bold>TOP 3 (Rango " + finalRank + "):</bold> &#E6CCFFGran desempeño en la batalla.");
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
                    entregarItemSeguro(p, generarRecompensa("EPICO", bossInternalName), deathLoc);

                    if (economyManager != null) {
                        economyManager.updateBalanceAsync(p.getUniqueId(), NexoAccount.AccountType.PLAYER, NexoAccount.Currency.COINS, BigDecimal.valueOf(500), true);
                    }
                } else {
                    crossplayUtils.sendMessage(p, "&#00f5ff🎖️ <bold>PARTICIPACIÓN:</bold> &#E6CCFFEl gremio agradece tu ayuda.");
                    entregarItemSeguro(p, generarRecompensa("COMUN", bossInternalName), deathLoc);

                    if (economyManager != null) {
                        economyManager.updateBalanceAsync(p.getUniqueId(), NexoAccount.AccountType.PLAYER, NexoAccount.Currency.COINS, BigDecimal.valueOf(100), true);
                    }
                }

                crossplayUtils.sendMessage(p, "&#E6CCFFTu recompensa protegida aguarda en el lugar de la caída.");
                crossplayUtils.sendMessage(p, "&#555555--------------------------------");
            });

            rank++;
        }
    }

    private void entregarItemSeguro(Player p, ItemStack item, Location deathLoc) {
        // 🌟 PAPER 1.21 FIX: Prevención de Ghost Items
        if (item == null || item.isEmpty()) return;
        
        var dropLoc = deathLoc.clone().add((Math.random() - 0.5) * 2, 0.5, (Math.random() - 0.5) * 2);
        lootProtectionListener.dropProtectedItem(dropLoc, item, p);
    }

    // Sistema de recompensas (listo para conectarse con NexoItems)
    private ItemStack generarRecompensa(String tier, String bossName) {
        if (tier.equals("MITICO")) return new ItemStack(Material.NETHER_STAR, 1);
        if (tier.equals("EPICO")) return new ItemStack(Material.DIAMOND, 3);
        return new ItemStack(Material.GOLD_INGOT, 5);
    }
}