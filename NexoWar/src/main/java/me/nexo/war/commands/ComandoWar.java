package me.nexo.war.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.clans.core.ClanManager;
import me.nexo.clans.core.NexoClan;
import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.NexoAPI;
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

/**
 * ⚔️ NexoWar - Comando Principal (Arquitectura Enterprise)
 * Cero CommandExecutor. 100% Lamp, Guice y Type-Safe.
 */
@Singleton
@Command({"war", "guerra"})
public class ComandoWar {

    private final UserManager userManager;
    private final ConfigManager configManager;
    private final WarManager warManager;
    private final NexoCore core;

    private final Map<UUID, DesafioPendiente> desafiosPendientes = new ConcurrentHashMap<>();
    private record DesafioPendiente(UUID clanAtacanteId, BigDecimal apuesta) {}

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public ComandoWar(UserManager userManager, ConfigManager configManager, WarManager warManager, NexoCore core) {
        this.userManager = userManager;
        this.configManager = configManager;
        this.warManager = warManager;
        this.core = core;
    }

    // 💡 PILAR 1: Lamp maneja el comando base automáticamente con "~"
    @DefaultFor("~")
    public void ayuda(Player player) {
        for (String line : configManager.getMessages().ayuda().comandoWar()) {
            CrossplayUtils.sendMessage(player, line);
        }
    }

    // 💡 Lamp autoconvierte el argumento "apuesta" en un BigDecimal
    @Subcommand("challenge")
    public void challenge(Player player, String targetTag, BigDecimal apuesta) {
        NexoUser user = userManager.getUserOrNull(player.getUniqueId());

        if (user == null || !user.hasClan()) {
            CrossplayUtils.sendMessage(player, configManager.getMessages().errores().sinClan());
            return;
        }

        if (!user.getClanRole().equals("LIDER") && !user.getClanRole().equals("OFICIAL")) {
            CrossplayUtils.sendMessage(player, configManager.getMessages().errores().rangoInsuficiente());
            return;
        }

        if (apuesta.compareTo(BigDecimal.ZERO) <= 0) {
            CrossplayUtils.sendMessage(player, configManager.getMessages().errores().apuestaInvalida());
            return;
        }

        Optional<ClanManager> clanManagerOpt = NexoAPI.getServices().get(ClanManager.class);
        if (clanManagerOpt.isEmpty()) {
            CrossplayUtils.sendMessage(player, configManager.getMessages().errores().servicioClanesOffline());
            return;
        }
        ClanManager clanManager = clanManagerOpt.get();

        Optional<NexoClan> atacanteOpt = clanManager.getClanFromCache(user.getClanId());
        if (atacanteOpt.isEmpty()) return;
        NexoClan atacante = atacanteOpt.get();

        if (atacante.getBankBalance().compareTo(apuesta) < 0) {
            CrossplayUtils.sendMessage(player, configManager.getMessages().errores().fondosInsuficientes());
            return;
        }

        CrossplayUtils.sendMessage(player, configManager.getMessages().procesos().escaneandoRed());

        // 🚀 PILAR 4: Operación a la BD Asíncrona pura
        CompletableFuture.runAsync(() -> {
            String sql = "SELECT id FROM nexo_clans WHERE tag = ?";
            try (Connection conn = core.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, targetTag);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    UUID targetId = UUID.fromString(rs.getString("id"));
                    if (targetId.equals(atacante.getId())) {
                        CrossplayUtils.sendMessage(player, configManager.getMessages().errores().autoAtaque());
                        return;
                    }

                    clanManager.loadClanAsync(targetId, defensor -> {
                        if (defensor == null) return;
                        if (defensor.getBankBalance().compareTo(apuesta) < 0) {
                            String msg = configManager.getMessages().errores().objetivoSinFondos().replace("%apuesta%", apuesta.toPlainString());
                            CrossplayUtils.sendMessage(player, msg);
                            return;
                        }

                        desafiosPendientes.put(targetId, new DesafioPendiente(atacante.getId(), apuesta));
                        String msgEmitido = configManager.getMessages().exito().contratoEmitido().replace("%defensor%", defensor.getName());
                        CrossplayUtils.sendMessage(player, msgEmitido);

                        // ALERTA AL CULTO DEFENSOR
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            NexoUser tu = userManager.getUserOrNull(p.getUniqueId());
                            if (tu != null && tu.getClanId() != null && tu.getClanId().equals(targetId) && (tu.getClanRole().equals("LIDER") || tu.getClanRole().equals("OFICIAL"))) {
                                for (String line : configManager.getMessages().alertas().declaracionGuerra()) {
                                    String alertMsg = line.replace("%atacante%", atacante.getName()).replace("%apuesta%", apuesta.toPlainString());
                                    CrossplayUtils.sendMessage(p, alertMsg);
                                }
                            }
                        }
                    });
                } else {
                    String msgNoEncontrado = configManager.getMessages().errores().objetivoNoEncontrado().replace("%tag%", targetTag);
                    CrossplayUtils.sendMessage(player, msgNoEncontrado);
                }
            } catch (Exception e) {
                CrossplayUtils.sendMessage(player, configManager.getMessages().errores().errorBaseDatos());
            }
        });
    }

    @Subcommand("accept")
    public void accept(Player player) {
        NexoUser user = userManager.getUserOrNull(player.getUniqueId());

        if (user == null || !user.hasClan()) {
            CrossplayUtils.sendMessage(player, configManager.getMessages().errores().sinClan());
            return;
        }

        if (!user.getClanRole().equals("LIDER") && !user.getClanRole().equals("OFICIAL")) {
            CrossplayUtils.sendMessage(player, configManager.getMessages().errores().rangoInsuficiente());
            return;
        }

        DesafioPendiente desafio = desafiosPendientes.remove(user.getClanId());
        if (desafio == null) {
            CrossplayUtils.sendMessage(player, configManager.getMessages().errores().sinContratos());
            return;
        }

        CrossplayUtils.sendMessage(player, configManager.getMessages().procesos().iniciandoDespliegue());

        Optional<ClanManager> clanManagerOpt = NexoAPI.getServices().get(ClanManager.class);
        if (clanManagerOpt.isEmpty()) return;
        ClanManager clanManager = clanManagerOpt.get();

        clanManager.getClanFromCache(user.getClanId()).ifPresent(defensor -> {
            clanManager.loadClanAsync(desafio.clanAtacanteId(), atacante -> {
                if (atacante != null) {
                    warManager.iniciarDesafio(player, atacante, defensor, desafio.apuesta());
                }
            });
        });
    }
}