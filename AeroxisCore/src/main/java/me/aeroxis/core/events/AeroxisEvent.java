package me.aeroxis.core.events;

import org.bukkit.entity.Player;
import java.util.List;

/**
 * 📅 Interfaz Base para todos los Eventos Globales (Dungeons, Bosses, Minijuegos)
 */
public interface AeroxisEvent {
    
    String getId();           // Ej: "invasion_zombie"
    String getNombre();       // Ej: "&#FF3366🩸 Luna de Sangre"
    String getTipo();         // Ej: "BOSS_SPAWN", "DUNGEON_WAVE", "EMF_FISHING"
    
    boolean cumpleRequisitos(int minJugadores); // ¿Se puede iniciar ahora?
    
    void iniciarEvento();     // La lógica de arranque (Spawnear al boss, oscurecer el cielo)
    void finalizarEvento();   // Lógica de limpieza
    
    boolean estaActivo();
    List<Player> getParticipantes();
}