package me.nexo.core.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.config.ConfigManager;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.UserRepository;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Subcommand;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 🏛️ Nexo Network - Comando Web (Arquitectura Enterprise / Lamp Framework)
 * Encriptación 100% asíncrona mediante Hilos Virtuales, Inyección DI Pura y Configs Type-Safe.
 */
@Singleton // 🌟 FIX: Instancia única para el Command Handler
@Command("web")
public class WebCommand {

    private final UserRepository userRepository;
    private final ConfigManager configManager;
    private final CrossplayUtils crossplayUtils;
    
    // 🚀 Motor de concurrencia Zero-Lag
    private final ExecutorService virtualExecutor;

    // 💉 PILAR 1: Inyección completa, eliminando el uso estático de CrossplayUtils
    @Inject
    public WebCommand(UserRepository userRepository, ConfigManager configManager, CrossplayUtils crossplayUtils) {
        this.userRepository = userRepository;
        this.configManager = configManager;
        this.crossplayUtils = crossplayUtils;
        this.virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @Subcommand("register")
    public void register(Player player, String password) {

        // 🚀 PILAR 3: Programación Reactiva en Hilos Virtuales
        CompletableFuture.supplyAsync(() -> {
            try {
                return hashPassword(password);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, virtualExecutor).thenCompose(hashedPassword ->
                userRepository.updateWebPassword(player.getUniqueId(), hashedPassword)
        ).thenAcceptAsync(success -> {
            // 🛡️ PILAR 2: Lectura Mágica Type-Safe con la instancia inyectada de CrossplayUtils
            if (success) {
                crossplayUtils.sendMessage(player, configManager.getMessages().comandos().web().exito1());
                crossplayUtils.sendMessage(player, configManager.getMessages().comandos().web().exito2());
            } else {
                crossplayUtils.sendMessage(player, configManager.getMessages().comandos().web().errorDb());
            }
        }, virtualExecutor).exceptionally(ex -> {
            crossplayUtils.sendMessage(player, configManager.getMessages().comandos().web().errorCritico());
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