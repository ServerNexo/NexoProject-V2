package me.nexo.colecciones.colecciones;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 📚 NexoColecciones - Perfil de Datos de Colecciones por Jugador
 * Nota: Objeto instanciado en tiempo real. No requiere Inyección de Dependencias.
 */
public class CollectionProfile {
    private final UUID playerUUID;

    // Memoria de cantidad recolectada (ID de Colección -> Cantidad)
    private final ConcurrentHashMap<String, Integer> progress;

    // 🌟 FIX: Memoria de Tiers reclamados con Sets 100% Concurrentes
    private final ConcurrentHashMap<String, Set<Integer>> claimedTiers;

    private volatile boolean needsFlush = false; // 🌟 FIX: 'volatile' garantiza sincronía entre hilos

    public CollectionProfile(UUID playerUUID, Map<String, Integer> loadedProgress, Map<String, Set<Integer>> loadedClaimedTiers) {
        this.playerUUID = playerUUID;
        this.progress = loadedProgress != null ? new ConcurrentHashMap<>(loadedProgress) : new ConcurrentHashMap<>();

        // 🌟 FIX: Reconstruimos los Sets normales a Sets Concurrentes para evitar Crashes
        this.claimedTiers = new ConcurrentHashMap<>();
        if (loadedClaimedTiers != null) {
            loadedClaimedTiers.forEach((key, value) -> {
                Set<Integer> concurrentSet = ConcurrentHashMap.newKeySet();
                concurrentSet.addAll(value);
                this.claimedTiers.put(key, concurrentSet);
            });
        }
    }

    // ==========================================================
    // 🧮 GESTIÓN DE PROGRESO BASE
    // ==========================================================

    // Método para leer el progreso actual en la RAM
    public int getProgress(String id) {
        return this.progress.getOrDefault(id, 0);
    }

    // Método para añadir progreso silenciosamente (SIN RECLAMO AUTOMÁTICO)
    public void addProgress(String id, int amount) {
        int oldAmount = progress.getOrDefault(id, 0);
        progress.put(id, oldAmount + amount);
        this.needsFlush = true; // Avisa al FlushTask que debe guardar esto
    }

    // Fija un progreso exacto (útil para comandos de Admin)
    public void setProgress(String id, int amount) {
        progress.put(id, amount);
        this.needsFlush = true;
    }

    // ==========================================================
    // 🎁 GESTIÓN DE TIERS (RECLAMO MANUAL)
    // ==========================================================

    // Verifica si un jugador ya cobró un Nivel en específico
    public boolean hasClaimedTier(String collectionId, int tierLevel) {
        Set<Integer> claimed = claimedTiers.get(collectionId);
        return claimed != null && claimed.contains(tierLevel);
    }

    // Marca un Nivel como cobrado permanentemente de forma Thread-Safe
    public void markTierAsClaimed(String collectionId, int tierLevel) {
        // 🌟 FIX: Usamos ConcurrentHashMap.newKeySet() en lugar del HashSet inseguro
        claimedTiers.computeIfAbsent(collectionId, k -> ConcurrentHashMap.newKeySet()).add(tierLevel);
        this.needsFlush = true;
    }

    // ==========================================================
    // ⚙️ GETTERS DE ARQUITECTURA (Para Base de Datos)
    // ==========================================================

    public boolean isNeedsFlush() { return needsFlush; }
    public void setNeedsFlush(boolean needsFlush) { this.needsFlush = needsFlush; }

    public ConcurrentHashMap<String, Integer> getProgressMap() { return progress; }
    public ConcurrentHashMap<String, Set<Integer>> getClaimedTiersMap() { return claimedTiers; }
    public UUID getPlayerUUID() { return playerUUID; }
}