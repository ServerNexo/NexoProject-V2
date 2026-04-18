package me.nexo.core.commands;

import com.google.inject.Inject;
import me.nexo.core.config.ConfigManager;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.UserRepository;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Subcommand;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.CompletableFuture;

/**
 * 🏛️ Nexo Network - Comando Web (Arquitectura Enterprise / Lamp Framework)
 * Encriptación 100% asíncrona, Inyección DI y Configuraciones Type-Safe (Pilar 2).
 */
@Command("web")
public class WebCommand {

    // 💉 PILAR 3: Inyectamos el DAO y nuestro nuevo Gestor de Textos
    private final UserRepository userRepository;
    private final ConfigManager configManager;

    @Inject
    public WebCommand(UserRepository userRepository, ConfigManager configManager) {
        this.userRepository = userRepository;
        this.configManager = configManager;
    }

    @Subcommand("register")
    public void register(Player player, String password) {

        // 🚀 PILAR 4: Programación Reactiva Asíncrona
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