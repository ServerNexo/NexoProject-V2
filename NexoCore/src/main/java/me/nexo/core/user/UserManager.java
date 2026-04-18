package me.nexo.core.user;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * 🏛️ Nexo Network - User Manager
 * Gestor en Memoria (RAM) ultra rápido con Caffeine y Guice.
 */
@Singleton // 🌟 FIX CRÍTICO: Garantiza que todo el servidor comparta la MISMA caché.
public class UserManager {

    // 🟢 CLEAN CODE: Caché ultra rápida concurrente.
    private final Cache<UUID, NexoUser> usersCache;

    @Inject // 🌟 FIX: Permite que el ServiceManager/Guice inicialice esta clase correctamente.
    public UserManager() {
        this.usersCache = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofHours(2))
                .build();
    }

    /**
     * Guarda a un jugador en la caché local (RAM).
     */
    public void addUserToCache(NexoUser user) {
        if (user != null) {
            usersCache.put(user.getUuid(), user);
        }
    }

    /**
     * Remueve a un jugador de la caché local (RAM).
     */
    public void removeUserFromCache(UUID uuid) {
        usersCache.invalidate(uuid);
    }

    /**
     * Obtiene a un jugador de la caché.
     * Devuelve Optional para obligar al programador a revisar si el jugador realmente existe.
     */
    public Optional<NexoUser> getUser(UUID uuid) {
        return Optional.ofNullable(usersCache.getIfPresent(uuid));
    }

    /**
     * Obtiene a un jugador directamente (puede ser nulo).
     */
    public NexoUser getUserOrNull(UUID uuid) {
        return usersCache.getIfPresent(uuid);
    }
}