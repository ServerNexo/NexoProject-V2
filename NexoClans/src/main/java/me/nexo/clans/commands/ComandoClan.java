package me.nexo.clans.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.clans.NexoClans;
import me.nexo.clans.core.ClanManager;
import me.nexo.clans.menu.ClanMenu;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.database.DatabaseManager;
import me.nexo.core.user.NexoUser;
import me.nexo.core.user.UserManager;
import me.nexo.economy.managers.EconomyManager; // Sinergia inyectada del módulo Economy
import me.nexo.economy.core.NexoAccount;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 👥 NexoClans - Comando Principal de Facciones (Arquitectura Enterprise)
 * Rendimiento: Switch Java 21, Inyección Nativa PaperMC, Cero Lag I/O y TabCompleter Unificado.
 */
@Singleton
public class ComandoClan extends Command {

    private final NexoClans plugin;
    private final ClanManager clanManager;
    private final UserManager userManager;
    private final DatabaseManager dbManager;
    private final EconomyManager economyManager;
    private final CrossplayUtils crossplayUtils;

    // 🌟 FIX: Gestor formal de Hilos Virtuales
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // 🌟 FIX: TabCompleter inmutable y cacheado (Nativo Java 16+)
    private static final List<String> SUB_COMMANDS = List.of(
            "create", "invite", "join", "leave", "ff", "friendlyfire",
            "kick", "disband", "deposit", "withdraw", "sethome", "home", "tribute"
    );

    // 💉 PILAR 1: Inyección de Dependencias Pura y Estructura Nativa
    @Inject
    public ComandoClan(NexoClans plugin, ClanManager clanManager, UserManager userManager,
                       DatabaseManager dbManager, EconomyManager economyManager, CrossplayUtils crossplayUtils) {
        super("clan"); // Nombre base del comando inyectado al CommandMap
        this.setAliases(List.of("f", "faction", "faccion")); // Alias nativos
        this.setDescription("Panel de control y gestión de facciones.");

        this.plugin = plugin;
        this.clanManager = clanManager;
        this.userManager = userManager;
        this.dbManager = dbManager;
        this.economyManager = economyManager;
        this.crossplayUtils = crossplayUtils;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("[!] El comando de clanes solo es accesible por entidades físicas.");
            return true;
        }

        // Lectura O(1) en RAM inyectada
        NexoUser user = userManager.getUserOrNull(player.getUniqueId());

        if (user == null) {
            crossplayUtils.sendMessage(player, "&#FF5555[!] Tus datos aún están siendo descargados del Nexo. Espera un momento.");
            return true;
        }

