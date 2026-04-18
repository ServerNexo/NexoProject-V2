package me.nexo.core.user;

import me.nexo.core.api.ServiceManager;

import java.util.UUID;

public class NexoAPI {

    private static NexoAPI instance;
    private final UserManager userManager;
    private static final ServiceManager serviceManager = new ServiceManager();

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