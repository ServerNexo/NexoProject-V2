package me.nexo.factories.logic;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Singleton;
import me.nexo.factories.core.ActiveFactory;
import me.nexo.protections.core.ProtectionStone;

/**
 * 🏭 NexoFactories - Evaluador Lógico (Arquitectura Enterprise)
 * Rendimiento: Compilación AOT (Ahead-Of-Time) y Caché de Lambdas O(1).
 * Cero parseo de JSON o Strings durante el Tick Loop.
 */
@Singleton
public class ScriptEvaluator {

    // 🌟 MAGIA ENTERPRISE: Caché de Scripts "Compilados".
    // En lugar de leer el JSON cada segundo, guardamos la instrucción matemática pura.
    private final Cache<String, CompiledScript> scriptCache = Caffeine.newBuilder()
            .maximumSize(5000) // Soporta hasta 5000 scripts únicos en memoria
            .build();

    /**
     * Evalúa si una máquina DEBE encenderse o apagarse basándose en su Script.
     * Complejidad: O(1) puro.
     */
    public boolean shouldRun(ActiveFactory factory, ProtectionStone stone, String jsonScript) {
        // Si no hay script, opera normalmente
        if (jsonScript == null || jsonScript.isEmpty() || jsonScript.equals("NONE")) {
            return true;
        }

        // 🚀 Obtenemos el script compilado O(1). Si no existe, lo compila (solo la primera vez)
        CompiledScript compiled = scriptCache.get(jsonScript, this::compileScript);

        // Si la compilación falló por un script corrupto, apagamos por seguridad
        if (compiled == null) return false;

        // Ejecutamos la matemática pura (Cero lag)
        return compiled.evaluate(factory, stone);
    }

    /**
     * 🧠 COMPILADOR INTERNO: Convierte texto pesado en lambdas de alto rendimiento.
     * Solo se ejecuta UNA VEZ por cada script nuevo que se inserta en una máquina.
     */
    private CompiledScript compileScript(String rawJson) {
        try {
            JsonObject logic = JsonParser.parseString(rawJson).getAsJsonObject();

            if (!logic.has("condition")) {
                return (f, s) -> true; // Lambda por defecto (Siempre encendido)
            }

            String condition = logic.get("condition").getAsString();

            // REGLA 1: Prioridad de Escudo
            if (condition.startsWith("ENERGY_>_")) {
                double requiredEnergy = Double.parseDouble(condition.split("_>_")[1]);

                // Retornamos una función lambda que solo hace una comparación matemática < >
                return (f, s) -> s != null && s.getCurrentEnergy() > requiredEnergy;
            }

            // REGLA 2: Límite de Almacenamiento
            if (condition.startsWith("STORAGE_<_")) {
                int maxStorage = Integer.parseInt(condition.split("_<_")[1]);

                // Retornamos la función lambda pre-calculada
                return (f, s) -> f.getStoredOutput() < maxStorage;
            }

            // Regla desconocida, opera normal
            return (f, s) -> true;

        } catch (Exception e) {
            // Script hackeado o mal formado. Retorna null para que la máquina se bloquee.
            return null;
        }
    }

    // ==========================================
    // ⚡ INTERFAZ FUNCIONAL DE ALTO RENDIMIENTO
    // ==========================================
    @FunctionalInterface
    private interface CompiledScript {
        boolean evaluate(ActiveFactory factory, ProtectionStone stone);
    }
}