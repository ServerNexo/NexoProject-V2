package me.nexo.core.api;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🏛️ Nexo Network - Gestor de Servicios Legacy (Service Locator)
 * Arquitectura Enterprise: Actúa como un puente temporal (Legacy Bridge) 
 * administrado por Guice para mantener compatibilidad con módulos antiguos.
 * * ⚠️ NOTA: Para módulos nuevos, utiliza la Inyección de Dependencias directa (@Inject).
 */
@Singleton
public class ServiceManager {

    // 🛡️ Memoria RAM concurrente de alta velocidad
    private final Map<Class<?>, Object> services = new ConcurrentHashMap<>();

    // 💉 PILAR 1: Inyección de Dependencias. Guice garantiza una instancia única.
    @Inject
    public ServiceManager() {
        // Constructor listo para el inyector
    }

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