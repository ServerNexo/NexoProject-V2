package me.nexo.economy.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.NexoUser;
import me.nexo.economy.NexoEconomy;
import me.nexo.economy.blackmarket.BlackMarketManager;
import me.nexo.economy.blackmarket.BlackMarketMenu;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * 💰 NexoEconomy - Comando del Mercado Negro (Arquitectura Enterprise)
 */
@Singleton
public class ComandoMercadoNegro implements CommandExecutor {

    private final NexoEconomy plugin;
    private final BlackMarketManager blackMarketManager;

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public ComandoMercadoNegro(NexoEconomy plugin, BlackMarketManager blackMarketManager) {
        this.plugin = plugin;
        this.blackMarketManager = blackMarketManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // 🌟 COMANDOS DE ADMINISTRADOR
        if (args.length > 0 && sender.hasPermission("nexoeconomy.admin")) {
            if (args[0].equalsIgnoreCase("open")) {
                blackMarketManager.openMarket();
                sender.sendMessage("§8[§5✓§8] §dHas invocado al Mercader Oscuro.");
                return true;
            }
            if (args[0].equalsIgnoreCase("close")) {
                blackMarketManager.closeMarket();
                sender.sendMessage("§8[§c✓§8] §cHas desterrado al Mercader Oscuro.");
                return true;
            }
        }

        // 🌟 FILTRO DE CONSOLA
        if (!(sender instanceof Player player)) {
            sender.sendMessage("[!] El terminal requiere un operario humano para invocar el menú.");
            return true;
        }

        // 🌟 VALIDACIÓN VOID REACH (Acceso Remoto Protegido)
        NexoUser user = NexoCore.getPlugin(NexoCore.class).getUserManager().getUserOrNull(player.getUniqueId());
        if (user == null || !user.isVoidBlessingActive()) {
            CrossplayUtils.sendMessage(player, "&#8b0000[!] <bold>ACCESO DENEGADO:</bold> &#E6CCFFEl Mercado Negro remoto requiere la Bendición del Vacío activa.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return true;
        }

        // Validar si el mercader está activo antes de abrir el menú
        if (!blackMarketManager.isMarketOpen()) {
            CrossplayUtils.sendMessage(player, "&#FF5555[!] El mercader no se encuentra en este plano. Vuelve más tarde.");
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_AMBIENT, 1.0f, 0.5f);
            return true;
        }

        // 🌟 ABRE EL MENÚ SEGURO
        new BlackMarketMenu(player, plugin).open();

        return true;
    }
}