        // ==========================================
        // 🔮 APERTURA DEL MENÚ PRINCIPAL
        // ==========================================
        if (args.length == 0) {
            if (user.hasClan()) {
                clanManager.getClanFromCache(user.getClanId()).ifPresentOrElse(
                        clan -> new ClanMenu(player, clan, user, clanManager, crossplayUtils).open(),
                        () -> crossplayUtils.sendMessage(player, "&#FFAA00[!] Cargando datos holográficos de la facción...")
                );
            } else {
                crossplayUtils.sendMessage(player, "&#FF5555[!] No perteneces a ninguna facción. Usa /clan create o /clan join.");
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();

        // 🌟 OPTIMIZACIÓN: Switch nativo de Java 21 para mayor rendimiento
        switch (subCommand) {

            case "create" -> {
                if (user.hasClan()) {
                    crossplayUtils.sendMessage(player, "&#FF5555[!] Ya juraste lealtad a un clan. Abandónalo primero.");
                    return true;
                }
                if (args.length < 3) {
                    crossplayUtils.sendMessage(player, "&#FFAA00[!] Uso: /clan create <TAG> <Nombre>");
                    return true;
                }
                String tag = args[1].toUpperCase();
                if (tag.length() < 2 || tag.length() > 4 || !tag.matches("^[A-Z0-9]+$")) {
                    crossplayUtils.sendMessage(player, "&#FF5555[!] El TAG debe tener entre 2 y 4 caracteres alfanuméricos.");
                    return true;
                }
                String nombreRaw = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                String nombreLimpio = nombreRaw.replaceAll("&#[a-fA-F0-9]{6}", "").replaceAll("&[0-9a-fk-orA-FK-OR]", "");

                if (nombreLimpio.length() < 3 || nombreLimpio.length() > 16) {
                    crossplayUtils.sendMessage(player, "&#FF5555[!] El nombre de la facción debe tener entre 3 y 16 letras.");
                    return true;
                }
                if (!nombreLimpio.matches("^[a-zA-Z0-9 ]+$")) {
                    crossplayUtils.sendMessage(player, "&#FF5555[!] El nombre contiene caracteres prohibidos.");
                    return true;
                }
                clanManager.crearClanAsync(player, user, tag, nombreRaw);
            }

            case "invite" -> {
                if (!user.hasClan() || (!"LIDER".equals(user.getClanRole()) && !"OFICIAL".equals(user.getClanRole()))) {
                    crossplayUtils.sendMessage(player, "&#FF5555[!] Tu rango no tiene autoridad para reclutar.");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target != null) {
                    clanManager.getClanFromCache(user.getClanId()).ifPresent(clan -> clanManager.invitarJugador(player, target, clan));
                } else {
                    crossplayUtils.sendMessage(player, "&#FF5555[!] El jugador especificado no se encuentra en línea.");
                }
            }

            case "join" -> {
                UUID inv = clanManager.getInvitacionPendiente(player);
                if (inv != null) {
                    clanManager.loadClanAsync(inv, clan -> clanManager.unirseClanAsync(player, user, clan));
                } else {
                    crossplayUtils.sendMessage(player, "&#FF5555[!] No tienes invitaciones pendientes.");
                }
            }

            case "leave" -> {
                if (user.hasClan() && !"LIDER".equals(user.getClanRole())) {
                    clanManager.abandonarClanAsync(player, user);
                } else if ("LIDER".equals(user.getClanRole())) {
                    crossplayUtils.sendMessage(player, "&#FF5555[!] El líder no puede abandonar. Nombra a otro líder o disuelve el clan (/clan disband).");
                }
            }

            case "ff", "friendlyfire" -> {
                if (user.hasClan() && ("LIDER".equals(user.getClanRole()) || "OFICIAL".equals(user.getClanRole()))) {
                    clanManager.getClanFromCache(user.getClanId()).ifPresent(clan -> clanManager.toggleFriendlyFireAsync(clan, player, !clan.isFriendlyFire()));
                } else {
                    crossplayUtils.sendMessage(player, "&#FF5555[!] Solo los altos mandos pueden alternar el riesgo de sangre.");
                }
            }

            case "kick" -> {
                if (!user.hasClan() || (!"LIDER".equals(user.getClanRole()) && !"OFICIAL".equals(user.getClanRole()))) {
                    crossplayUtils.sendMessage(player, "&#FF5555[!] Tu rango no tiene autoridad para exiliar.");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target != null) {
                    NexoUser tUser = userManager.getUserOrNull(target.getUniqueId());
                    if (tUser != null && tUser.getClanId() != null && tUser.getClanId().equals(user.getClanId())) {
                        clanManager.expulsarJugadorAsync(player, target, tUser);
                    }
                }
            }

            case "disband" -> {
                if (user.hasClan() && "LIDER".equals(user.getClanRole())) {
                    clanManager.disolverClanAsync(player, user, user.getClanId());
                } else {
                    crossplayUtils.sendMessage(player, "&#FF5555[!] Solo el Líder absoluto puede disolver la facción.");
                }
            }

            // ==========================================
            // 💰 ECONOMÍA LOCK-FREE BANCARIA
            // ==========================================
            case "deposit" -> {
                if (!user.hasClan()) return true;
                if (args.length < 2) {
                    crossplayUtils.sendMessage(player, "&#FFAA00[!] Uso: /clan deposit <cantidad>");
                    return true;
                }
                try {
                    var amount = new BigDecimal(args[1]);
                    if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();

                    economyManager.updateBalanceAsync(player.getUniqueId(), NexoAccount.AccountType.PLAYER, NexoAccount.Currency.COINS, amount, false).thenAccept(success -> {
                        if (success) {
                            clanManager.getClanFromCache(user.getClanId()).ifPresent(clan -> {
                                clan.depositMoney(amount.doubleValue());
                                clanManager.saveBankAsync(clan);
                                crossplayUtils.sendMessage(player, "&#55FF55[✓] Has depositado " + amount + " monedas al tesoro de la facción.");
                            });
                        } else {
                            crossplayUtils.sendMessage(player, "&#FF5555[!] Fondos insuficientes en tus bolsillos.");
                        }
                    });
                } catch (Exception e) {
                    crossplayUtils.sendMessage(player, "&#FF5555[!] Cantidad monetaria inválida.");
                }
            }

            case "withdraw" -> {
                if (!user.hasClan() || (!"LIDER".equals(user.getClanRole()) && !"OFICIAL".equals(user.getClanRole()))) {
                    crossplayUtils.sendMessage(player, "&#FF5555[!] Solo los altos mandos tienen la llave de la tesorería.");
                    return true;
                }
                if (args.length < 2) {
                    crossplayUtils.sendMessage(player, "&#FFAA00[!] Uso: /clan withdraw <cantidad>");
                    return true;
                }
                try {
                    var amount = new BigDecimal(args[1]);
                    if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();

                    clanManager.getClanFromCache(user.getClanId()).ifPresent(clan -> {
                        if (!clan.hasEnoughMoney(amount.doubleValue())) {
                            crossplayUtils.sendMessage(player, "&#FF5555[!] La bóveda del clan no tiene suficientes fondos.");
                            return;
                        }
                        clan.withdrawMoney(amount.doubleValue());
                        clanManager.saveBankAsync(clan);

                        economyManager.updateBalanceAsync(player.getUniqueId(), NexoAccount.AccountType.PLAYER, NexoAccount.Currency.COINS, amount, true);
                        crossplayUtils.sendMessage(player, "&#55FF55[✓] Has retirado " + amount + " monedas de la tesorería.");
                    });
                } catch (Exception e) {
                    crossplayUtils.sendMessage(player, "&#FF5555[!] Cantidad monetaria inválida.");
                }
            }

            // ==========================================
            // 📍 UBICACIÓN Y BASES
            // ==========================================
            case "sethome" -> {
                if (!user.hasClan() || !"LIDER".equals(user.getClanRole())) {
                    crossplayUtils.sendMessage(player, "&#FF5555[!] Solo el Líder puede clavar el estandarte de la facción.");
                    return true;
                }
                clanManager.getClanFromCache(user.getClanId()).ifPresent(clan -> clanManager.setClanHomeAsync(clan, player, player.getLocation()));
            }

            case "home" -> {
                if (!user.hasClan()) return true;
                clanManager.getClanFromCache(user.getClanId()).ifPresent(clan -> {
                    if (clan.getPublicHome() == null || clan.getPublicHome().isEmpty()) {
                        crossplayUtils.sendMessage(player, "&#FF5555[!] Tu facción aún no tiene una base establecida.");
                        return;
                    }
                    try {
                        String[] parts = clan.getPublicHome().split(";");
                        World w = Bukkit.getWorld(parts[0]);
                        double x = Double.parseDouble(parts[1]);
                        double y = Double.parseDouble(parts[2]);
                        double z = Double.parseDouble(parts[3]);
                        float yaw = Float.parseFloat(parts[4]);
                        float pitch = Float.parseFloat(parts[5]);

                        player.teleportAsync(new Location(w, x, y, z, yaw, pitch)).thenAccept(success -> {
                            if (success) crossplayUtils.sendMessage(player, "&#55FF55[✈] Telediportado a la base del clan.");
                        });
                    } catch (Exception e) {
                        crossplayUtils.sendMessage(player, "&#FF5555[!] Error espacio-temporal al calcular las coordenadas de la base.");
                    }
                });
            }

            // ==========================================
            // 🔮 TRIBUTOS AL MONOLITO
            // ==========================================
            case "tribute", "tributo" -> {
                if (!user.hasClan()) return true;

                ItemStack itemEnMano = player.getInventory().getItemInMainHand();
                if (itemEnMano.getType() == Material.AIR) {
                    crossplayUtils.sendMessage(player, "&#FF5555[!] Debes sostener en la mano el material que deseas sacrificar.");
                    return true;
                }

                long expPorItem = 1;
                String tipo = itemEnMano.getType().name();
                if (tipo.contains("DIAMOND") || tipo.contains("EMERALD")) expPorItem = 50;
                if (tipo.contains("IRON") || tipo.contains("GOLD")) expPorItem = 10;
                if (itemEnMano.hasItemMeta() && itemEnMano.getItemMeta().hasDisplayName()) expPorItem = 100;

                long expTotal = expPorItem * itemEnMano.getAmount();

                clanManager.getClanFromCache(user.getClanId()).ifPresent(clan -> {
                    boolean subioNivel = clan.addMonolithExp(expTotal);
                    player.getInventory().setItemInMainHand(null); // Consumimos el ítem

                    crossplayUtils.sendMessage(player, "&#55FF55[✓] <bold>SACRIFICIO ACEPTADO:</bold> &#E6CCFFEl Monolito absorbió &#FFAA00" + expTotal + " de Poder.");

                    if (subioNivel) {
                        // 🌟 FIX: Envío global moderno con MiniMessage
                        Bukkit.broadcast(crossplayUtils.parseCrossplay(null, " "));
                        Bukkit.broadcast(crossplayUtils.parseCrossplay(null, "&#ff00ff🏆 <bold>EVOLUCIÓN DE FACCIÓN</bold>"));
                        Bukkit.broadcast(crossplayUtils.parseCrossplay(null, "&#E6CCFFEl clan &#55FF55" + clan.getName() + " &#E6CCFFha ascendido al Nivel &#FFAA00" + clan.getMonolithLevel() + "&#E6CCFF."));
                        Bukkit.broadcast(crossplayUtils.parseCrossplay(null, " "));
                    }

                    // 🌟 FIX: Hilos Virtuales controlados por Executor para guardar en DB Inyectada
                    virtualExecutor.submit(() -> {
                        String sql = "UPDATE nexo_clans SET monolith_exp = ?, monolith_level = ? WHERE id = CAST(? AS UUID)";
                        try (var conn = dbManager.getConnection();
                             var ps = conn.prepareStatement(sql)) {
                            ps.setLong(1, clan.getMonolithExp());
                            ps.setInt(2, clan.getMonolithLevel());
                            ps.setString(3, clan.getId().toString());
                            ps.executeUpdate();
                        } catch (Exception ignored) {}
                    });
                });
            }

            default -> {
                crossplayUtils.sendMessage(player, "&#FFAA00[?] Comando desconocido. Usa /clan para abrir el panel.");
            }
        }
        return true;
    }

    // ==========================================================
    // 🧠 AUTOCOMPLETADO INTELIGENTE NATIVO
    // ==========================================================
    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return SUB_COMMANDS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        if (args.length == 2 && ("invite".equalsIgnoreCase(args[0]) || "kick".equalsIgnoreCase(args[0]))) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }

        return Collections.emptyList(); // Cero objetos basura
    }
}