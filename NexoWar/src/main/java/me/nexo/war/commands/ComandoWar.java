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
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ⚔️ NexoWar - Comando Principal (Arquitectura NATIVA)
 * Cero CommandExecutor, Cero Lamp. 100% Nativo y Asíncrono.
 */
@Singleton
public class ComandoWar extends Command {

    private final UserManager userManager;
    private final ConfigManager configManager;
    private final WarManager warManager;
    private final NexoCore core;

    private final Map<UUID, DesafioPendiente> desafiosPendientes = new ConcurrentHashMap<>();
    private record DesafioPendiente(UUID clanAtacanteId, BigDecimal apuesta) {}

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public ComandoWar(UserManager userManager, ConfigManager configManager, WarManager warManager, NexoCore core) {
        super("war"); // 🌟 Nombre nativo base
        this.setAliases(List.of("guerra")); // Alias nativos

        this.userManager = userManager;
        this.configManager = configManager;
        this.warManager = warManager;
        this.core = core;
    }

    // ==========================================
    // ⚙️ MOTOR DE EJECUCIÓN NATIVO
    // ==========================================
    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c[!] El terminal no puede participar en guerras físicas.");
            return true;
        }

        if (args.length == 0) {
            ayuda(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "challenge" -> {
                if (args.length < 3) {
                    CrossplayUtils.sendMessage(player, "&#FF5555[!] Uso correcto: /war challenge <tag_del_clan> <apuesta>");
                    return true;
                }

                String targetTag = args[1];
                BigDecimal apuesta;
                try {
                    apuesta = new BigDecimal(args[2]);
                } catch (NumberFormatException e) {
                    CrossplayUtils.sendMessage(player, configManager.getMessages().errores().apuestaInvalida());
                    return true;
                }

                challenge(player, targetTag, apuesta);
            }
            case "accept" -> accept(player);
            default -> ayuda(player);
        }

        return true;
    }

    // ==========================================
    // 🧠 MOTOR DE AUTOCOMPLETADO NATIVO
    // ==========================================
    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        if (args.length == 1) {
            return List.of("challenge", "accept").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("challenge")) {
            return List.of("<tag_clan>");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("challenge")) {
            return List.of("<cantidad_apuesta>");
        }
        return Collections.emptyList();
    }

    // ==========================================
    // 🛠️ LÓGICA DE SUBCOMANDOS (Mantenida intacta)
    // ==========================================
    private void ayuda(Player player) {
        for (String line : configManager.getMessages().ayuda().comandoWar()) {
            CrossplayUtils.sendMessage(player, line);
        }
    }

    private void challenge(Player player, String targetTag, BigDecimal apuesta) {
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

        // 🚀 Operación Asíncrona pura
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

    private void accept(Player player) {
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