package me.aeroxis.core.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sun.net.httpserver.HttpServer;
import me.aeroxis.core.AeroxisCore;
import me.aeroxis.core.crossplay.CrossplayUtils;
import net.kyori.adventure.text.Component; // 🌟 IMPORT COMPONENT
import net.kyori.adventure.title.Title; // 🌟 IMPORT TITLE API
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration; // 🌟 IMPORT DURATION PARA LOS TIEMPOS
import java.util.concurrent.Executors;

/**
 * 🌐 Aeroxis Network - Web API Interna (Arquitectura Enterprise)
 * Servidor HTTP de altísimo rendimiento impulsado por Virtual Threads.
 * Compatible con integraciones universales (TikTok, Kick, Twitch).
 */
@Singleton // 🌟 FIX CRÍTICO: Previene BindException garantizando instancia única
public class AeroxisWebServer {

    private HttpServer server;
    private final Gson gson;
    private final AeroxisCore plugin;
    private final CrossplayUtils crossplayUtils; // 🌟 AÑADIDO PARA ANUNCIOS GLOBALES

    // 🔒 Clave secreta universal (En producción, pon esto en tu config.yml)
    private static final String CREATOR_SECRET_KEY = "AEROXIS_STREAM_2026_SECRET";

    // 💉 PILAR 1: Inyección de Dependencias
    @Inject
    public AeroxisWebServer(AeroxisCore plugin, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.crossplayUtils = crossplayUtils;
        this.gson = new Gson();
    }

    public void start() {
        try {
            // Levantamos el servidor en el puerto 8080
            server = HttpServer.create(new InetSocketAddress(8080), 0);

            // 🚀 MAGIA ZERO-LAG: Usamos Java 21 Virtual Threads
            server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

            // Endpoint 1: Economía (Bazar)
            server.createContext("/api/bazaar", exchange -> {
                try {
                    String response = gson.toJson("{ 'status': 'ok', 'items': 'lista_aqui' }");
                    sendResponse(exchange, 200, response);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            // Endpoint 2: Factorías (Mecánicas)
            server.createContext("/api/factories/", exchange -> {
                try {
                    String response = gson.toJson("{ 'status': 'online', 'energy': 500 }");
                    sendResponse(exchange, 200, response);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            // 🌟 ENDPOINT 3: INTEGRACIÓN UNIVERSAL DE STREAMERS (TikTok, Kick, Twitch)
            server.createContext("/api/stream/booster", exchange -> {
                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendResponse(exchange, 405, "{\"error\": \"Solo se permiten peticiones POST\"}");
                    return;
                }

                try {
                    // 1. Leemos el cuerpo (Body) del mensaje enviado por el bot del Streamer
                    InputStream is = exchange.getRequestBody();
                    String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);

                    JsonObject json = gson.fromJson(body, JsonObject.class);

                    // 2. Validamos la seguridad
                    if (!json.has("secret") || !CREATOR_SECRET_KEY.equals(json.get("secret").getAsString())) {
                        sendResponse(exchange, 401, "{\"error\": \"Clave de acceso (Secret) denegada o ausente\"}");
                        return;
                    }

                    // 3. Extraemos los datos del Streamer y la plataforma
                    String streamerName = json.has("streamer") ? json.get("streamer").getAsString() : "Un Creador Misterioso";
                    String platform = json.has("platform") ? json.get("platform").getAsString() : "Directo";

                    // 4. 🚀 Volvemos al Hilo Principal (Main Thread) de Bukkit para hacer magia
                    Bukkit.getScheduler().runTask(plugin, () -> activarBoosterGlobal(streamerName, platform));

                    // 5. Respondemos al Bot diciendo que todo salió perfecto
                    sendResponse(exchange, 200, "{\"status\": \"Booster Activado Exitosamente\"}");

                } catch (JsonSyntaxException e) {
                    sendResponse(exchange, 400, "{\"error\": \"JSON malformado\"}");
                } catch (Exception e) {
                    e.printStackTrace();
                    sendResponse(exchange, 500, "{\"error\": \"Error interno del servidor\"}");
                }
            });

            server.start();
            plugin.getLogger().info("🌐 AeroxisWebAPI iniciada en el puerto 8080 (Virtual Threads activos)");
        } catch (IOException e) {
            plugin.getLogger().severe("❌ Error crítico: No se pudo iniciar el servidor web: " + e.getMessage());
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            plugin.getLogger().info("🌐 AeroxisWebAPI detenida correctamente.");
        }
    }

    // ==========================================
    // 🛠️ UTILS INTERNOS
    // ==========================================

    /**
     * Método centralizado para enviar respuestas HTTP limpias
     */
    private void sendResponse(com.sun.net.httpserver.HttpExchange exchange, int statusCode, String jsonResponse) throws IOException {
        byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    /**
     * Se ejecuta en el Main Thread para aplicar efectos y anunciar al servidor
     */
    private void activarBoosterGlobal(String streamerName, String platform) {
        // Anuncio espectacular a todo el servidor
        crossplayUtils.broadcastMessage("\n&#9146FF<bold>🎥 ¡BENDICIÓN DE LA COMUNIDAD! 🎥</bold>");
        crossplayUtils.broadcastMessage("&#E6CCFFLa audiencia de &#55FF55" + streamerName + " &#E6CCFF(en " + platform + ") ha activado un Booster.");
        crossplayUtils.broadcastMessage("&#FFD700¡EXPERIENCIA Y LOOT X2 PARA TODOS!\n");

        // 🌟 FIX: API de Adventure Moderna con colores HEX soportados
        Component mainTitle = crossplayUtils.parseCrossplay(null, "&#D700FF<bold>¡BOOSTER ACTIVO!</bold>");
        Component subTitle = crossplayUtils.parseCrossplay(null, "&#FFFFFFGracias al directo de &#55FF55" + streamerName);
        Title.Times times = Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3500), Duration.ofMillis(1000));
        Title boosterTitle = Title.title(mainTitle, subTitle, times);

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);
            p.showTitle(boosterTitle); // Uso del método moderno
        }

        // TODO: Activar variable real en tu Manager de Economía/PvE para que dé el x2.
        plugin.getLogger().info("🚀 BOOSTER GLOBAL x2 activado por: " + streamerName + " vía " + platform);
    }
}