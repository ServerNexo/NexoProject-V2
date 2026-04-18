package me.nexo.core.api;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ServiceManager {

    private final Map<Class<?>, Object> services = new ConcurrentHashMap<>();

    /**
     * Registra una instancia de una API o Manager.
     * @param serviceClass La clase de la interfaz o clase concreta.
     * @param provider La instancia que implementa el servicio.
     * @param <T> El tipo del servicio.
     */
    public <T> void register(Class<T> serviceClass, T provider) {
        services.put(serviceClass, provider);
    }

    /**
     * Obtiene una instancia de un servicio registrado.
     * @param serviceClass La clase del servicio a obtener.
     * @param <T> El tipo del servicio.
     * @return Un Optional que contiene el servicio si se encuentra.
     */
    public <T> Optional<T> get(Class<T> serviceClass) {
        return Optional.ofNullable(serviceClass.cast(services.get(serviceClass)));
    }

    /**
     * Remueve un servicio del registro.
     * @param serviceClass La clase del servicio a remover.
     */
    public void unregister(Class<?> serviceClass) {
        services.remove(serviceClass);
    }
}