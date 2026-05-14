package me.aeroxis.core.api;

import java.util.UUID;

public interface AeroxisColeccionesAPI {

    // Devuelve cuánto ha farmeado el jugador en total de ese material
    long getCollectionAmount(UUID playerId, String collectionId);

    // 🌟 NUEVO: Permite a otros plugins (como AeroxisIslas o AeroxisMinions) inyectar progreso
    void addCollectionProgress(UUID playerId, String collectionId, int amount);
}