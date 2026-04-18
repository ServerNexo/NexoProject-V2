package me.nexo.protections.managers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.clans.core.ClanManager;
import me.nexo.clans.core.NexoClan;
import me.nexo.core.user.NexoAPI;
import me.nexo.core.user.NexoUser;
import me.nexo.core.user.UserManager;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 🛡️ NexoProtections - Gestor de Límites (Arquitectura Enterprise)
 * Cero llamadas estáticas a NexoCore o NexoClans.
 */
@Singleton
public class LimitManager {

    private final ClaimManager claimManager;
    private final UserManager userManager;

    // 💉 PILAR 3: Inyección
    @Inject
    public LimitManager(ClaimManager claimManager, UserManager userManager) {
        this.claimManager = claimManager;
        this.userManager = userManager;
    }

    /**
     * Calcula el límite máximo de bases que puede tener un jugador o su clan.
     */
    public int getMaxProtections(UUID playerId) {
        NexoUser user = userManager.getUserOrNull(playerId);
        if (user == null) return 2; // Límite base por si falla

        // 🌟 LÓGICA DE CLANES (Escala con el Monolito)
        if (user.hasClan()) {
            // Buscamos el clan a través del ServiceManager (desacoplado y seguro)
            Optional<ClanManager> clanManagerOpt = NexoAPI.getServices().get(ClanManager.class);

            if (clanManagerOpt.isPresent()) {
                NexoClan clan = clanManagerOpt.get().getClanFromCache(user.getClanId()).orElse(null);
                if (clan != null) {
                    // Nivel 1 = 2 Bases | Nivel 5 = 10 Bases (O el escalado que decidas)
                    return 2 + (clan.getMonolithLevel() * 2);
                }
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
        // Envolvemos la revisión en una Promesa asíncrona para no congelar el servidor
        return CompletableFuture.supplyAsync(() -> canClaimMore(player.getUniqueId()));
    }

    public int getProtectionRadius(Player player) {
        NexoUser user = userManager.getUserOrNull(player.getUniqueId());

        // 🌟 Escala dinámica: Clanes más grandes = Bases más grandes
        if (user != null && user.hasClan()) {
            Optional<ClanManager> clanManagerOpt = NexoAPI.getServices().get(ClanManager.class);
            if (clanManagerOpt.isPresent()) {
                NexoClan clan = clanManagerOpt.get().getClanFromCache(user.getClanId()).orElse(null);
                if (clan != null) {
                    // Radio base 15, expande +5 bloques por cada nivel del monolito del clan
                    return 15 + (clan.getMonolithLevel() * 5);
                }
            }
        }
        return 15; // Radio base para jugadores solitarios
    }
}