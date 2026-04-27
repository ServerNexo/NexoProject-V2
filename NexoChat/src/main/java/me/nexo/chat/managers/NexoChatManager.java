package me.nexo.chat.managers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.event.player.AsyncChatEvent;
import me.nexo.chat.NexoChatPlugin;
import me.nexo.chat.utils.ChatPerms;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class NexoChatManager implements Listener {

    private final NexoChatPlugin plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    // 🐘 Bases de RAM (Cosméticos e Ignorados)
    private final Map<UUID, String> activeCosmetics = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> ignoredPlayers = new ConcurrentHashMap<>();

    // 🛡️ Memoria Anti-Spam y StaffChat
    private final Map<UUID, Long> lastMessageTime = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastMessageContent = new ConcurrentHashMap<>();
    private final Set<UUID> staffChatToggled = ConcurrentHashMap.newKeySet();

    // 🎁 Memoria del Sistema GG
    public boolean ggEventActive = false;
    public final Set<UUID> playersRewarded = ConcurrentHashMap.newKeySet();

    // 🔇 Memoria de Muteos
    private final Map<UUID, Long> muteEndTimes = new ConcurrentHashMap<>();
    private final Map<UUID, String> muteReasons = new ConcurrentHashMap<>();

    @Inject
    public NexoChatManager(NexoChatPlugin plugin) {
        this.plugin = plugin;
    }

    // ==========================================
    // 🔇 MÉTODOS DE MUTEO
    // ==========================================
    public void setMute(UUID uuid, long timeEnd, String reason) {
        muteEndTimes.put(uuid, timeEnd);
        muteReasons.put(uuid, reason);
    }

    public void removeMute(UUID uuid) {
        muteEndTimes.remove(uuid);
        muteReasons.remove(uuid);
    }

    public boolean isMuted(UUID uuid) {
        if (!muteEndTimes.containsKey(uuid)) return false;
        if (System.currentTimeMillis() > muteEndTimes.get(uuid)) {
            removeMute(uuid);
            return false;
        }
        return true;
    }

    public String getMuteReason(UUID uuid) { return muteReasons.getOrDefault(uuid, "No especificada"); }
    public long getMuteTimeLeft(UUID uuid) { return muteEndTimes.getOrDefault(uuid, 0L) - System.currentTimeMillis(); }

    public Map<UUID, Long> getMuteEndTimes() { return muteEndTimes; }
    public Map<UUID, String> getMuteReasons() { return muteReasons; }

    // ==========================================
    // 👮 MÉTODOS DE STAFF CHAT
    // ==========================================
    public boolean toggleStaffChat(Player player) {
        if (staffChatToggled.contains(player.getUniqueId())) {
            staffChatToggled.remove(player.getUniqueId());
            return false;
        }
        staffChatToggled.add(player.getUniqueId());
        return true;
    }

    public void sendStaffChatMessage(Player sender, String message) {
        Component scMsg = mm.deserialize("<dark_red><bold>[STAFF]</bold></dark_red> <red>" + sender.getName() + "</red> <dark_gray>»</dark_gray> <white>" + message + "</white>");
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission(ChatPerms.STAFF_CHAT)) {
                p.sendMessage(scMsg);
            }
        }
        Bukkit.getConsoleSender().sendMessage(scMsg);
    }

    // ==========================================
    // 🎨 MÉTODOS DE COSMÉTICOS Y BASES DE DATOS
    // ==========================================
    public void setPlayerCosmetic(UUID uuid, String tag) { activeCosmetics.put(uuid, tag); }
    public String getPlayerCosmetic(UUID uuid) { return activeCosmetics.getOrDefault(uuid, "<gray>"); }
    public Map<UUID, String> getCosmeticsMap() { return activeCosmetics; }
    public Map<UUID, Set<UUID>> getIgnoredPlayersMap() { return ignoredPlayers; }

    // ==========================================
    // 🛑 MÉTODOS DE IGNORAR
    // ==========================================
    public boolean toggleIgnore(Player player, Player target) {
        ignoredPlayers.putIfAbsent(player.getUniqueId(), new HashSet<>());
        Set<UUID> ignored = ignoredPlayers.get(player.getUniqueId());
        if (ignored.contains(target.getUniqueId())) {
            ignored.remove(target.getUniqueId());
            return false;
        }
        ignored.add(target.getUniqueId());
        return true;
    }

    public boolean isIgnoring(Player player, Player target) {
        return ignoredPlayers.getOrDefault(player.getUniqueId(), Collections.emptySet()).contains(target.getUniqueId());
    }

    // ==========================================
    // 🛠️ UTILIDAD: PARSEO DE COLORES LEGACY Y HEX
    // ==========================================
    public Component parseColors(String text) {
        text = text.replaceAll("&#([a-fA-F0-9]{6})", "<#$1>");
        text = text.replace("&0", "<black>").replace("&1", "<dark_blue>").replace("&2", "<dark_green>").replace("&3", "<dark_aqua>")
                .replace("&4", "<dark_red>").replace("&5", "<dark_purple>").replace("&6", "<gold>").replace("&7", "<gray>")
                .replace("&8", "<dark_gray>").replace("&9", "<blue>").replace("&a", "<green>").replace("&b", "<aqua>")
                .replace("&c", "<red>").replace("&d", "<light_purple>").replace("&e", "<yellow>").replace("&f", "<white>")
                .replace("&l", "<bold>").replace("&m", "<strikethrough>").replace("&n", "<underlined>").replace("&o", "<italic>").replace("&r", "<reset>");
        return mm.deserialize(text);
    }

    // ==========================================
    // 💬 EVENTO PRINCIPAL DE CHAT
    // ==========================================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String plainText = PlainTextComponentSerializer.plainText().serialize(event.message());

        // 🌟 0. REEMPLAZO DE EMOJIS CUSTOMIZADOS
        if (plugin.getConfig().contains("emojis")) {
            for (String key : plugin.getConfig().getConfigurationSection("emojis").getKeys(false)) {
                String replacement = plugin.getConfig().getString("emojis." + key);
                if (replacement != null) {
                    plainText = plainText.replace(key, replacement); // Convierte :feliz: a Unicode o Texto
                }
            }
        }

        // 🔇 1. DETECCIÓN DE MUTEO
        if (isMuted(player.getUniqueId())) {
            event.setCancelled(true);
            long timeLeft = getMuteTimeLeft(player.getUniqueId());
            String reason = getMuteReason(player.getUniqueId());

            String timeStr = timeLeft > 315360000000L ? "Permanente" : (timeLeft / 1000 / 60) + " minuto(s)";

            player.sendMessage(mm.deserialize(
                    "\n<red>🔇 <bold>ESTÁS SILENCIADO</bold></red>\n" +
                            "<gray>Razón: <white>" + reason + "</white>\n" +
                            "<gray>Tiempo restante: <white>" + timeStr + "</white>\n"
            ));
            return;
        }

        // 👮 2. DETECCIÓN DE STAFF CHAT TOGGLEADO
        if (staffChatToggled.contains(player.getUniqueId())) {
            event.setCancelled(true);
            sendStaffChatMessage(player, plainText);
            return;
        }

        // 🎁 3. DETECCIÓN DE EVENTO GG
        if (ggEventActive && plainText.equalsIgnoreCase("gg") && !playersRewarded.contains(player.getUniqueId())) {
            playersRewarded.add(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> {
                List<String> commands = plugin.getConfig().getStringList("eventos.tienda.recompensa_comandos");
                for (String cmd : commands) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", player.getName()));
                }
            });
            String ggMsg = plugin.getConfig().getString("eventos.tienda.mensaje_gg", "<aqua>%player%</aqua> <gray>ha dicho GG.</gray>");
            Bukkit.broadcast(parseColors(ggMsg.replace("%player%", player.getName())));
        }

        // 🛡️ 4. ANTI-SPAM Y COOLDOWN
        if (!player.hasPermission(ChatPerms.BYPASS_SPAM)) {
            long now = System.currentTimeMillis();
            long cooldown = (long) (plugin.getConfig().getDouble("chat.cooldown_segundos", 1.5) * 1000);

            if (lastMessageTime.containsKey(player.getUniqueId()) && (now - lastMessageTime.get(player.getUniqueId())) < cooldown) {
                player.sendMessage(mm.deserialize("<red>Estás escribiendo muy rápido. Espera un momento.</red>"));
                event.setCancelled(true);
                return;
            }
            if (plainText.equalsIgnoreCase(lastMessageContent.getOrDefault(player.getUniqueId(), ""))) {
                player.sendMessage(mm.deserialize("<red>No repitas el mismo mensaje.</red>"));
                event.setCancelled(true);
                return;
            }
            lastMessageTime.put(player.getUniqueId(), now);
            lastMessageContent.put(player.getUniqueId(), plainText);
        }

        // 🛑 5. FILTRO DE IGNORADOS
        event.viewers().removeIf(viewer -> viewer instanceof Player && isIgnoring((Player) viewer, player));

        // 🎨 6. PARSEO DE COLORES
        Component sanitizedMessage;
        if (player.hasPermission(ChatPerms.TAGS)) {
            sanitizedMessage = parseColors(plainText);
        } else {
            // Incluso si no tiene permisos VIP, parseamos el MiniMessage para que los emojis de color (como <red>❤</red>) funcionen
            sanitizedMessage = mm.deserialize(plainText);
        }

        // 🗡️ 7. PROCESAR ÍTEMS Y MENCIONES
        Component messageWithItems = processItemLinking(player, sanitizedMessage);
        Component finalMessage = processMentions(messageWithItems);

        // 🎨 8. RENDERIZADO FINAL (Con soporte para Rangos de LuckPerms a través de PlaceholderAPI)
        event.renderer((source, sourceDisplayName, message, viewer) -> {
            Component formattedIdentity = buildPlayerIdentity(source);

            // 🌟 Extraemos el prefijo del jugador usando PlaceholderAPI
            String rawPrefix = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(source, "%luckperms_prefix%");

            // 🌟 Parseamos los colores del prefijo usando nuestro propio motor (Soporta HEX y Legacy)
            Component prefixComponent = parseColors(rawPrefix);

            return Component.text()
                    .append(prefixComponent) // Rango (Admin, VIP, etc.)
                    .append(Component.text(" ")) // Espaciador entre el Rango y el Nombre
                    .append(formattedIdentity) // Nombre + Cosmético de Nexo
                    .append(Component.text(" » ", NamedTextColor.DARK_GRAY))
                    .append(finalMessage.colorIfAbsent(NamedTextColor.WHITE))
                    .build();
        });
    }

    private Component buildPlayerIdentity(Player player) {
        String rawName = player.getName();
        String cosmeticTag = getPlayerCosmetic(player.getUniqueId());

        boolean isLegendaryTag = cosmeticTag.equals("<#010000>") || cosmeticTag.equals("<#000100>") ||
                cosmeticTag.equals("<#000001>") || cosmeticTag.equals("<#010100>") || cosmeticTag.equals("<#010001>");

        if (isLegendaryTag && !player.hasPermission(ChatPerms.RGB_COLORS)) {
            setPlayerCosmetic(player.getUniqueId(), "<gray>");
            cosmeticTag = "<gray>";
        } else if (cosmeticTag.contains("<gradient") && !player.hasPermission(ChatPerms.HEX_COLORS)) {
            setPlayerCosmetic(player.getUniqueId(), "<gray>");
            cosmeticTag = "<gray>";
        }

        Component nameComponent = parseColors(cosmeticTag + rawName + (cosmeticTag.contains("<gradient") ? "</gradient>" : ""));

        Component hoverData = Component.text()
                .append(mm.deserialize("<gold>⚙ Perfil de " + rawName + "</gold>\n"))
                .append(mm.deserialize("<gray>Salud: </gray><red>" + (int)player.getHealth() + "❤</red>\n"))
                .append(mm.deserialize("<gray>Ping: </gray><green>" + player.getPing() + "ms</green>\n\n"))
                .append(mm.deserialize("<yellow>▶ Click para enviar mensaje privado</yellow>"))
                .build();

        return nameComponent.hoverEvent(HoverEvent.showText(hoverData)).clickEvent(ClickEvent.suggestCommand("/mensaje " + rawName + " "));
    }

    // 🎒 DETECCIÓN DE ÍTEMS, ENDERCHEST E INVENTARIO
    private Component processItemLinking(Player player, Component message) {
        String plainText = PlainTextComponentSerializer.plainText().serialize(message);

        // 1. 🗡️ Detectar Ítem en Mano (#item, #i, #mano)
        java.util.regex.Pattern itemPattern = java.util.regex.Pattern.compile("#(item|i|mano)\\b", java.util.regex.Pattern.CASE_INSENSITIVE);
        if (itemPattern.matcher(plainText).find()) {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand.isEmpty()) {
                message = message.replaceText(TextReplacementConfig.builder().match(itemPattern).replacement(Component.text("[Mano Vacía]", NamedTextColor.GRAY)).build());
            } else {
                Component itemName = hand.getItemMeta().hasDisplayName() ? hand.getItemMeta().displayName() : Component.translatable(hand.translationKey());
                Component itemComp = Component.text("[").append(itemName.colorIfAbsent(NamedTextColor.AQUA)).append(Component.text("]")).color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD).hoverEvent(hand.asHoverEvent());
                message = message.replaceText(TextReplacementConfig.builder().match(itemPattern).replacement(itemComp).build());
            }
        }

        // 2. 🎒 Detectar Inventario y EnderChest (#ec, #inv, #inventario)
        java.util.regex.Pattern invPattern = java.util.regex.Pattern.compile("#(ec|inv|inventario)\\b", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = invPattern.matcher(plainText);

        if (matcher.find()) {
            String type = matcher.group(1).toLowerCase();
            String displayType = type.equals("ec") ? "EnderChest" : "Inventario";

            Component invComp = Component.text("[\uD83D\uDD0D Ver " + displayType + " de " + player.getName() + "]")
                    .color(NamedTextColor.LIGHT_PURPLE)
                    .decorate(TextDecoration.BOLD)
                    .hoverEvent(HoverEvent.showText(mm.deserialize("<green>¡Click para abrir el " + displayType + "!</green>")))
                    .clickEvent(ClickEvent.runCommand("/nexo_inv " + player.getName() + " " + type));

            message = message.replaceText(TextReplacementConfig.builder().match(invPattern).replacement(invComp).build());
        }

        return message;
    }

    private Component processMentions(Component message) {
        String plainText = PlainTextComponentSerializer.plainText().serialize(message);
        if (!plainText.contains("@")) return message;

        Component processed = message;
        for (Player online : Bukkit.getOnlinePlayers()) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("@" + online.getName(), java.util.regex.Pattern.CASE_INSENSITIVE);
            if (pattern.matcher(plainText).find()) {
                processed = processed.replaceText(TextReplacementConfig.builder().match(pattern).replacement(Component.text("@" + online.getName(), NamedTextColor.YELLOW)).build());
                online.playSound(Sound.sound(Key.key("block.note_block.bell"), Sound.Source.MASTER, 1f, 1.2f));
            }
        }
        return processed;
    }
}