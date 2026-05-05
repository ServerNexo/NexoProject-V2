package me.nexo.core.api;
import java.util.UUID;

public interface NexoColeccionesAPI {
    // Devuelve cuánto ha farmeado el jugador en total de ese material
    long getCollectionAmount(UUID playerId, String collectionId);
}