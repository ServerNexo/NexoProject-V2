package me.aeroxis.dungeons.api;

import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 🏰 AeroxisDungeons - Contrato Base para Instancias de Mazmorras
 * Maneja el ciclo de vida completo de un mundo efímero Slime en RAM.
 */
public interface IDungeonController {

    /**
     * @return El identificador único de esta instancia (UUID del escuadrón/partida).
     */
    UUID getInstanceId();

    /**
     * @return El mundo Slime cargado en RAM asignado a esta mazmorra.
     */
    World getSlimeWorld();

    /**
     * Inicializa la mazmorra. (Spawneo de NPCs iniciales, reset de puzzles, etc).
     */
    void initialize();

    /**
     * Comienza la partida (Abre barreras, inicia el timer o la primera oleada).
     */
    void start();

    /**
     * Lógica cuando un jugador muere dentro de la instancia.
     */
    void handlePlayerDeath(Player player);

    /**
     * Lógica cuando un jugador se desconecta (Manejo de reconexión o abandono).
     */
    void handlePlayerQuit(Player player);

    // =========================================
    // 🌟 NUEVOS MÉTODOS PARA EL MODO SUMMON/PUZZLES
    // =========================================

    /**
     * Enruta las interacciones del jugador (Ej: Clic en el Altar de Invocación).
     */
    void handleInteract(org.bukkit.event.player.PlayerInteractEvent event);

    /**
     * Enruta la muerte de un mob dentro de la instancia (Para dropear fragmentos o llaves).
     */
    void handleMobDeath(org.bukkit.event.entity.EntityDeathEvent event);

    // =========================================

    /**
     * Finaliza la mazmorra de forma asíncrona.
     * @param success true si ganaron, false si fracasaron (wipe).
     * @return Un future que se completa cuando se repartió el loot y se guardaron stats en Supabase.
     */
    CompletableFuture<Void> endDungeon(boolean success);

    /**
     * Limpieza crítica: Teletransporta a los jugadores restantes al Hub y le ordena 
     * al SlimeManager que destruya el mundo de la RAM.
     */
    void destroyInstance();
}