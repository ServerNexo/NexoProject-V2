# 🌌 AeroxisProject V2 - Enterprise Ecosystem

![Java](https://img.shields.io/badge/Java-21%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![PaperMC](https://img.shields.io/badge/API-Paper_1.21.5-black?style=for-the-badge&logo=paper)
![Gradle](https://img.shields.io/badge/Build-Gradle_Kotlin_DSL-02303A?style=for-the-badge&logo=gradle)
![Guice](https://img.shields.io/badge/DI-Google_Guice-4285F4?style=for-the-badge&logo=google)

**AeroxisProject V2** es el motor central (Core) de alta disponibilidad diseñado para **Aeroxis Network**. Construido bajo una arquitectura AAA, este proyecto modular elimina el código espagueti y el "Thread Starvation", utilizando inyección de dependencias estricta, bases de datos asíncronas e Hilos Virtuales nativos.

---

## 🏗️ Arquitectura y Tecnologías Base

Este proyecto rechaza las prácticas convencionales de Spigot en favor de metodologías de Software Engineering puro:

* **⚡ Virtual Threads (Java 21):** Todas las lecturas/escrituras (I/O) a la base de datos se ejecutan en hilos virtuales, asegurando **0% de impacto en los TPS (Ticks Per Second)** del servidor.
* **💉 Google Guice (DI):** Sistema de Inyección de Dependencias. Cero variables estáticas o singletons globales descontrolados. Cada módulo hereda del *Injector* del Core.
* **🗄️ PostgreSQL & HikariCP:** Almacenamiento relacional rápido y concurrente con persistencia tipo Batch.
* **🧠 Caffeine Cache:** Memoria caché ultrarrápida O(1) para perfiles de usuarios y balances económicos, previniendo abusos o duplicaciones.
* **🎨 Adventure API & MiniMessage:** Sistemas de chat y menús renderizados puramente mediante componentes y HEX Colors (`<gradient>`, `<#FF0000>`), sin depender del obsoleto `§`.
* **🎮 Crossplay Nativo:** Integración estricta para mitigar bugs visuales de Floodgate/Geyser en clientes Bedrock.

---

## 🧩 Ecosistema de Módulos

El ecosistema está dividido en submódulos de Gradle para garantizar una escalabilidad limpia:

| Módulo                    | Descripción |
|:--------------------------| :--- |
| **`AeroxisCore`**         | El núcleo. Gestiona la conexión DB, caché de usuarios universal, API de menús, servidor Web interno y optimizaciones Crossplay. |
| **`AeroxisChat`**            | Formateo AAA. Soporte de Emojis Custom (`\uE001`), Muteos temporales en PostgreSQL, Anti-Spam, SocialSpy y Placeholders de Rango. |
| **`AeroxisEconomy`**         | Motor financiero atómico (Coins, Gems, Mana). Mercado Negro, Bazar de jugadores y sesiones de Tradeo seguras. |
| **`AeroxisItems`**           | Gestor de Custom Items. Armas, Armaduras, Artefactos, Reforjas, Guardarropa, Mochilas y mecánicas de recolección RPG. |
| **`AeroxisDungeons`**        | Instancias aisladas. Matchmaking, arenas por oleadas (Waves), generación procedural de grid, puzzles y Jefes con Loot distribuido. |
| **`AeroxisFactories`**       | Automatización tipo Factorio. Planos (Blueprints), scripts lógicos de evaluación y estructuras de fabricación automatizada. |
| **`AeroxisMechanics`**       | Minijuegos de recolección inmersivos (Pesca, Minería, Tala, Agricultura), Combos de Combate y el Árbol de Habilidades (Skill Tree). |
| **`AeroxisMinions`**         | Entidades de farmeo offline, cálculos matemáticos basados en Tiers, mejoras (Upgrades) y manipulación de hologramas. |
| **`AeroxisPvP` & `AeroxisWar`** | Clases de armadura, penalizaciones de muerte, mecánicas de entrenamiento (Temples), Pasivas, Guerras crossplay y clanes. |
| **`AeroxisProtections`**     | Piedras de Protección (Protection Stones) y Claim Boxes con gestión avanzada de banderas (Flags) y límites de mantenimiento (Upkeep). |
| **`AeroxisColecciones`**     | Registro de Slayers, niveles de recolección (Tiers) y categorías para incentivar el "grindeo" de la comunidad. |
| **`AeroxisClans`**           | Facciones ligeras. Clanes, miembros, jerarquías, y chat privado de facción. |

---

## 🚀 Compilación y Despliegue

### Requisitos Previos:
* [Java Development Kit (JDK) 21+](https://adoptium.net/)
* Git

### Construcción (Build):
El proyecto utiliza Gradle Wrapper, por lo que no necesitas instalar Gradle en tu sistema.
```bash
# 1. Clonar el repositorio
git clone [https://github.com/tu-usuario/nexoproject-v2.git](https://github.com/tu-usuario/nexoproject-v2.git)
cd nexoproject-v2

# 2. Compilar el Core y todos los submódulos usando ShadowJar
./gradlew clean build