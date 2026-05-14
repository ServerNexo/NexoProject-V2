package me.aeroxis.crates;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.crates.commands.ComandoCrates;
import me.aeroxis.crates.config.ConfigManager;
import revxrsal.commands.bukkit.BukkitCommandHandler;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class CratesBootstrap {

    private final AeroxisCrates plugin;
    private final ConfigManager configManager;

    @Inject
    public CratesBootstrap(AeroxisCrates plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void startServices() {
        plugin.getLogger().info("⚡ Arrancando AeroxisCrates (AAA Edition)...");

        // 🌟 FIX ENTERPRISE: Encendemos el motor de configuración aquí de forma segura
        // Esto evita el [this-escape] y asegura que el YAML esté listo antes de registrar comandos
        configManager.loadConfigs();

        // Registrar Comandos usando Lamp v3
        BukkitCommandHandler handler = BukkitCommandHandler.create(plugin);

        // Autocompletado DINÁMICO leyendo del crates.yml
        handler.getAutoCompleter().registerSuggestion("crates", (args, sender, command) -> {
            List<String> crateIds = new ArrayList<>();
            if (configManager.getCratesNode() != null && !configManager.getCratesNode().node("crates").virtual()) {
                for (Object key : configManager.getCratesNode().node("crates").childrenMap().keySet()) {
                    crateIds.add(key.toString());
                }
            }
            return crateIds;
        });

        // Inyectamos y registramos el comando maestro
        handler.register(plugin.getInjector().getInstance(ComandoCrates.class));

        plugin.getLogger().info("✅ AeroxisCrates activado y autocompletado dinámico enlazado.");
    }

    public void stopServices() {
        plugin.getLogger().info("📦 AeroxisCrates apagado de forma segura.");
    }
}