package me.nexo.core.api;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sun.net.httpserver.HttpServer;
import me.nexo.core.NexoCore;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/**
 * 🌐 Nexo Network - Web API Interna (Arquitectura Enterprise)
 * Servidor HTTP de altísimo rendimiento impulsado por Virtual Threads.
 */
@Singleton // 🌟 FIX CRÍTICO: Previene BindException (Puerto ya en uso) garantizando instancia única
public class NexoWebServer {

    private HttpServer server;
    private final Gson gson;
    private final NexoCore plugin;

    // 💉 PILAR 1: Inyección de Dependencias
    @Inject
    public NexoWebServer(NexoCore plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
    }

    public void start() {
        try {
            // Levantamos el servidor en el puerto 8080
            server = HttpServer.create(new InetSocketAddress(8080), 0);

            // 🚀 MAGIA ZERO-LAG: Usamos Java 21 Virtual Threads para manejar miles de peticiones simultáneas
            server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

            // Endpoint 1: Economía (Bazar)
            server.createContext("/api/bazaar", exchange -> {
                try {
                    // Aquí leerías tu caché de Bazar real. Para el ejemplo devolvemos un JSON estático.
                    String response = gson.toJson("{ 'status': 'ok', 'items': 'lista_aqui' }");
                    
                    // 🛡️ FIX: Seguridad de codificación cruzada (Cross-Platform Encoding)
                    byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);

                    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                    exchange.sendResponseHeaders(200, responseBytes.length);
                    
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(responseBytes);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            // Endpoint 2: Factorías (Mecánicas)
            server.createContext("/api/factories/", exchange -> {
                try {
                    String response = gson.toJson("{ 'status': 'online', 'energy': 500 }");
                    
                    // 🛡️ FIX: Estandarización UTF-8
                    byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
                    
                    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                    exchange.sendResponseHeaders(200, responseBytes.length);
                    
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(responseBytes);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            server.start();
            plugin.getLogger().info("🌐 NexoWebAPI iniciada en el puerto 8080 (Virtual Threads activos)");
        } catch (IOException e) {
            plugin.getLogger().severe("❌ Error crítico: No se pudo iniciar el servidor web: " + e.getMessage());
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0); // El "0" indica que se cierra inmediatamente, ideal para un onDisable rápido
            plugin.getLogger().info("🌐 NexoWebAPI detenida correctamente.");
        }
    }
}