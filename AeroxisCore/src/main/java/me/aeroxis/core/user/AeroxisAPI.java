package me.aeroxis.core.user;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.core.api.ServiceManager;

import java.util.UUID;

/**
 * 🏛️ Nexo Network - API Global (Arquitectura Enterprise)
 * Transicionada a un servicio inyectable. Las referencias estáticas se mantienen
 * ÚNICAMENTE como puente seguro (Legacy) para módulos no migrados.
 */
@Singleton
public class AeroxisAPI {

    // 🛡️ Puente estático para retrocompatibilidad
    private static AeroxisAPI instance;

    private final UserManager userManager;
    private final ServiceManager serviceManager;

    // 💉 PILAR 1: Inyección de dependencias pura. Nada de "new ServiceManager()"
    @Inject
    public AeroxisAPI(UserManager userManager, ServiceManager serviceManager) {
        this.userManager = userManager;
        this.serviceManager = serviceManager;

        // 🌟 Guardamos la instancia inyectada para que el puente legacy funcione
        AeroxisAPI.instance = this;
    }

    /**
     * Obtiene los datos locales (RAM) de un jugador O(1).
     * @param uuid UUID del jugador
     * @return AeroxisUser o null si no está en la caché
     */
    public AeroxisUser getUserLocal(UUID uuid) {
        return userManager.getUserOrNull(uuid);
    }

    /**
     * Obtiene el ServiceManager inyectado.
     */
    public ServiceManager getServiceManager() {
        return this.serviceManager;
    }

    /**
     * 🌟 NUEVO: Obtiene el UserManager inyectado (Usado por NexoChat y otros módulos).
     */
    public UserManager getUserManager() {
        return this.userManager;
    }

    // ==========================================================
    // 🌉 PUENTE LEGACY (Para módulos viejos en proceso de migración)
    // * Evita usar estos métodos en código nuevo. ¡Inyecta AeroxisAPI en su lugar!
    // ==========================================================

    @Deprecated
    public static AeroxisAPI getInstance() {
        return instance;
    }

    @Deprecated
    public static ServiceManager getServices() {
        return instance != null ? instance.serviceManager : null;
    }
}