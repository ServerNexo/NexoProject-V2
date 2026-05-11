package me.nexo.cosmetics.di;

import com.google.inject.AbstractModule;
import me.nexo.cosmetics.NexoCosmetics;
import me.nexo.cosmetics.engine.CosmeticEngine;
import me.nexo.cosmetics.listeners.CosmeticListener;
import me.nexo.cosmetics.listeners.SkyblockCosmeticListener;
import me.nexo.cosmetics.manager.BoomboxManager; // 🌟 Importamos el Manager
import me.nexo.cosmetics.manager.CosmeticManager;

/**
 * 💉 Módulo de Inyección para NexoCosmetics
 */
public class CosmeticsModule extends AbstractModule {

    private final NexoCosmetics plugin;

    public CosmeticsModule(NexoCosmetics plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        bind(NexoCosmetics.class).toInstance(plugin);

        // 🧠 Gestores de Memoria y Lógica
        bind(CosmeticManager.class).asEagerSingleton();
        bind(BoomboxManager.class).asEagerSingleton(); // 🌟 Inyectamos el Manager en lugar del Menú

        // ⚙️ Motor Asíncrono de Folia
        bind(CosmeticEngine.class).asEagerSingleton();

        // 🎧 Eventos
        bind(CosmeticListener.class).asEagerSingleton();
        bind(SkyblockCosmeticListener.class).asEagerSingleton();
    }
}