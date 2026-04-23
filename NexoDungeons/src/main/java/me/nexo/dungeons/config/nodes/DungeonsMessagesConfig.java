package me.nexo.dungeons.config.nodes;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

/**
 * 🏰 NexoDungeons - Configurate Type-Safe Node (Arquitectura Enterprise)
 * Rendimiento: Estructura de Mapeo Activo O(1) basada en los módulos purificados.
 */
@ConfigSerializable
public class DungeonsMessagesConfig {

    @Setting private MenusNode menus = new MenusNode();
    @Setting private EmparejamientoNode emparejamiento = new EmparejamientoNode();
    @Setting private OleadasNode oleadas = new OleadasNode();
    @Setting private SeguridadNode seguridad = new SeguridadNode();

    public MenusNode menus() { return menus; }
    public EmparejamientoNode emparejamiento() { return emparejamiento; }
    public OleadasNode oleadas() { return oleadas; }
    public SeguridadNode seguridad() { return seguridad; }

    // =========================================
    // 🗂️ NODO: Menús
    // =========================================
    @ConfigSerializable
    public static class MenusNode {
        @Setting("buscando-fortalezas") private String buscandoFortalezas = "&#FFAA00[⏳] Sincronizando coordenadas en el Vacío... Buscando fortalezas.";
        @Setting("altares-mundo-abierto") private String altaresMundoAbierto = "&#FF5555[!] Los altares de invocación se encuentran en las profundidades del mundo abierto.";
        
        public String buscandoFortalezas() { return buscandoFortalezas; }
        public String altaresMundoAbierto() { return altaresMundoAbierto; }
    }

    // =========================================
    // 🗂️ NODO: Emparejamiento (QueueManager)
    // =========================================
    @ConfigSerializable
    public static class EmparejamientoNode {
        @Setting("ya-en-cola") private String yaEnCola = "&#FF5555[!] Ya te encuentras en la cola de emparejamiento.";
        @Setting("unido-a-cola") private String unidoACola = "&#55FF55[✓] <bold>EMPAREJAMIENTO:</bold> &#E6CCFFTe has unido a la cola de las mazmorras.";
        @Setting("posicion-cola") private String posicionCola = "&#E6CCFFPosición actual: &#00f5ff%posicion%";
        @Setting("abandonar-cola") private String abandonarCola = "&#FF5555[!] Has abandonado la cola de emparejamiento.";
        @Setting("arena-encontrada") private String arenaEncontrada = "&#00f5ff⚔ <bold>ARENA ENCONTRADA:</bold> &#E6CCFFDesplegando en %arena%.";
        @Setting("tamano-escuadron") private String tamanoEscuadron = "&#E6CCFFTamaño del escuadrón: &#55FF55%size% Jugador(es)";

        public String yaEnCola() { return yaEnCola; }
        public String unidoACola() { return unidoACola; }
        public String posicionCola() { return posicionCola; }
        public String abandonarCola() { return abandonarCola; }
        public String arenaEncontrada() { return arenaEncontrada; }
        public String tamanoEscuadron() { return tamanoEscuadron; }
    }

    // =========================================
    // 🗂️ NODO: Oleadas y Arenas (WaveArena)
    // =========================================
    @ConfigSerializable
    public static class OleadasNode {
        @Setting("punto-de-control") private String puntoDeControl = "&#FFAA00[!] <bold>PUNTO DE CONTROL:</bold> &#E6CCFFHas sobrevivido hasta la oleada &#55FF55%checkpoint%&#E6CCFF.";
        @Setting("nueva-oleada-titulo") private String nuevaOleadaTitulo = "&#FF5555<bold>OLEADA %wave%</bold>";
        @Setting("nueva-oleada-subtitulo") private String nuevaOleadaSubtitulo = "&#E6CCFFDefiende la zona...";

        public String puntoDeControl() { return puntoDeControl; }
        public String nuevaOleadaTitulo() { return nuevaOleadaTitulo; }
        public String nuevaOleadaSubtitulo() { return nuevaOleadaSubtitulo; }
    }

    // =========================================
    // 🗂️ NODO: Seguridad y Protección (Listeners)
    // =========================================
    @ConfigSerializable
    public static class SeguridadNode {
        @Setting("anti-dupe-drop") private String antiDupeDrop = "&#FF5555[!] La magia de la mazmorra te impide arrojar objetos aquí.";
        @Setting("evacuacion-segura") private String evacuacionSegura = "&#FFAA00[!] Has sido evacuado a una zona segura tras el colapso de la mazmorra.";
        @Setting("colapso-servidor") private String colapsoServidor = "&#FF5555[!] Las mazmorras han colapsado repentinamente debido a una fluctuación en el Vacío (Reinicio).";
        @Setting("botin-ajeno") private String botinAjeno = "&#FF5555[!] Ese botín le pertenece a otro jugador.";
        @Setting("artefacto-faltante") private String artefactoFaltante = "&#FF5555[!] No posees el artefacto necesario para activar este mecanismo.";

        public String antiDupeDrop() { return antiDupeDrop; }
        public String evacuacionSegura() { return evacuacionSegura; }
        public String colapsoServidor() { return colapsoServidor; }
        public String botinAjeno() { return botinAjeno; }
        public String artefactoFaltante() { return artefactoFaltante; }
    }
}