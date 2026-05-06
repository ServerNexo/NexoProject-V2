package me.nexo.colecciones.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.colecciones.NexoColecciones;
import me.nexo.colecciones.menu.SlayerMenu;
import me.nexo.colecciones.slayers.SlayerManager;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.economy.core.EconomyManager; // 🌟 NUEVO IMPORT INYECTADO
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * 📚 NexoColecciones - Comando de Cacería Slayer (Arquitectura Enterprise)
 * Rendimiento: Registro Nativo (PaperMC), Cero Service Locator y Dependencias Inyectadas.
 */
@Singleton
public class ComandoSlayer extends Command {

    private final NexoColecciones plugin;
    private final SlayerManager slayerManager;
    private final CrossplayUtils crossplayUtils;
    private final EconomyManager economyManager; // 🌟 AÑADIDO

    // 💉 PILAR 1: Inyección de Dependencias Directa (Agregamos la Economía)
    @Inject
    public ComandoSlayer(NexoColecciones plugin, SlayerManager slayerManager, CrossplayUtils crossplayUtils, EconomyManager economyManager) {
        super("slayer"); // Nombre del comando base
        this.setDescription("Abre el menú de contratos de cacería Slayer.");
        this.setUsage("/slayer [cancel]");

        this.plugin = plugin;
        this.slayerManager = slayerManager;
        this.crossplayUtils = crossplayUtils;
        this.economyManager = economyManager; // 🌟 ASIGNADO
    }

    // 🌟 FIX: Sobreescribimos execute() en lugar de onCommand para inyección nativa en CommandMap
    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {

        // 💻 PROTECCIÓN DE CONSOLA (Sin excepciones nulas)
        if (!(sender instanceof Player player)) {
            sender.sendMessage("[!] El terminal no puede invocar Slayers ni abrir el menú de cacería.");
            return true;
        }

        // ❌ SUBCOMANDO: CANCELAR CONTRATO
        if (args.length == 1 && args[0].equalsIgnoreCase("cancel")) {
            var activo = slayerManager.getActiveSlayer(player.getUniqueId());

            if (activo != null) {
                if (activo.getBossBar() != null) activo.getBossBar().removeAll();
                slayerManager.removeActiveSlayer(player.getUniqueId());

                crossplayUtils.sendMessage(player, "&#FF5555[!] <bold>CONTRATO CANCELADO:</bold> &#E6CCFFHas abandonado la cacería de esta bestia.");
            } else {
                crossplayUtils.sendMessage(player, "&#FF5555[!] No tienes ningún contrato Slayer activo en este momento.");
            }
            return true;
        }

        // 🌟 APERTURA DEL MENÚ PRINCIPAL SLAYER (Arquitectura Omega)
        // Propagamos TODAS las dependencias inyectadas hacia la interfaz gráfica (Incluyendo la economía)
        new SlayerMenu(player, plugin, slayerManager, crossplayUtils, economyManager).open();
        return true;
    }
}