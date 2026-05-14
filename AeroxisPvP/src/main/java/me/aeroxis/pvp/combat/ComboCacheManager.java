package me.aeroxis.pvp.combat;

import com.google.inject.Singleton;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ⚔️ AeroxisPvP - Motor de Caché de Combos
 * Registra los patrones de clicks (L/R) de los jugadores y detecta secuencias.
 */
@Singleton
public class ComboCacheManager {

    // ⏱️ Tiempo máximo en milisegundos entre clicks para que no se corte el combo
    private static final long COMBO_TIMEOUT = 1500L; 
    private static final int MAX_COMBO_LENGTH = 3;

    public enum ClickType {
        LEFT,  // Ataque Ligero
        RIGHT  // Ataque Pesado
    }

    private static class ComboState {
        List<ClickType> sequence = new ArrayList<>();
        long lastClickTime = 0;
    }

    // ⚡ Memoria Concurrente O(1)
    private final Map<UUID, ComboState> playerCombos = new ConcurrentHashMap<>();

    /**
     * Registra un nuevo click del jugador y devuelve la secuencia actual.
     */
    public List<ClickType> registerClick(Player player, ClickType click) {
        long now = System.currentTimeMillis();
        ComboState state = playerCombos.computeIfAbsent(player.getUniqueId(), k -> new ComboState());

        // Si pasó mucho tiempo desde el último click, reiniciamos el combo
        if (now - state.lastClickTime > COMBO_TIMEOUT) {
            state.sequence.clear();
        }

        state.sequence.add(click);
        state.lastClickTime = now;

        // Si llegamos al máximo (Ej: L-L-R), cortamos el primer click para que la rueda fluya
        if (state.sequence.size() > MAX_COMBO_LENGTH) {
            state.sequence.remove(0); // Elimina el más viejo
        }

        return new ArrayList<>(state.sequence); // Retornamos copia segura
    }

    /**
     * Limpia el combo actual (Útil cuando un jugador ejecuta la habilidad con éxito).
     */
    public void clearCombo(Player player) {
        ComboState state = playerCombos.get(player.getUniqueId());
        if (state != null) {
            state.sequence.clear();
        }
    }

    /**
     * Limpia la memoria cuando el jugador se desconecta (Anti Memory Leak).
     */
    public void removePlayer(Player player) {
        playerCombos.remove(player.getUniqueId());
    }
}