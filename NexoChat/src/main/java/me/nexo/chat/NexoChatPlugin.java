package me.nexo.chat;

import com.google.inject.Injector;
import me.nexo.chat.di.ChatModule;
import me.nexo.chat.managers.NexoAnnouncementManager;
import me.nexo.chat.managers.NexoChatManager;
import me.nexo.chat.managers.NexoConnectionListener;
import me.nexo.chat.managers.NexoDeathListener;
import me.nexo.chat.managers.NexoLoginListener;
import me.nexo.chat.managers.NexoPrivateMessageManager;
import me.nexo.chat.menu.NexoViewerMenu;
import me.nexo.chat.utils.ChatPerms;
import me.nexo.core.NexoCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.bukkit.BukkitCommandHandler; // 🌟 IMPORTACIÓN LAMP

import java.util.Arrays;

/**
 * 💬 NexoChat - Módulo de Comunicación y Cosméticos (Arquitectura AAA)
 */
public class NexoChatPlugin extends JavaPlugin {

    private Injector childInjector;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private BukkitCommandHandler commandHandler; // 🌟 GESTOR LAMP

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("💬 Iniciando NexoChat...");

        saveDefaultConfig();

        var core = (NexoCore) getServer().getPluginManager().getPlugin("NexoCore");
        if (core == null) {
            getLogger().severe("❌ Error crítico: Falta NexoCore.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.childInjector = core.getInjector().createChildInjector(new ChatModule(this));

        // 🌟 INICIALIZAR BASE DE DATOS
        var database = childInjector.getInstance(me.nexo.chat.database.NexoChatDatabase.class);
        database.createTables();

        // 🌟 REGISTRO DE MANAGERS Y LISTENERS
        var chatManager = childInjector.getInstance(NexoChatManager.class);
        var announcementManager = childInjector.getInstance(NexoAnnouncementManager.class);
        var connectionListener = childInjector.getInstance(NexoConnectionListener.class);
        var pmManager = childInjector.getInstance(NexoPrivateMessageManager.class);
        var deathListener = childInjector.getInstance(NexoDeathListener.class);
        var loginListener = childInjector.getInstance(NexoLoginListener.class);

        getServer().getPluginManager().registerEvents(chatManager, this);
        getServer().getPluginManager().registerEvents(announcementManager, this);
        getServer().getPluginManager().registerEvents(connectionListener, this);
        getServer().getPluginManager().registerEvents(deathListener, this);
        getServer().getPluginManager().registerEvents(loginListener, this);

        announcementManager.startTasks();

        // 🌟 REGISTRO DE PLACEHOLDER API
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new me.nexo.chat.utils.NexoChatExpansion(chatManager).register();
            getLogger().info("✅ [NexoChat] PlaceholderAPI detectado. ¡Expansion %nexochat_color% registrada!");
        } else {
            getLogger().warning("⚠️ [NexoChat] PlaceholderAPI NO ENCONTRADO. Los prefijos y TAB no funcionarán correctamente.");
        }

        // ==========================================
        // 🌟 REGISTRO DE COMANDOS LAMP (Nicks y Tags)
        // ==========================================
        this.commandHandler = BukkitCommandHandler.create(this);
        this.commandHandler.register(childInjector.getInstance(me.nexo.chat.commands.ComandoChat.class));
        // 🎥 NUEVO: Registro del comando de Streamers
        this.commandHandler.register(childInjector.getInstance(me.nexo.chat.commands.ComandoStream.class));

        // ==========================================
        // 🎮 REGISTRO DE COMANDOS NATIVOS BUKKIT
        // ==========================================

        // 1. /mensaje <jugador> <msg>
        Command msgCommand = new Command("mensaje") {
            @Override
            public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
                if (!(sender instanceof Player player)) return false;
                if (args.length < 2) {
                    player.sendMessage(mm.deserialize("<red>Uso: /mensaje <jugador> <mensaje></red>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    player.sendMessage(mm.deserialize("<red>Ese jugador no está conectado.</red>"));
                    return true;
                }
                String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                pmManager.sendMessage(player, target, message);
                return true;
            }
        };
        msgCommand.setAliases(Arrays.asList("privado", "m"));
        getServer().getCommandMap().register("nexochat", msgCommand);

        // 2. /responder <msg>
        Command replyCommand = new Command("responder") {
            @Override
            public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
                if (!(sender instanceof Player player)) return false;
                if (args.length < 1) {
                    player.sendMessage(mm.deserialize("<red>Uso: /responder <mensaje></red>"));
                    return true;
                }
                String message = String.join(" ", args);
                pmManager.replyMessage(player, message);
                return true;
            }
        };
        replyCommand.setAliases(Arrays.asList("r"));
        getServer().getCommandMap().register("nexochat", replyCommand);

        // 3. /ignorar <jugador>
        getServer().getCommandMap().register("nexochat", new Command("ignorar") {
            @Override
            public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
                if (!(sender instanceof Player player)) return false;
                if (args.length < 1) {
                    player.sendMessage(mm.deserialize("<red>Uso: /ignorar <jugador></red>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null || target.equals(player)) {
                    player.sendMessage(mm.deserialize("<red>Jugador inválido o no conectado.</red>"));
                    return true;
                }
                boolean ignored = chatManager.toggleIgnore(player, target);
                if (ignored) {
                    player.sendMessage(mm.deserialize("<green>Has silenciado a " + target.getName() + ".</green>"));
                } else {
                    player.sendMessage(mm.deserialize("<yellow>Has dejado de ignorar a " + target.getName() + ".</yellow>"));
                }
                return true;
            }
        });

        // 4. /anuncio <mensaje>
        getServer().getCommandMap().register("nexochat", new Command("anuncio") {
            @Override
            public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
                if (!sender.hasPermission(ChatPerms.ADMIN)) {
                    sender.sendMessage(mm.deserialize("<red>No tienes permisos.</red>"));
                    return true;
                }
                if (args.length == 0) {
                    sender.sendMessage(mm.deserialize("<red>Uso: /anuncio <mensaje></red>"));
                    return true;
                }
                String msg = String.join(" ", args);
                Bukkit.broadcast(chatManager.parseColors("\n<dark_red><bold>📢 ANUNCIO »</bold></dark_red> <white>" + msg + "</white>\n"));
                Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f));
                return true;
            }
        });

        // 5. /limpiarchat [jugador]
        getServer().getCommandMap().register("nexochat", new Command("limpiarchat") {
            @Override
            public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
                if (!sender.hasPermission(ChatPerms.ADMIN)) {
                    sender.sendMessage(mm.deserialize("<red>No tienes permisos.</red>"));
                    return true;
                }
                String blankMessage = " \n".repeat(100);
                if (args.length == 1) {
                    Player target = Bukkit.getPlayer(args[0]);
                    if (target != null) {
                        target.sendMessage(blankMessage);
                        target.sendMessage(mm.deserialize("<green>Tu chat ha sido limpiado por un administrador.</green>"));
                        sender.sendMessage(mm.deserialize("<green>Chat de " + target.getName() + " limpiado.</green>"));
                    } else {
                        sender.sendMessage(mm.deserialize("<red>Jugador no encontrado.</red>"));
                    }
                } else {
                    Bukkit.broadcast(Component.text(blankMessage));
                    Bukkit.broadcast(mm.deserialize("<green>✨ El chat global ha sido limpiado por un administrador.</green>"));
                }
                return true;
            }
        });

        // 6. /socialspy
        getServer().getCommandMap().register("nexochat", new Command("socialspy") {
            @Override
            public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
                if (!(sender instanceof Player player)) return false;
                if (!player.hasPermission(ChatPerms.ADMIN)) {
                    player.sendMessage(mm.deserialize("<red>No tienes permisos.</red>"));
                    return true;
                }

                boolean isSpying = pmManager.toggleSocialSpy(player);
                if (isSpying) {
                    player.sendMessage(mm.deserialize("<green>🕵️‍♂️ Modo SocialSpy ACTIVADO.</green>"));
                } else {
                    player.sendMessage(mm.deserialize("<red>🕵️‍♂️ Modo SocialSpy DESACTIVADO.</red>"));
                }
                return true;
            }
        });

        // 7. /nexotienda
        getServer().getCommandMap().register("nexochat", new Command("nexotienda") {
            @Override
            public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
                if (!sender.hasPermission(ChatPerms.ADMIN)) return true;
                if (args.length < 2) {
                    sender.sendMessage("Uso: /nexotienda <jugador> <Paquete>");
                    return true;
                }

                String targetPlayer = args[0];
                String packageName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

                String msg = getConfig().getString("eventos.tienda.anuncio", "").replace("%player%", targetPlayer).replace("%paquete%", packageName);
                Bukkit.broadcast(chatManager.parseColors(msg));
                Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f));

                chatManager.ggEventActive = true;
                chatManager.playersRewarded.clear();
                int duracion = getConfig().getInt("eventos.tienda.duracion_gg_segundos", 30);

                Bukkit.getAsyncScheduler().runDelayed(NexoChatPlugin.this, task -> {
                    chatManager.ggEventActive = false;
                    Bukkit.broadcast(mm.deserialize("<gray><i>El evento GG ha terminado.</i></gray>"));
                }, duracion, java.util.concurrent.TimeUnit.SECONDS);

                return true;
            }
        });

        // 8. /nexochat recargar
        getServer().getCommandMap().register("nexochat", new Command("nexochat") {
            @Override
            public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
                if (!sender.hasPermission(ChatPerms.ADMIN)) return true;
                if (args.length == 1 && args[0].equalsIgnoreCase("recargar")) {
                    reloadConfig();
                    announcementManager.startTasks();
                    sender.sendMessage(mm.deserialize("<green>✅ Configuración recargada.</green>"));
                    return true;
                }
                sender.sendMessage(mm.deserialize("<yellow>Uso: /nexochat recargar</yellow>"));
                return true;
            }
        });

        // 9. /staffchat o /sc
        Command scCommand = new Command("staffchat") {
            @Override
            public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
                if (!sender.hasPermission(ChatPerms.STAFF_CHAT)) {
                    sender.sendMessage(mm.deserialize("<red>No tienes permisos.</red>"));
                    return true;
                }
                if (!(sender instanceof Player player)) return true;

                if (args.length == 0) {
                    if (chatManager.toggleStaffChat(player)) {
                        player.sendMessage(mm.deserialize("<green>🔐 Has ENTRADO al chat de Staff.</green>"));
                    } else {
                        player.sendMessage(mm.deserialize("<red>🔓 Has SALIDO del chat de Staff.</red>"));
                    }
                    return true;
                }
                chatManager.sendStaffChatMessage(player, String.join(" ", args));
                return true;
            }
        };
        scCommand.setAliases(Arrays.asList("sc"));
        getServer().getCommandMap().register("nexochat", scCommand);

        // 🌟 10. COMANDO OCULTO: /nexo_inv (Visualizador de #ec y #inv)
        getServer().getCommandMap().register("nexochat", new Command("nexo_inv") {
            @Override
            public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
                if (!(sender instanceof Player viewer)) return false;
                if (args.length < 2) return true; // Comando interno

                String targetName = args[0];
                String type = args[1]; // "ec" o "inv"

                Player target = Bukkit.getPlayerExact(targetName);
                if (target == null || !target.isOnline()) {
                    viewer.sendMessage(mm.deserialize("<red>El jugador " + targetName + " ya no está conectado.</red>"));
                    return true;
                }

                new NexoViewerMenu().open(viewer, target, type);
                return true;
            }
        });

        // 🌟 11. /mute <jugador> [razón] (Permanente)
        getServer().getCommandMap().register("nexochat", new Command("mute") {
            @Override
            public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
                if (!sender.hasPermission(ChatPerms.MUTE)) {
                    sender.sendMessage(mm.deserialize("<red>No tienes permisos.</red>"));
                    return true;
                }
                if (args.length < 1) {
                    sender.sendMessage(mm.deserialize("<red>Uso: /mute <jugador> [razón]</red>"));
                    return true;
                }

                String targetName = args[0];
                String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "Comportamiento Inadecuado";
                long timeEnd = System.currentTimeMillis() + 3153600000000L; // +100 Años (Permanente)

                Player target = Bukkit.getPlayerExact(targetName);
                if (target != null) {
                    chatManager.setMute(target.getUniqueId(), timeEnd, reason);
                    target.sendMessage(mm.deserialize("<red>🔇 Has sido SILENCIADO PERMANENTEMENTE.</red>\n<gray>Razón:</gray> <white>" + reason + "</white>"));
                } else {
                    @SuppressWarnings("deprecation")
                    org.bukkit.OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
                    database.muteOfflinePlayer(offlineTarget.getUniqueId(), timeEnd, reason);
                }

                Bukkit.broadcast(mm.deserialize("<dark_red>[Nexo Moderación]</dark_red> <red>" + targetName + " ha sido silenciado de forma permanente.</red>"), ChatPerms.ADMIN);
                sender.sendMessage(mm.deserialize("<green>Has muteado a " + targetName + ".</green>"));
                return true;
            }
        });

        // 🌟 12. /tempmute <jugador> <minutos> [razón]
        getServer().getCommandMap().register("nexochat", new Command("tempmute") {
            @Override
            public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
                if (!sender.hasPermission(ChatPerms.MUTE)) return true;
                if (args.length < 2) {
                    sender.sendMessage(mm.deserialize("<red>Uso: /tempmute <jugador> <minutos> [razón]</red>"));
                    return true;
                }

                String targetName = args[0];
                long minutes;
                try {
                    minutes = Long.parseLong(args[1]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(mm.deserialize("<red>El tiempo debe ser un número válido en minutos.</red>"));
                    return true;
                }

                String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "Romper las reglas de chat";
                long timeEnd = System.currentTimeMillis() + (minutes * 60 * 1000);

                Player target = Bukkit.getPlayerExact(targetName);
                if (target != null) {
                    chatManager.setMute(target.getUniqueId(), timeEnd, reason);
                    target.sendMessage(mm.deserialize("<red>🔇 Has sido SILENCIADO TEMPORALMENTE.</red>\n<gray>Tiempo:</gray> <white>" + minutes + " minutos</white>\n<gray>Razón:</gray> <white>" + reason + "</white>"));
                } else {
                    @SuppressWarnings("deprecation")
                    org.bukkit.OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
                    database.muteOfflinePlayer(offlineTarget.getUniqueId(), timeEnd, reason);
                }

                Bukkit.broadcast(mm.deserialize("<dark_red>[Nexo Moderación]</dark_red> <red>" + targetName + " ha sido silenciado por " + minutes + " minuto(s).</red>"), ChatPerms.ADMIN);
                return true;
            }
        });

        // 🌟 13. /unmute <jugador>
        getServer().getCommandMap().register("nexochat", new Command("unmute") {
            @Override
            public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
                if (!sender.hasPermission(ChatPerms.MUTE)) return true;
                if (args.length < 1) {
                    sender.sendMessage(mm.deserialize("<red>Uso: /unmute <jugador></red>"));
                    return true;
                }

                String targetName = args[0];
                Player target = Bukkit.getPlayerExact(targetName);

                if (target != null) {
                    chatManager.removeMute(target.getUniqueId());
                    target.sendMessage(mm.deserialize("<green>🔊 Ya no estás silenciado. Puedes hablar en el chat.</green>"));
                } else {
                    @SuppressWarnings("deprecation")
                    org.bukkit.OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
                    database.muteOfflinePlayer(offlineTarget.getUniqueId(), 0, ""); // Borramos tiempo en DB
                }

                sender.sendMessage(mm.deserialize("<green>Has desmuteado a " + targetName + ".</green>"));
                return true;
            }
        });

        // 🌟 14. /mantenimiento (Activar/Desactivar)
        getServer().getCommandMap().register("nexochat", new Command("mantenimiento") {
            @Override
            public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
                if (!sender.hasPermission(ChatPerms.ADMIN)) {
                    sender.sendMessage(mm.deserialize("<red>No tienes permisos.</red>"));
                    return true;
                }

                boolean currentState = getConfig().getBoolean("pantallas_sistema.mantenimiento_activo", false);
                boolean newState = !currentState;

                getConfig().set("pantallas_sistema.mantenimiento_activo", newState);
                saveConfig();

                if (newState) {
                    sender.sendMessage(mm.deserialize("<green>🛠️ Modo Mantenimiento ACTIVADO. Los jugadores no podrán entrar.</green>"));
                    Bukkit.broadcast(mm.deserialize("<red><bold>ATENCIÓN:</bold> El servidor entrará en mantenimiento en breve.</red>"));
                } else {
                    sender.sendMessage(mm.deserialize("<red>🛠️ Modo Mantenimiento DESACTIVADO. Servidor abierto al público.</red>"));
                }
                return true;
            }
        });

        getLogger().info("✅ ¡NexoChat 100% en línea!");
        getLogger().info("========================================");
    }

    public Injector getInjector() {
        return childInjector;
    }
}