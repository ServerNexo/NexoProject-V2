package me.nexo.economy.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.UserManager;
import me.nexo.economy.NexoEconomy;
import me.nexo.economy.blackmarket.BlackMarketManager;
import me.nexo.economy.blackmarket.BlackMarketMenu;
import me.nexo.economy.core.EconomyManager;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 💰 NexoEconomy - Comando del Mercado Negro (Arquitectura Enterprise Java 21)
 * Rendimiento: CommandMap Nativo, Console Safety y Sinergia Inyectada.
 */
@Singleton
public class ComandoMercadoNegro extends Command {

    // 🌟 DEPENDENCIAS PROPAGADAS (Desde NexoEconomy y NexoCore)
    private final NexoEconomy plugin;
    private final BlackMarketManager blackMarketManager;
    private final EconomyManager ecoManager;
    private final CrossplayUtils crossplayUtils;
    private final UserManager userManager;

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public ComandoMercadoNegro(NexoEconomy plugin, BlackMarketManager blackMarketManager, EconomyManager ecoManager, CrossplayUtils crossplayUtils, UserManager userManager) {
        super("mercadonegro");

        // 🌟 FIX: Uso de setters para encapsulamiento
        this.setDescription("Abre la interfaz clandestina del Mercado Negro.");
        this.setAliases(List.of("blackmarket", "bm"));

        this.plugin = plugin;
        this.blackMarketManager = blackMarketManager;
        this.ecoManager = ecoManager;
        this.crossplayUtils = crossplayUtils;
        this.userManager = userManager;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {

        // 🌟 COMANDOS DE ADMINISTRADOR (Soporte Consola y Jugadores)
        if (args.length > 0 && sender.hasPermission("nexoeconomy.admin")) {
            // Evaluamos si el sender es un jugador (para el parseo), si no, pasamos null (Consola)
            Player p = sender instanceof Player ? (Player) sender : null;

            if (args[0].equalsIgnoreCase("open")) {
                blackMarketManager.openMarket();
                // 🌟 FIX: Usamos sender.sendMessage() nativo pasando el componente parseado
                sender.sendMessage(crossplayUtils.parseCrossplay(p, "&#555555[&#AA00AA✓&#555555] &#FF55FFHas invocado al Mercader Oscuro."));
                return true;
            }
            if (args[0].equalsIgnoreCase("close")) {
                blackMarketManager.closeMarket();
                sender.sendMessage(crossplayUtils.parseCrossplay(p, "&#555555[&#FF5555✓&#555555] &#FF5555Has desterrado al Mercader Oscuro."));
                return true;
            }
        }

        // 🌟 FILTRO DE CONSOLA (Pattern Matching)
        // A partir de aquí, el resto del comando obliga a ser un Jugador para abrir el menú
        if (!(sender instanceof Player player)) {
            sender.sendMessage("[!] El terminal requiere un operario humano para invocar el menú.");
            return true;
        }

        // 🌟 VALIDACIÓN VOID REACH (Uso puro de la dependencia inyectada)
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

        // 🌟 ABRE EL MENÚ SEGURO
        new BlackMarketMenu(player, plugin, blackMarketManager, ecoManager, crossplayUtils).open();

        return true;
    }
}