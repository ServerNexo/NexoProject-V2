package me.nexo.chat.managers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class NexoPrivateMessageManager {

    private final MiniMessage mm = MiniMessage.miniMessage();
    // 🧠 Memoria: Guarda <TuUUID, UUID de la persona con la que hablas>
    private final Map<UUID, UUID> replyMap = new ConcurrentHashMap<>();

    // 🕵️‍♂️ Memoria Spy: Guarda quién tiene el SocialSpy activado
    private final Set<UUID> socialSpyUsers = ConcurrentHashMap.newKeySet();

    // 🌟 Inyectamos el ChatManager para acceder a la lista de ignorados
    private final NexoChatManager chatManager;

    @Inject
    public NexoPrivateMessageManager(NexoChatManager chatManager) {
        this.chatManager = chatManager;
    }

    /**
     * 🕵️‍♂️ Alternar el modo SocialSpy
     */
    public boolean toggleSocialSpy(Player player) {
        if (socialSpyUsers.contains(player.getUniqueId())) {
            socialSpyUsers.remove(player.getUniqueId());
            return false; // Se desactivó
        } else {
            socialSpyUsers.add(player.getUniqueId());
            return true; // Se activó
        }
    }

    /**
     * ✉️ Enviar un Mensaje Privado
     */
    public void sendMessage(Player sender, Player target, String message) {
        if (sender.getUniqueId().equals(target.getUniqueId())) {
            sender.sendMessage(mm.deserialize("<red>No puedes enviarte mensajes a ti mismo.</red>"));
            return;
        }

        // 🛑 VALIDACIÓN DE IGNORADOS: Si el objetivo me ignoró, le bloqueamos el mensaje
        if (chatManager.isIgnoring(target, sender)) {
            sender.sendMessage(mm.deserialize("<red>No puedes enviar mensajes a este jugador (Te ha ignorado).</red>"));
            return;
        }

        // Guardamos la sesión para el /responder mutuo
        replyMap.put(sender.getUniqueId(), target.getUniqueId());
        replyMap.put(target.getUniqueId(), sender.getUniqueId());

        // Formato AAA en español
        sender.sendMessage(mm.deserialize("<gold>[Tú -> " + target.getName() + "]</gold> <white>" + message + "</white>"));
        target.sendMessage(mm.deserialize("<gold>[" + sender.getName() + " -> Tú]</gold> <white>" + message + "</white>"));

        // Sonido de notificación
        target.playSound(Sound.sound(Key.key("block.note_block.pling"), Sound.Source.MASTER, 1f, 2f));

        // 🕵️‍♂️ LÓGICA SOCIALSPY: Avisar a los admins que lo tienen encendido
        String spyMsg = "<dark_gray>[Spy] " + sender.getName() + " -> " + target.getName() + ": " + message + "</dark_gray>";
        for (UUID spyId : socialSpyUsers) {
            Player spy = Bukkit.getPlayer(spyId);
            // Evitamos mandarle el mensaje espía al admin si él es quien está enviando o recibiendo el mensaje original
            if (spy != null && !spy.equals(sender) && !spy.equals(target)) {
                spy.sendMessage(mm.deserialize(spyMsg));
            }
        }
    }

    /**
     * ↩️ Responder al último Mensaje
     */
    public void replyMessage(Player sender, String message) {
        UUID targetId = replyMap.get(sender.getUniqueId());

        if (targetId == null) {
            sender.sendMessage(mm.deserialize("<red>No tienes a nadie a quien responder.</red>"));
            return;
        }

        Player target = Bukkit.getPlayer(targetId);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(mm.deserialize("<red>Ese jugador ya no está conectado.</red>"));
            replyMap.remove(sender.getUniqueId());
            return;
        }

        sendMessage(sender, target, message);
    }
}