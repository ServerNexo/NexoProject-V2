package me.nexo.colecciones.colecciones;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 📚 NexoColecciones - Perfil de Datos de Colecciones por Jugador
 * Rendimiento: 100% Thread-Safe (Operaciones Atómicas).
 * Nota: Objeto instanciado en tiempo real. No requiere Inyección de Dependencias.
 */
public class CollectionProfile {
    
    private final UUID playerUUID;

    // Memoria de cantidad recolectada (ID de Colección -> Cantidad)
    private final ConcurrentHashMap<String, Integer> progress;

    // Memoria de Tiers reclamados con Sets 100% Concurrentes
    private final ConcurrentHashMap<String, Set<Integer>> claimedTiers;

    private volatile boolean needsFlush = false; // 🌟 Garantiza sincronía entre hilos y la CPU

    public CollectionProfile(UUID playerUUID, Map<String, Integer> loadedProgress, Map<String, Set<Integer>> loadedClaimedTiers) {
        this.playerUUID = playerUUID;
        this.progress = loadedProgress != null ? new ConcurrentHashMap<>(loadedProgress) : new ConcurrentHashMap<>();

        // Reconstruimos los Sets normales a Sets Concurrentes para evitar Crashes
        this.claimedTiers = new ConcurrentHashMap<>();
        if (loadedClaimedTiers != null) {
            loadedClaimedTiers.forEach((key, value) -> {
                var concurrentSet = ConcurrentHashMap.<Integer>newKeySet();
                concurrentSet.addAll(value);
                this.claimedTiers.put(key, concurrentSet);
            });
        }
    }

    // ==========================================================
    // 🧮 GESTIÓN DE PROGRESO BASE
    // ==========================================================

    public int getProgress(String id) {
        return this.progress.getOrDefault(id, 0);
    }

    public void addProgress(String id, int amount) {
        // 🌟 FIX CONCURRENCIA: Operación atómica pura (Merge) en lugar de Check-Then-Act (Get+Put).
        // Si dos mobs mueren en el mismo tick, ningún progreso se perderá.
        progress.merge(id, amount, Integer::sum);
        this.needsFlush = true; // Avisa al FlushTask que debe guardar esto
    }

    public void setProgress(String id, int amount) {
        progress.put(id, amount);
        this.needsFlush = true;
    }

    // ==========================================================
    // 🎁 GESTIÓN DE TIERS (RECLAMO MANUAL)
    // ==========================================================

    public boolean hasClaimedTier(String collectionId, int tierLevel) {
        var claimed = claimedTiers.get(collectionId);
        return claimed != null && claimed.contains(tierLevel);
    }

    public void markTierAsClaimed(String collectionId, int tierLevel) {
        claimedTiers.computeIfAbsent(collectionId, k -> ConcurrentHashMap.newKeySet()).add(tierLevel);
        this.needsFlush = true;
    }

    // ==========================================================
    // ⚙️ GETTERS DE ARQUITECTURA (Para Base de Datos en FlushTask)
    // ==========================================================

    public boolean isNeedsFlush() { return needsFlush; }
    public void setNeedsFlush(boolean needsFlush) { this.needsFlush = needsFlush; }

    public ConcurrentHashMap<String, Integer> getProgressMap() { return progress; }
    public ConcurrentHashMap<String, Set<Integer>> getClaimedTiersMap() { return claimedTiers; }
    public UUID getPlayerUUID() { return playerUUID; }
}