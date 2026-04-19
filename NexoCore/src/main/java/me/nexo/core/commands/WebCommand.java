package me.nexo.core.commands;

import com.google.inject.Inject;
import me.nexo.core.config.ConfigManager;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.UserRepository;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 🏛️ Nexo Network - Comando Web (Arquitectura NATIVA)
 * Encriptación 100% asíncrona, Inyección DI y Configuraciones Type-Safe.
 */
public class WebCommand extends Command {

    // 💉 PILAR 3: Inyectamos el DAO y nuestro Gestor de Textos
    private final UserRepository userRepository;
    private final ConfigManager configManager;

    @Inject
    public WebCommand(UserRepository userRepository, ConfigManager configManager) {
        super("web"); // Nombre nativo
        this.setUsage("/web register <contraseña>");

        this.userRepository = userRepository;
        this.configManager = configManager;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cEste comando solo puede ser usado por jugadores.");
            return true;
        }

        if (args.length != 2 || !args[0].equalsIgnoreCase("register")) {
            CrossplayUtils.sendMessage(player, "§cUso correcto: " + this.getUsage());
            return true;
        }

        String password = args[1];

        // 🚀 PILAR 4: Programación Reactiva Asíncrona intacta
        CompletableFuture.supplyAsync(() -> {
            try {
                return hashPassword(password);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).thenCompose(hashedPassword ->
                userRepository.updateWebPassword(player.getUniqueId(), hashedPassword)
        ).thenAccept(success -> {
            // 🛡️ PILAR 2: Lectura Mágica Type-Safe + Colores Hex/Crossplay
            if (success) {
                CrossplayUtils.sendMessage(player, configManager.getMessages().comandos().web().exito1());
                CrossplayUtils.sendMessage(player, configManager.getMessages().comandos().web().exito2());
            } else {
                CrossplayUtils.sendMessage(player, configManager.getMessages().comandos().web().errorDb());
            }
        }).exceptionally(ex -> {
            CrossplayUtils.sendMessage(player, configManager.getMessages().comandos().web().errorCritico());
            ex.printStackTrace();
            return null;
        });

        return true;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        if (args.length == 1) {
            if ("register".startsWith(args[0].toLowerCase())) {
                return Arrays.asList("register");
            }
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("register")) {
            return Arrays.asList("<tu_contraseña_secreta>"); // Guía visual de autocompletado
        }
        return new ArrayList<>();
    }

    // 🔒 Encriptación SHA-256 (Nativa de Java, Cero Dependencias)
    private String hashPassword(String password) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encodedHash = digest.digest(password.getBytes(StandardCharsets.UTF_8));

        StringBuilder hexString = new StringBuilder(2 * encodedHash.length);
        for (byte b : encodedHash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}