package me.aeroxis.dungeons.engine;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import me.aeroxis.dungeons.api.IDungeonController;
import me.aeroxis.dungeons.modes.PuzzleDungeon;
import me.aeroxis.dungeons.modes.SummonDungeon;
import me.aeroxis.dungeons.modes.WaveDungeon;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🏭 Fabrica de Mazmorras Enterprise
 * Orquesta la creación de los controladores en base al tipo de partida
 * y mantiene un registro en RAM de las mazmorras activas.
 */
@Singleton
public class AeroxisDungeonFactory {

    private final Injector injector;

    // 🌟 MAPA DE RUTEO: Asocia el nombre del mundo clonado (Slime) con su Controlador respectivo
    private final Map<String, IDungeonController> activeDungeons = new ConcurrentHashMap<>();

    @Inject
    public AeroxisDungeonFactory(Injector injector) {
        this.injector = injector;
    }

    /**
     * Crea un controlador de mazmorra basado en el tipo solicitado y lo registra.
     */
    public IDungeonController createDungeon(String mode, UUID instanceId, World slimeWorld, List<Player> party) {

        IDungeonController dungeon = switch (mode.toUpperCase()) {
            case "PUZZLE" -> {
                PuzzleDungeon pDungeon = injector.getInstance(PuzzleDungeon.class);
                pDungeon.setup(instanceId, slimeWorld, party);
                yield pDungeon;
            }
            case "SUMMON" -> {
                SummonDungeon sDungeon = injector.getInstance(SummonDungeon.class);
                // 🌟 FIX: Proveemos la ubicación predeterminada del altar en la mazmorra real
                Location altarLoc = new Location(slimeWorld, 0, 64, 0);
                sDungeon.setup(instanceId, slimeWorld, party, altarLoc);
                yield sDungeon;
            }
            case "WAVE" -> {
                WaveDungeon wDungeon = injector.getInstance(WaveDungeon.class);
                wDungeon.setup(instanceId, slimeWorld, party);
                yield wDungeon;
            }
            default -> throw new IllegalArgumentException("Modo de mazmorra desconocido: " + mode);
        };

        // 🌟 REGISTRO ACTIVO: Guardamos la mazmorra usando el nombre del mundo generado
        activeDungeons.put(slimeWorld.getName(), dungeon);

        return dungeon;
    }

    /**
     * 🔍 Devuelve el controlador de la mazmorra asociada a este mundo (si existe).
     * Usado por el DungeonListener para enrutar muertes y desconexiones.
     */
    public IDungeonController getActiveDungeon(World world) {
        if (world == null) return null;
        return activeDungeons.get(world.getName());
    }

    /**
     * 🧹 Limpia la referencia en memoria cuando la mazmorra es destruida.
     * Evita Memory Leaks.
     */
    public void removeActiveDungeon(World world) {
        if (world != null) {
            activeDungeons.remove(world.getName());
        }
    }
}