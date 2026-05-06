package me.nexo.core.api;

import java.util.UUID;

public interface NexoColeccionesAPI {

    // Devuelve cuánto ha farmeado el jugador en total de ese material
    long getCollectionAmount(UUID playerId, String collectionId);

    // 🌟 NUEVO: Permite a otros plugins (como NexoIslas o NexoMinions) inyectar progreso
    void addCollectionProgress(UUID playerId, String collectionId, int amount);
}