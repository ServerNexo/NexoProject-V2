package me.nexo.war.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.clans.core.ClanManager;
import me.nexo.clans.core.NexoClan;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.database.DatabaseManager;
import me.nexo.core.user.NexoUser;
import me.nexo.core.user.UserManager;
import me.nexo.war.config.ConfigManager;
import me.nexo.war.managers.WarManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.DefaultFor;
import revxrsal.commands.annotation.Subcommand;


import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ⚔️ NexoWar - Comando Principal (Arquitectura Enterprise)
 * Cero CommandExecutor, cero Service Locators estáticos. 100% DI y Hilos Virtuales.
 */
@Singleton
@Command({"war", "guerra"})
public class ComandoWar {

    private final UserManager userManager;
    private final ConfigManager configManager;
    private final WarManager warManager;
    
    // 🌟 Sinergia de Módulos (Inyectados directamente desde el Core)
    private final ClanManager clanManager;
    private final DatabaseManager dbManager;
    private final CrossplayUtils crossplayUtils;

    private final Map<UUID, DesafioPendiente> desafiosPendientes = new ConcurrentHashMap<>();
    private record DesafioPendiente(UUID clanAtacanteId, BigDecimal apuesta) {}

    // 🚀 Motor de concurrencia Zero-Lag
    private final ExecutorService virtualExecutor;

    // 💉 PILAR 1: Inyección de Dependencias Estricta
    @Inject
    public ComandoWar(UserManager userManager, ConfigManager configManager, WarManager warManager, 
                      ClanManager clanManager, DatabaseManager dbManager, CrossplayUtils crossplayUtils) {
        this.userManager = userManager;
        this.configManager = configManager;
        this.warManager = warManager;
        this.clanManager = clanManager;
        this.dbManager = dbManager;
        this.crossplayUtils = crossplayUtils;
        this.virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    // 💡 PILAR 1: Lamp maneja el comando base automáticamente con "~"
    @DefaultFor("~")
    public void ayuda(Player player) {
        for (String line : configManager.getMessages().ayuda().comandoWar()) {
            crossplayUtils.sendMessage(player, line);
        }
    }

    // 💡 Lamp autoconvierte el argumento "apuesta" en un BigDecimal
    @Subcommand("challenge")
    public void challenge(Player player, String targetTag, BigDecimal apuesta) {
        NexoUser user = userManager.getUserOrNull(player.getUniqueId());

        if (user == null || !user.hasClan()) {
            crossplayUtils.sendMessage(player, configManager.getMessages().errores().sinClan());
            return;
        }

        if (!user.getClanRole().equals("LIDER") && !user.getClanRole().equals("OFICIAL")) {
            crossplayUtils.sendMessage(player, configManager.getMessages().errores().rangoInsuficiente());
            return;
        }

        if (apuesta.compareTo(BigDecimal.ZERO) <= 0) {
            crossplayUtils.sendMessage(player, configManager.getMessages().errores().apuestaInvalida());
            return;
        }

        Optional<NexoClan> atacanteOpt = clanManager.getClanFromCache(user.getClanId());
        if (atacanteOpt.isEmpty()) return;
        NexoClan atacante = atacanteOpt.get();

        if (atacante.getBankBalance().compareTo(apuesta) < 0) {
            crossplayUtils.sendMessage(player, configManager.getMessages().errores().fondosInsuficientes());
            return;
        }

        crossplayUtils.sendMessage(player, configManager.getMessages().procesos().escaneandoRed());

        // 🚀 PILAR 3: Operación a la BD Asíncrona administrada en Hilos Virtuales
        CompletableFuture.runAsync(() -> {
            String sql = "SELECT id FROM nexo_clans WHERE tag = ?";
            
            // 🛡️ FIX: ResultSet cerrado automáticamente para evitar Cursor Leaks
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, targetTag);
                
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        UUID targetId = UUID.fromString(rs.getString("id"));
                        if (targetId.equals(atacante.getId())) {
                            crossplayUtils.sendMessage(player, configManager.getMessages().errores().autoAtaque());
                            return;
                        }

                        clanManager.loadClanAsync(targetId, defensor -> {
                            if (defensor == null) return;
                            if (defensor.getBankBalance().compareTo(apuesta) < 0) {
                                String msg = configManager.getMessages().errores().objetivoSinFondos().replace("%apuesta%", apuesta.toPlainString());
                                crossplayUtils.sendMessage(player, msg);
                                return;
                            }

                            desafiosPendientes.put(targetId, new DesafioPendiente(atacante.getId(), apuesta));
                            String msgEmitido = configManager.getMessages().exito().contratoEmitido().replace("%defensor%", defensor.getName());
                            crossplayUtils.sendMessage(player, msgEmitido);

                            // ALERTA AL CULTO DEFENSOR
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                NexoUser tu = userManager.getUserOrNull(p.getUniqueId());
                                if (tu != null && tu.getClanId() != null && tu.getClanId().equals(targetId) && (tu.getClanRole().equals("LIDER") || tu.getClanRole().equals("OFICIAL"))) {
                                    for (String line : configManager.getMessages().alertas().declaracionGuerra()) {
                                        String alertMsg = line.replace("%atacante%", atacante.getName()).replace("%apuesta%", apuesta.toPlainString());
                                        crossplayUtils.sendMessage(p, alertMsg);
                                    }
                                }
                            }
                        });
                    } else {
                        String msgNoEncontrado = configManager.getMessages().errores().objetivoNoEncontrado().replace("%tag%", targetTag);
                        crossplayUtils.sendMessage(player, msgNoEncontrado);
                    }
                }
            } catch (Exception e) {
                crossplayUtils.sendMessage(player, configManager.getMessages().errores().errorBaseDatos());
            }
        }, virtualExecutor); // 🌟 Motor asignado
    }

    @Subcommand("accept")
    public void accept(Player player) {
        NexoUser user = userManager.getUserOrNull(player.getUniqueId());

        if (user == null || !user.hasClan()) {
            crossplayUtils.sendMessage(player, configManager.getMessages().errores().sinClan());
            return;
        }

        if (!user.getClanRole().equals("LIDER") && !user.getClanRole().equals("OFICIAL")) {
            crossplayUtils.sendMessage(player, configManager.getMessages().errores().rangoInsuficiente());
            return;
        }

        DesafioPendiente desafio = desafiosPendientes.remove(user.getClanId());
        if (desafio == null) {
            crossplayUtils.sendMessage(player, configManager.getMessages().errores().sinContratos());
            return;
        }

        crossplayUtils.sendMessage(player, configManager.getMessages().procesos().iniciandoDespliegue());

        // 🛡️ El ClanManager inyectado asegura alta velocidad de acceso a la RAM
        clanManager.getClanFromCache(user.getClanId()).ifPresent(defensor -> {
            clanManager.loadClanAsync(desafio.clanAtacanteId(), atacante -> {
                if (atacante != null) {
                    warManager.iniciarDesafio(player, atacante, defensor, desafio.apuesta());
                }
            });
        });
    }
}