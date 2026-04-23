package me.nexo.protections.managers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.clans.core.ClanManager;
import me.nexo.clans.core.NexoClan;
import me.nexo.core.user.NexoUser;
import me.nexo.core.user.UserManager;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 🛡️ NexoProtections - Gestor de Límites (Arquitectura Enterprise)
 * Rendimiento: Hilos Virtuales para Promesas, Cero Service Locators (Inyección Pura).
 */
@Singleton
public class LimitManager {

    private final ClaimManager claimManager;
    private final UserManager userManager;
    private final ClanManager clanManager; // 🌟 Sinergia Multi-Módulo Inyectada

    // 🌟 FIX: Gestor formal de Hilos Virtuales para reemplazar el ForkJoinPool de CompletableFuture
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // 💉 PILAR 1: Inyección Directa y Limpia
    @Inject
    public LimitManager(ClaimManager claimManager, UserManager userManager, ClanManager clanManager) {
        this.claimManager = claimManager;
        this.userManager = userManager;
        this.clanManager = clanManager;
    }

    /**
     * Calcula el límite máximo de bases que puede tener un jugador o su clan.
     */
    public int getMaxProtections(UUID playerId) {
        NexoUser user = userManager.getUserOrNull(playerId);
        if (user == null) return 2; // Límite base por si falla

        // 🌟 LÓGICA DE CLANES (Escala con el Monolito)
        if (user.hasClan()) {
            // 🌟 FIX: Búsqueda O(1) directa al Manager Inyectado
            NexoClan clan = clanManager.getClanFromCache(user.getClanId()).orElse(null);
            if (clan != null) {
                // Nivel 1 = 2 Bases | Nivel 5 = 10 Bases (O el escalado que decidas)
                return 2 + (clan.getMonolithLevel() * 2);
            }
        }

        // 🌟 LÓGICA SOLITARIO (Límite estricto de 2 piedras)
        // TODO: Comprobar si el jugador tiene rango VIP para darle +1
        return 2;
    }

    public boolean canClaimMore(UUID playerId) {
        // Contamos cuántas piedras le pertenecen a este jugador iterando la caché RAM
        long currentBases = claimManager.getAllStones().values().stream()
                .filter(stone -> stone.getOwnerId().equals(playerId))
                .count();

        return currentBases < getMaxProtections(playerId);
    }

    public CompletableFuture<Boolean> canPlaceNewStone(Player player) {
        // 🌟 FIX ZERO-LAG: Envolvemos la revisión en un Hilo Virtual en lugar del ForkJoinPool
        return CompletableFuture.supplyAsync(() -> canClaimMore(player.getUniqueId()), virtualExecutor);
    }

    public int getProtectionRadius(Player player) {
        NexoUser user = userManager.getUserOrNull(player.getUniqueId());

        // 🌟 Escala dinámica: Clanes más grandes = Bases más grandes
        if (user != null && user.hasClan()) {
            NexoClan clan = clanManager.getClanFromCache(user.getClanId()).orElse(null);
            if (clan != null) {
                // Radio base 15, expande +5 bloques por cada nivel del monolito del clan
                return 15 + (clan.getMonolithLevel() * 5);
            }
        }
        return 15; // Radio base para jugadores solitarios
    }
}