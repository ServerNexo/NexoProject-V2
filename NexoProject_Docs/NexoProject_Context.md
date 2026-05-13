# NexoProject-V2 - Contexto para IA (Gemini / LLMs)

Este documento está diseñado para ser enviado a un asistente de IA (como Gemini) como contexto inicial del proyecto, evitando así la necesidad de copiar y pegar múltiples clases.

## 1. Arquitectura del Proyecto
* **Tipo:** Ecosistema de servidor de Minecraft (Spigot / Paper / Folia). Java 21 estricto.
* **Estructura:** Monorepo altamente modularizado.
* **Gestor de dependencias:** Gradle (Kotlin DSL - `build.gradle.kts`, `settings.gradle.kts`).
* **Comunicación:** El módulo **`NexoCore`** es el corazón del sistema. Los demás submódulos piden información a `NexoCore`. Algunos módulos tienen `softdepend` entre ellos.

## 2. Tecnologías y Patrones de Diseño Detectados
* **Inyección de Dependencias (DI):** Uso activo de Google Guice (`@Inject`, `@Singleton`) empaquetado vía `ShadowJar` en `NexoCore`. No instanciar clases estáticamente si pueden ser inyectadas.
* **Programación Concurrente:** Uso de estructuras Thread-Safe (`ConcurrentHashMap`) y cachés en memoria.
* **Optimización de Tareas (Schedulers):** Uso de las APIs nativas de Scheduler regionales por entidad (`player.getScheduler().runAtFixedRate`) para mantener "0 Lag" y compatibilidad con Folia. Evitar `BukkitRunnable`.
* **Caché de alta eficiencia:** Uso de `Caffeine Cache` empaquetado en el Core.
* **Framework de Comandos:** **Revxrsal Lamp** (`com.github.revxrsal.Lamp`). Se encuentra globalizado en el ecosistema (no reubicado, para que otros módulos lo usen).
* **Gestor de Configuración:** `Sponge Configurate` (YAML Type-Safe).

## 3. Base de Datos
* **Plataforma:** **Supabase** (PostgreSQL).
* **Driver y Pool:** PostgreSQL + `HikariCP` (Inyectados en el `NexoCore`).
* **Flujo Estricto:** **Todas** las consultas de lectura/escritura hacia Supabase (PostgreSQL) deben ser estrictamente asíncronas (`Zero-Main-Thread`). Se recomienda combinar llamadas de DB con el sistema de Caché (`Caffeine`).

## 4. Dependencias del Ecosistema
El `NexoCore` distribuye (o provee APIs para) varias librerías clave. Dependiendo del módulo que desarrolles, podrías tener acceso a:
* `Paper API 1.21.5-R0.1` (Versión base)
* `Triumph GUI` (`dev.triumphteam:triumph-gui` - Empaquetado en el Core sin reubicar).
* `Nexo` (Motor de recursos/texturas).
* `GeyserMC Floodgate` (Soporte Bedrock).
* `Eco` (`com.willfp:eco`).
* `PlaceholderAPI` (`me.clip:placeholderapi`).
* `AuraSkills` (`dev.aurelium:auraskills-api-bukkit`).

## 5. Resumen de Módulos (Subproyectos)
*   **NexoCore:** Motor central. Maneja la DB (Supabase), comandos (Lamp), menús (Triumph-GUI) y sirve como proveedor de datos para los demás módulos.
*   **NexoMechanics & NexoTools:** Mecánicas de juego base y utilidades administrativas.
*   **NexoPvP, NexoClans & NexoWar:** Combate, sistema de clanes y guerras entre ellos.
*   **NexoEconomy, NexoItems & NexoColecciones:** Sistemas financieros, creación de ítems custom y progresión.
*   **NexoMinions & NexoFactories:** Automatización. Minions y fábricas.
*   **NexoIslas, NexoDungeons & NexoProtections:** Modos de juego (Skyblock), PvE (Mazmorras) y Claims de tierras.
*   **NexoCrates & NexoCosmetics:** Monetización, lootboxes y cosméticos visuales/auditivos (Boombox 3D).
*   **NexoChat:** Formateo de canales.

## 6. Instrucciones para la IA (Tú)
* **Estilo de Código:** Escribe código **Java 21**. Usa `var` cuando quede claro el tipo.
* **Inyección de Dependencias:** Usa `@Inject` y `@Singleton` (Guice) en constructores.
* **Comandos:** Escribe los comandos usando las anotaciones del framework **Revxrsal Lamp** (`@Command`, `@Subcommand`, `@Default`, etc.).
* **Rendimiento y BD:** Las operaciones a **Supabase (PostgreSQL)** se deben ejecutar fuera del hilo principal y en conjunto con `HikariCP`. Si se requieren datos frecuentes, utiliza `Caffeine`.
* **Diseño UI:** Para interfaces (Inventarios), utiliza **Triumph GUI** provisto por el Core.
* **Contexto:** Al solicitar la creación o modificación de una clase, asume que el `build.gradle.kts` del módulo ya tiene acceso a las herramientas del `NexoCore` y enfócate únicamente en la lógica solicitada.