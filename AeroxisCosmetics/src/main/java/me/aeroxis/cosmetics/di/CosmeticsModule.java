package me.aeroxis.cosmetics.di;

import com.google.inject.AbstractModule;
import me.aeroxis.cosmetics.AeroxisCosmetics;
import me.aeroxis.cosmetics.engine.CosmeticEngine;
import me.aeroxis.cosmetics.listeners.CosmeticListener;
import me.aeroxis.cosmetics.listeners.SkyblockCosmeticListener;
import me.aeroxis.cosmetics.manager.BoomboxManager; // 🌟 Importamos el Manager
import me.aeroxis.cosmetics.manager.CosmeticManager;

/**
 * 💉 Módulo de Inyección para AeroxisCosmetics
 */
public class CosmeticsModule extends AbstractModule {

    private final AeroxisCosmetics plugin;

    public CosmeticsModule(AeroxisCosmetics plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        bind(AeroxisCosmetics.class).toInstance(plugin);

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