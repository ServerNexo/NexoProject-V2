package me.nexo.dungeons.bosses;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.NexoAPI;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 🏰 NexoDungeons - Distribuidor de Recompensas de Jefes (Arquitectura Enterprise)
 */
@Singleton
public class LootDistributor {

    private final NexoDungeons plugin;
    private final LootProtectionListener lootProtectionListener;

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public LootDistributor(NexoDungeons plugin, LootProtectionListener lootProtectionListener) {
        this.plugin = plugin;
        this.lootProtectionListener = lootProtectionListener; // 🌟 Inyectamos la protección de botín
    }

    // 🌟 FIX: Firma del método actualizada (Sin 'static', y recibiendo 'deathLoc')
    public void distributeLoot(String bossInternalName, Map<UUID, Double> damageMap, Location deathLoc) {

        List<Map.Entry<UUID, Double>> ranking = damageMap.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .toList();

        double totalDamage = ranking.stream().mapToDouble(Map.Entry::getValue).sum();

        int rank = 1;
        for (Map.Entry<UUID, Double> entry : ranking) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null || !p.isOnline()) continue;

            double percentage = (entry.getValue() / totalDamage) * 100;
            final int finalRank = rank;

            // Tarea síncrona para enviar mensajes, sonidos e interactuar con la API
            Bukkit.getScheduler().runTask(plugin, () -> {
                // 🌟 FIX: Mensajes directos Hexadecimales. Cero lag de lectura I/O.
                CrossplayUtils.sendMessage(p, "&#555555--------------------------------");
                CrossplayUtils.sendMessage(p, "&#ff00ff🐉 <bold>TITÁN DERROTADO:</bold> &#E6CCFF" + bossInternalName.toUpperCase());
                CrossplayUtils.sendMessage(p, "&#E6CCFFDaño infligido: &#FF5555" + String.format("%.0f", entry.getValue()) + " &#555555(" + String.format("%.1f", percentage) + "%)");

                if (finalRank == 1) {
                    CrossplayUtils.sendMessage(p, "&#FFAA00🏆 <bold>PRIMER LUGAR:</bold> &#E6CCFF¡Has infligido el mayor daño!");
                    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                    entregarItemSeguro(p, generarRecompensa("MITICO", bossInternalName), deathLoc);

                    // Economía asíncrona segura conectada mediante NexoAPI
                    NexoAPI.getServices().get(EconomyManager.class).ifPresent(eco ->
                            eco.updateBalanceAsync(p.getUniqueId(), NexoAccount.AccountType.PLAYER, NexoAccount.Currency.COINS, BigDecimal.valueOf(1000), true)
                    );
                } else if (finalRank <= 3) {
                    CrossplayUtils.sendMessage(p, "&#55FF55🥈 <bold>TOP 3 (Rango " + finalRank + "):</bold> &#E6CCFFGran desempeño en la batalla.");
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
                    entregarItemSeguro(p, generarRecompensa("EPICO", bossInternalName), deathLoc);

                    NexoAPI.getServices().get(EconomyManager.class).ifPresent(eco ->
                            eco.updateBalanceAsync(p.getUniqueId(), NexoAccount.AccountType.PLAYER, NexoAccount.Currency.COINS, BigDecimal.valueOf(500), true)
                    );
                } else {
                    CrossplayUtils.sendMessage(p, "&#00f5ff🎖️ <bold>PARTICIPACIÓN:</bold> &#E6CCFFEl gremio agradece tu ayuda.");
                    entregarItemSeguro(p, generarRecompensa("COMUN", bossInternalName), deathLoc);

                    NexoAPI.getServices().get(EconomyManager.class).ifPresent(eco ->
                            eco.updateBalanceAsync(p.getUniqueId(), NexoAccount.AccountType.PLAYER, NexoAccount.Currency.COINS, BigDecimal.valueOf(100), true)
                    );
                }

                CrossplayUtils.sendMessage(p, "&#E6CCFFTu recompensa protegida aguarda en el lugar de la caída.");
                CrossplayUtils.sendMessage(p, "&#555555--------------------------------");
            });

            rank++;
        }
    }

    // 🌟 FIX: Llama al listener inyectado y usa la location de la muerte
    private void entregarItemSeguro(Player p, ItemStack item, Location deathLoc) {
        if (item == null) return;
        // Dropeamos el botín en el cadáver del Boss con un pequeño offset aleatorio visual
        Location dropLoc = deathLoc.clone().add((Math.random() - 0.5) * 2, 0.5, (Math.random() - 0.5) * 2);
        lootProtectionListener.dropProtectedItem(dropLoc, item, p);
    }

    // Sistema de recompensas (listo para conectarse con NexoItems)
    private ItemStack generarRecompensa(String tier, String bossName) {
        if (tier.equals("MITICO")) return new ItemStack(Material.NETHER_STAR, 1);
        if (tier.equals("EPICO")) return new ItemStack(Material.DIAMOND, 3);
        return new ItemStack(Material.GOLD_INGOT, 5);
    }
}