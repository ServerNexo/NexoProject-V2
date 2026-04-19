package me.nexo.core.user;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.api.ServiceManager;

import java.util.UUID;

@Singleton // 🌟 FIX: Garantiza que solo exista una API en toda la red
public class NexoAPI {

    private static NexoAPI instance;
    private final UserManager userManager;
    private static final ServiceManager serviceManager = new ServiceManager();

    @Inject // 🌟 FIX: Le decimos a Guice que construya esta API automáticamente
    public NexoAPI(UserManager userManager) {
        instance = this;
        this.userManager = userManager;
    }

    public static NexoAPI getInstance() {
        return instance;
    }

    public static ServiceManager getServices() {
        return serviceManager;
    }

    public NexoUser getUserLocal(UUID uuid) {
        return userManager.getUserOrNull(uuid);
    }
}