package me.nexo.core.api;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import me.nexo.core.NexoCore;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class NexoWebServer {

    private HttpServer server;
    private final Gson gson = new Gson();
    private final NexoCore plugin;

    public NexoWebServer(NexoCore plugin) {
        this.plugin = plugin;
    }

    public void start() {
        try {
            // Levantamos el servidor en el puerto 8080
            server = HttpServer.create(new InetSocketAddress(8080), 0);

            // 🚀 MAGIA ZERO-LAG: Usamos Java 21 Virtual Threads para manejar miles de peticiones
            server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

            // Endpoint 1: Economía (Bazar)
            server.createContext("/api/bazaar", exchange -> {
                try {
                    // Aquí leerías tu caché de Bazar real. Para el ejemplo devolvemos un JSON estático.
                    String response = gson.toJson("{ 'status': 'ok', 'items': 'lista_aqui' }");

                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, response.getBytes().length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            // Endpoint 2: Factorías (Mecánicas)
            server.createContext("/api/factories/", exchange -> {
                String response = gson.toJson("{ 'status': 'online', 'energy': 500 }");
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
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
            server.stop(0);
            plugin.getLogger().info("🌐 NexoWebAPI detenida correctamente.");
        }
    }
}