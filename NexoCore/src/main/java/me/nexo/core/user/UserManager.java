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
 * Arquitectura Enterprise: Gestor en Memoria (RAM) ultra rápido con Caffeine y Guice puro.
 * Completamente Thread-Safe y libre de estados estáticos.
 */
@Singleton
public class UserManager {

    // 🟢 CLEAN CODE: Caché concurrente de alto rendimiento.
    private final Cache<UUID, NexoUser> usersCache;

    // 💉 PILAR 1: Inyección de dependencias requerida por el ServiceBootstrap/Guice
    @Inject
    public UserManager() {
        this.usersCache = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofHours(2)) // Auto-limpieza tras inactividad
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
        if (uuid != null) {
            usersCache.invalidate(uuid);
        }
    }

    /**
     * Obtiene a un jugador de la caché.
     * @return Optional para obligar al programador a manejar casos nulos de forma segura.
     */
    public Optional<NexoUser> getUser(UUID uuid) {
        if (uuid == null) return Optional.empty();
        return Optional.ofNullable(usersCache.getIfPresent(uuid));
    }

    /**
     * Obtiene a un jugador directamente de la caché.
     * @return El NexoUser o null si no se encuentra en memoria.
     */
    public NexoUser getUserOrNull(UUID uuid) {
        if (uuid == null) return null;
        return usersCache.getIfPresent(uuid);
    }
}