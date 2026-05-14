package me.aeroxis.core.utils;

import com.google.inject.Singleton;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.entity.Player;

/**
 * 🔊 Nexo Network - Gestor Auditivo Inmersivo (AAA UX)
 */
@Singleton
public class SoundManager {

    public void playMenuOpen(Player player) {
        playSound(player, "block.chest.open", 0.8f, 1.2f);
    }

    public void playError(Player player) {
        playSound(player, "entity.villager.no", 1.0f, 1.0f);
    }

    public void playClick(Player player) {
        playSound(player, "ui.button.click", 0.5f, 1.2f);
    }

    // 🥉 COSMÉTICO BÁSICO (Sólido)
    public void playSolidEquip(Player player) {
        playSound(player, "item.armor.equip_leather", 1.0f, 1.0f);
        playSound(player, "entity.experience_orb.pickup", 0.8f, 1.0f);
    }

    // 🥈 COSMÉTICO ÉPICO (Gradiente)
    public void playGradientEquip(Player player) {
        playSound(player, "item.armor.equip_diamond", 1.0f, 1.2f);
        playSound(player, "entity.player.levelup", 0.7f, 1.5f);
    }

    // 🥇 COSMÉTICO LEGENDARIO (Shader / Animado)
    public void playShaderEquip(Player player) {
        playSound(player, "block.beacon.activate", 0.8f, 2.0f); // Sonido resonante
        playSound(player, "entity.wither.spawn", 0.2f, 2.0f); // Efecto sutil "espacial"
    }

    // 🧱 RESET
    public void playReset(Player player) {
        playSound(player, "block.grindstone.use", 1.0f, 1.5f);
    }

    private void playSound(Player player, String key, float volume, float pitch) {
        player.playSound(Sound.sound(Key.key(key), Sound.Source.MASTER, volume, pitch));
    }
}