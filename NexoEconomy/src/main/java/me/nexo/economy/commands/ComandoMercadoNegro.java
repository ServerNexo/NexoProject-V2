package me.nexo.economy.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.UserManager;
import me.nexo.economy.blackmarket.BlackMarketManager;
import me.nexo.economy.blackmarket.BlackMarketMenu;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 💰 NexoEconomy - Comando del Mercado Negro (Arquitectura Enterprise)
 * Rendimiento: CommandMap Nativo, Sinergia con Inyector Principal y Cero Service Locators.
 */
@Singleton
public class ComandoMercadoNegro extends Command {

    // 🌟 DEPENDENCIAS PROPAGADAS (Desde NexoEconomy y NexoCore)
    private final BlackMarketManager blackMarketManager;
    private final CrossplayUtils crossplayUtils;
    private final UserManager userManager;

    // 💉 PILAR 1: Inyección de Dependencias Directa (Sinergia de Módulos Hijos)
    @Inject
    public ComandoMercadoNegro(BlackMarketManager blackMarketManager, CrossplayUtils crossplayUtils, UserManager userManager) {
        super("mercadonegro");
        this.description = "Abre la interfaz clandestina del Mercado Negro.";
        this.aliases = List.of("blackmarket", "bm");

        this.blackMarketManager = blackMarketManager;
        this.crossplayUtils = crossplayUtils;
        this.userManager = userManager;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {

        // 🌟 COMANDOS DE ADMINISTRADOR
        if (args.length > 0 && sender.hasPermission("nexoeconomy.admin")) {
            if (args[0].equalsIgnoreCase("open")) {
                blackMarketManager.openMarket();
                crossplayUtils.sendMessage(sender, "&#555555[&#AA00AA✓&#555555] &#FF55FFHas invocado al Mercader Oscuro.");
                return true;
            }
            if (args[0].equalsIgnoreCase("close")) {
                blackMarketManager.closeMarket();
                crossplayUtils.sendMessage(sender, "&#555555[&#FF5555✓&#555555] &#FF5555Has desterrado al Mercader Oscuro.");
                return true;
            }
        }

        // 🌟 FILTRO DE CONSOLA (Pattern Matching)
        if (!(sender instanceof Player player)) {
            sender.sendMessage("[!] El terminal requiere un operario humano para invocar el menú.");
            return true;
        }

        // 🌟 VALIDACIÓN VOID REACH (Uso puro de la dependencia inyectada en lugar del Core estático)
        var user = userManager.getUserOrNull(player.getUniqueId());
        if (user == null || !user.isVoidBlessingActive()) {
            crossplayUtils.sendMessage(player, "&#8b0000[!] <bold>ACCESO DENEGADO:</bold> &#E6CCFFEl Mercado Negro remoto requiere la Bendición del Vacío activa.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return true;
        }

        // Validar si el mercader está activo antes de abrir el menú
        if (!blackMarketManager.isMarketOpen()) {
            crossplayUtils.sendMessage(player, "&#FF5555[!] El mercader no se encuentra en este plano. Vuelve más tarde.");
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_AMBIENT, 1.0f, 0.5f);
            return true;
        }

        // 🌟 ABRE EL MENÚ SEGURO (Con dependencias transitivas)
        new BlackMarketMenu(player, blackMarketManager, crossplayUtils).open();

        return true;
    }
}