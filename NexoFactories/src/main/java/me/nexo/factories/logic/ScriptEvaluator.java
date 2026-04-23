package me.nexo.factories.logic;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.factories.core.ActiveFactory;

/**
 * 🏭 NexoFactories - Evaluador Lógico (Arquitectura Enterprise Java 21)
 * Rendimiento: Compilación AOT (Ahead-Of-Time), Caché O(1) y Desacoplamiento Total.
 */
@Singleton
public class ScriptEvaluator {

    // 🌟 MAGIA ENTERPRISE: Caché de Scripts "Compilados".
    private final Cache<String, CompiledScript> scriptCache;

    // 💉 PILAR 1: Inyección de Dependencias (Constructor Explícito)
    @Inject
    public ScriptEvaluator() {
        this.scriptCache = Caffeine.newBuilder()
                .maximumSize(5000) // Soporta hasta 5000 scripts únicos en memoria
                .build();
    }

    /**
     * Evalúa si una máquina DEBE encenderse o apagarse basándose en su Script.
     * Complejidad: O(1) puro.
     * * @param factory La fábrica a evaluar.
     * @param currentEnergy La energía de la red (Desacoplado de ProtectionStone para evitar ClassLoader Crashes).
     * @param jsonScript El script JSON en texto crudo.
     * @return true si la máquina debe operar, false de lo contrario.
     */
    public boolean shouldRun(ActiveFactory factory, double currentEnergy, String jsonScript) {
        // Si no hay script, opera normalmente
        if (jsonScript == null || jsonScript.isEmpty() || jsonScript.equals("NONE")) {
            return true;
        }

        // 🚀 Obtenemos el script compilado O(1). Si no existe, lo compila (solo la primera vez)
        CompiledScript compiled = scriptCache.get(jsonScript, this::compileScript);

        // Si la compilación falló por un script corrupto, apagamos por seguridad
        if (compiled == null) return false;

        // Ejecutamos la matemática pura (Cero lag y sin acoplamiento)
        return compiled.evaluate(factory, currentEnergy);
    }

    /**
     * 🧠 COMPILADOR INTERNO: Convierte texto pesado en lambdas de alto rendimiento.
     */
    private CompiledScript compileScript(String rawJson) {
        try {
            JsonObject logic = JsonParser.parseString(rawJson).getAsJsonObject();

            if (!logic.has("condition")) {
                return (f, energy) -> true; // Lambda por defecto (Siempre encendido)
            }

            String condition = logic.get("condition").getAsString();

            // REGLA 1: Prioridad de Escudo
            if (condition.startsWith("ENERGY_>_")) {
                double requiredEnergy = Double.parseDouble(condition.split("_>_")[1]);

                // 🌟 FIX: Evaluamos el double puro sin depender de llamadas reflectivas o módulos externos
                return (f, energy) -> energy > requiredEnergy;
            }

            // REGLA 2: Límite de Almacenamiento
            if (condition.startsWith("STORAGE_<_")) {
                int maxStorage = Integer.parseInt(condition.split("_<_")[1]);

                return (f, energy) -> f.getStoredOutput() < maxStorage;
            }

            // Regla desconocida, opera normal
            return (f, energy) -> true;

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
        boolean evaluate(ActiveFactory factory, double currentEnergy);
    }
}