# NexoProject-V2 - Resumen de Clases por Módulo

Este documento proporciona un mapa estructural de alto nivel de las clases en cada módulo, explicando brevemente la responsabilidad principal de las carpetas y componentes sin necesidad de leer el código fuente completo.

---

### 1. ⚙️ NexoCore
El motor y proveedor central de utilidades, bases de datos y eventos globales.
*   **api/**: Interfaces de comunicación globales y servidor web integrado (`NexoWebServer`, `ServiceManager`).
*   **bosses/**: Lógica de IA, daño e invasión de Jefes globales (`GlobalBossCombatListener`, `BossScaler`).
*   **cataclysms/**: Sistema de eventos mundiales y meteoritos.
*   **commands/**: Comandos base (Ej: `/nexo`).
*   **crossplay/**: Utilidades vitales para compatibilidad Bedrock-Java (Ej: `NexoDisplayFixer`, optimización de partículas).
*   **database/**: Conexión asíncrona a Supabase mediante HikariCP.
*   **events/**: Gestor de eventos custom programados mediante cron (`NexoCronUtil`).
*   **hub/ & menus/**: Lógica de inventarios (Triumph GUI), donaciones y bendiciones del vacío (Void Blessing).
*   **user/**: El manejador fundamental de perfiles de jugador (`UserManager`, `UserRepository`).

---

### 2. 💬 NexoChat
Control y formato del chat del ecosistema.
*   **commands/**: Comandos como `/chat` y `/stream`.
*   **managers/**: Escuchas de conexión/desconexión y muertes para anunciar en chat, así como mensajes privados.
*   **menu/**: Interfaces para seleccionar colores de chat y tags.
*   **render/**: Renderizado avanzado de tarjetas de perfil (Hover en el chat) y dibujo de cabezas.
*   **utils/**: Permisos y expansión para PlaceholderAPI.

---

### 3. 🛡️ NexoClans & ⚔️ NexoWar
Sistemas de grupos de jugadores y batallas intergrupales.
*   **NexoClans**:
    *   **core/**: Entidad `NexoClan` y perfiles de miembros.
    *   **listeners/**: Fuego amigo y daño.
    *   **menu/**: Gestión visual de miembros e invitaciones.
*   **NexoWar**:
    *   **core/**: Gestión de contratos de guerra (`WarContract`).
    *   **managers/**: Inicia y finaliza conflictos a gran escala con soporte crossplay (`WarCrossplayListener`).

---

### 4. 💰 NexoEconomy
Economía, comercio y mercados globales.
*   **bazar/**: Sistema avanzado de compra/venta basado en órdenes y escucha de chat (posiblemente para fijar precios).
*   **blackmarket/**: Sistema de mercado negro rotativo.
*   **trade/**: Sistema seguro de intercambio de ítems directo entre jugadores.

---

### 5. 🎒 NexoItems & 📜 NexoColecciones
Gestión avanzada de inventarios y colecciones.
*   **NexoItems**:
    *   **accesorios/ & artefactos/**: Sistemas de equipamiento y atributos adicionales mediante DTOs.
    *   **estaciones/**: Funcionalidades de crafteo avanzado (Desguace, Reforja, Herrería, Yunque).
    *   **guardarropa/ & mochilas/**: Inventarios virtuales y PVs (Player Vaults).
    *   **mecanicas/**: Sincronización de ítems y lógicas custom al minar/pescar.
    *   **crossplay/**: Generador de mapeos para Bedrock.
*   **NexoColecciones**:
    *   **data/**: Categorías, niveles y plantillas de recompensa de colecciones.
    *   **slayers/**: Sistema de cacería de mobs (Slayers) con misiones y progreso.

---

### 6. 🏭 NexoFactories & 🤖 NexoMinions
Sistemas de automatización y generación pasiva.
*   **NexoFactories**:
    *   **core/**: Recetas de crafteo automatizado y plantillas de estructuras.
    *   **logic/**: Evaluación de scripts o condiciones para producción lógica.
    *   **managers/**: Escaneo de blueprints (planos), silos de almacenamiento de granjas.
*   **NexoMinions**:
    *   **data/**: ADN de minions y niveles (Tiers).
    *   **manager/**: Gestión de las entidades `ActiveMinion` y sus actualizaciones.
    *   **menu/**: Recolección visual e interacción.

---

### 7. 🏰 NexoDungeons & 🗺️ NexoIslas
Espacios instanciados y PvE.
*   **NexoDungeons**:
    *   **engine/**: Motores de generación, escalado de abismos y puzles.
    *   **matchmaking/**: Sistema de colas (`QueueManager`) para entrar a mazmorras grupales.
    *   **modes/**: Múltiples tipos de mazmorra (Por Oleadas, Puzles, de Invocación).
    *   **nemesis/**: Sistema de persecución/hunt de enemigos.
*   **NexoIslas**:
    *   **data/**: Bases de datos individuales por isla, roles y miembros.
    *   **managers/**: Lógica de niveles de isla (`IslandLevelEngine`) y mejoras.
    *   **menus/**: Panel de control visual, tops y ajustes de seguridad.

---

### 8. 🛡️ NexoProtections
Reclamación (Claims) de terrenos.
*   **core/**: Cajas de claims (`ClaimBox`) y piedras de protección.
*   **managers/**: Límites, mantenimiento de protecciones (Upkeep) y control del entorno (explosiones/fuego).
*   **menu/**: Gestión de miembros y configuraciones de banderas (Flags) de protección.

---

### 9. ⚔️ NexoPvP
Combate directo jugador vs jugador.
*   **classes/**: Sistemas de peso de armadura y clases de combate.
*   **combat/**: Registro de combos y gestión del 'Poise' (Postura/Estabilidad).
*   **mechanics/**: Penalizaciones por muerte y estaciones de entrenamiento.
*   **pasivas/**: Manejo de habilidades pasivas de PvP.

---

### 10. 🛠️ NexoMechanics & 🧰 NexoTools
Mecánicas extras del juego y utilidades para administradores.
*   **NexoMechanics**:
    *   **archeology/**: Rastreador por brújula y eventos de arqueología.
    *   **minigames/**: Minijuegos de farmeo, encantamiento, alquimia y pesca.
    *   **skills/**: Árboles de habilidades.
*   **NexoTools**:
    *   **commands/**: Comandos de teleportación (TPA) y utilidades.
    *   **managers/**: Guardado de localizaciones y peticiones de TP.

---

### 11. 🎁 NexoCrates & 🎨 NexoCosmetics
Monetización visual y recompensas.
*   **NexoCrates**: Cajas con historial, ruletas y menús de revelado de cartas (Card Reveal).
*   **NexoCosmetics**: Motor de cosméticos, vinculación con el cielo/skyblock y gestor de Boombox (Música).