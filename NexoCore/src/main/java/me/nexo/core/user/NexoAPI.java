package me.nexo.core.user;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.api.ServiceManager;

import java.util.UUID;

/**
 * 🏛️ Nexo Network - API Global (Arquitectura Enterprise)
 * Transicionada a un servicio inyectable. Las referencias estáticas se mantienen
 * ÚNICAMENTE como puente seguro (Legacy) para módulos no migrados.
 */
@Singleton
public class NexoAPI {

    // 🛡️ Puente estático para retrocompatibilidad
    private static NexoAPI instance;
    
    private final UserManager userManager;
    private final ServiceManager serviceManager;

    // 💉 PILAR 1: Inyección de dependencias pura. Nada de "new ServiceManager()"
    @Inject
    public NexoAPI(UserManager userManager, ServiceManager serviceManager) {
        this.userManager = userManager;
        this.serviceManager = serviceManager;
        
        // 🌟 Guardamos la instancia inyectada para que el puente legacy funcione
        NexoAPI.instance = this;
    }

    /**
     * Obtiene los datos locales (RAM) de un jugador O(1).
     * @param uuid UUID del jugador
     * @return NexoUser o null si no está en la caché
     */
    public NexoUser getUserLocal(UUID uuid) {
        return userManager.getUserOrNull(uuid);
    }

    /**
     * Obtiene el ServiceManager inyectado.
     */
    public ServiceManager getServiceManager() {
        return this.serviceManager;
    }

    // ==========================================================
    // 🌉 PUENTE LEGACY (Para módulos viejos en proceso de migración)
    // * Evita usar estos métodos en código nuevo. ¡Inyecta NexoAPI en su lugar!
    // ==========================================================

    @Deprecated
    public static NexoAPI getInstance() {
        return instance;
    }

    @Deprecated
    public static ServiceManager getServices() {
        return instance != null ? instance.serviceManager : null;
    }
}