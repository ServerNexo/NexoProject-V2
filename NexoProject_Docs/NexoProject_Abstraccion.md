# NexoProject-V2 - Abstracción de Contextos y Módulos

El proyecto `NexoProject-V2` es un ecosistema de servidor Minecraft altamente modularizado gestionado a través de **Gradle (Kotlin DSL)**. Su arquitectura se basa en dividir funcionalidades masivas en submódulos (plugins) específicos e interconectados.

A continuación, se presenta la abstracción conceptual de cada módulo y la funcionalidad que representa dentro de tu ecosistema, sin haber modificado una sola línea de tu código fuente existente:

## ⚙️ Núcleo y Dependencias Base
*   **NexoCore**: El motor central del proyecto. Seguramente contiene las utilidades base (bases de datos, formateo, APIs compartidas) sobre la cual todos los demás módulos se apoyan.
*   **NexoMechanics**: Mecánicas generales del servidor/juego. Ideal para características que no encajan en una categoría específica pero afectan la jugabilidad de manera global.
*   **NexoTools**: Herramientas utilitarias para la administración o soporte general.

## ⚔️ Sistemas de Combate y Jugadores
*   **NexoPvP**: Abstracción de todas las funcionalidades relacionadas con el Player vs Player (Mecánicas de combate, recompensas, o tracking).
*   **NexoClans**: Sistema de agrupamiento de jugadores (Clanes/Guilds) con jerarquías y gestión de grupos.
*   **NexoWar**: Sistema de conflictos a gran escala (Guerras). Probablemente fuertemente acoplado a `NexoClans` para eventos GvG (Guild vs Guild).

## 💰 Economía y Progresión
*   **NexoEconomy**: El motor económico. Controla balances, monedas virtuales y transacciones entre jugadores o el sistema.
*   **NexoItems**: Sistema dedicado a la gestión y creación de ítems personalizados (Armas custom, consumibles, tiers, etc).
*   **NexoColecciones**: Sistema de recolección de objetos o progreso a largo plazo.

## 🏭 Sistemas de Producción y Automatización
*   **NexoMinions**: Sistema de entidades automatizadas que farmean, minan o recolectan recursos por el jugador.
*   **NexoFactories**: Abstracción para el procesamiento a gran escala de ítems y materiales.

## 🗺️ Mundo y Entorno
*   **NexoIslas**: Modo de juego tipo Skyblock. Gestión de mundos instanciados, progresión y mejoras de isla por jugador.
*   **NexoDungeons**: Instancias PvE (Jugador vs Entorno). Generación o gestión de eventos con bosses y recompensas estructuradas.
*   **NexoProtections**: Sistema de reclamación de tierras (claims) o protección de zonas específicas contra modificaciones no deseadas.

## 🎁 Recompensas y Cosméticos
*   **NexoCrates**: Sistema de Lootboxes/Cajas de recompensas. Usado típicamente para monetización o recompensas de eventos.
*   **NexoCosmetics**: Cosméticos de jugador. Aquí reside el **BoomboxManager** (Radio 3D en movimiento), partículas, sombreros, etc.

## 💬 Social
*   **NexoChat**: Formateo de chat, gestión de canales, rangos visibles y mensajería privada.

---
*Nota: Este análisis fue generado puramente leyendo la estructura de directorios y el archivo `settings.gradle.kts` para ofrecerte una visión general de alto nivel de lo que has construido hasta ahora.*