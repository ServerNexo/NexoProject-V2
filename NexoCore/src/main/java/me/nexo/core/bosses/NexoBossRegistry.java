package me.nexo.core.bosses;

import com.google.inject.Singleton;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * 🏭 NexoBossRegistry - API Global para Jefes
 * Permite a cualquier módulo registrar e invocar jefes sin dependencias circulares.
 */
@Singleton
public final class NexoBossRegistry {

    // Diccionario de "Fábricas" de Jefes (ID -> Función creadora)
    private final Map<String, BiFunction<JavaPlugin, Location, NexoBoss>> bossFactories = new ConcurrentHashMap<>();

    /**
     * Registra un nuevo jefe en el ecosistema.
     * @param id El identificador único (ej: "EL_RENACIDO")
     * @param factory La función que sabe cómo construir al jefe
     */
    public void registerBoss(String id, BiFunction<JavaPlugin, Location, NexoBoss> factory) {
        bossFactories.put(id.toUpperCase(), factory);
    }

    /**
     * Invoca a un jefe previamente registrado.
     * @param id El identificador del jefe a invocar.
     * @param callerPlugin El plugin que lo está invocando (ej: NexoDungeons).
     * @param loc Ubicación de aparición.
     * @param gearScore Nivel de equipo del grupo para el auto-escalado.
     * @return La instancia de NexoBoss si existe, o null.
     */
    public NexoBoss spawnBoss(String id, JavaPlugin callerPlugin, Location loc, int gearScore) {
        var factory = bossFactories.get(id.toUpperCase());
        if (factory == null) return null;

        // Construimos al jefe usando la receta guardada
        NexoBoss boss = factory.apply(callerPlugin, loc);
        
        // Ejecutamos su rutina de aparición y escalado
        boss.spawn(loc, gearScore, id);
        
        return boss;
    }
}