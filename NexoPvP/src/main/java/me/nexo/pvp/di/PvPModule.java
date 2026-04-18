package me.nexo.pvp.di;

import com.google.inject.AbstractModule;
import me.nexo.core.NexoCore;
import me.nexo.core.user.UserRepository;
import me.nexo.pvp.NexoPvP;
import me.nexo.pvp.api.PvPBootstrap;
import me.nexo.pvp.config.ConfigManager;
import me.nexo.pvp.pasivas.PasivasManager;
import me.nexo.pvp.pvp.PvPManager;

public class PvPModule extends AbstractModule {

    private final NexoPvP plugin;

    public PvPModule(NexoPvP plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        bind(NexoPvP.class).toInstance(plugin);

        // 🛡️ PILAR 4: Conectamos NexoPvP a la Base de Datos purificada del Core
        bind(UserRepository.class).toInstance(NexoCore.getPlugin(NexoCore.class).getUserRepository());

        // Singletons
        bind(ConfigManager.class).asEagerSingleton();
        bind(PvPManager.class).asEagerSingleton();
        bind(PasivasManager.class).asEagerSingleton();
        bind(PvPBootstrap.class).asEagerSingleton();
    }
}