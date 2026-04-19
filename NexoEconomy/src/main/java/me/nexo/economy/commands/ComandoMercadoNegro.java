package me.nexo.economy.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.NexoUser;
import me.nexo.core.user.UserManager;
import me.nexo.economy.NexoEconomy;
import me.nexo.economy.blackmarket.BlackMarketManager;
import me.nexo.economy.blackmarket.BlackMarketMenu;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * 💰 NexoEconomy - Comando del Mercado Negro (Arquitectura NATIVA)
 * Fusión de Ejecución y Autocompletado, bypassing estricto de PaperMC.
 */
@Singleton
public class ComandoMercadoNegro extends Command {

    private final NexoEconomy plugin;
    private final BlackMarketManager blackMarketManager;
    private final UserManager userManager;

    private static final List<String> ADMIN_COMMANDS = List.of("open", "close");

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public ComandoMercadoNegro(NexoEconomy plugin, BlackMarketManager blackMarketManager, UserManager userManager) {
        super("mercadonegro"); // 🌟 Nombre nativo
        this.setAliases(List.of("blackmarket", "bm")); // Alias extra

        this.plugin = plugin;
        this.blackMarketManager = blackMarketManager;
        this.userManager = userManager; // 🌟 FIX: Inyectado para evitar llamadas estáticas lentas
    }

    // ==========================================
    // ⚙️ MOTOR DE EJECUCIÓN NATIVO
    // ==========================================
    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {

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

        // 🌟 VALIDACIÓN VOID REACH (Acceso Remoto Protegido en O(1))
        NexoUser user = userManager.getUserOrNull(player.getUniqueId());

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

    // ==========================================
    // 🧠 MOTOR DE AUTOCOMPLETADO NATIVO DIRECTO
    // ==========================================
    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        // Solo autocompletamos los comandos administrativos para los que tengan permiso
        if (args.length == 1 && sender.hasPermission("nexoeconomy.admin")) {
            return ADMIN_COMMANDS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        return Collections.emptyList();
    }
}