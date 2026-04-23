package me.nexo.mechanics.config.nodes;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.List;

/**
 * ⚙️ NexoMechanics - Configurate Type-Safe Node (Arquitectura Enterprise)
 * Rendimiento: Estructura de Mapeo Activo O(1).
 * Nota: Es instanciado internamente por el Configurate Loader, no requiere Guice.
 */
@ConfigSerializable
public class MechanicsMessagesConfig {

    @Setting private MenusNode menus = new MenusNode();
    @Setting private MensajesNode mensajes = new MensajesNode();

    public MenusNode menus() { return menus; }
    public MensajesNode mensajes() { return mensajes; }

    // =========================================
    // 🗂️ NODO: Menus
    // =========================================
    @ConfigSerializable
    public static class MenusNode {
        @Setting("skill_tree") private SkillTreeMenuNode skillTree = new SkillTreeMenuNode();
        public SkillTreeMenuNode skillTree() { return skillTree; }
    }

    @ConfigSerializable
    public static class SkillTreeMenuNode {
        @Setting private String titulo = "&#E6CCFF<bold>»</bold> &#00f5ffÁrbol de Habilidades";
        @Setting private ItemsNode items = new ItemsNode();
        @Setting private NodosNode nodos = new NodosNode();

        public String titulo() { return titulo; }
        public ItemsNode items() { return items; }
        public NodosNode nodos() { return nodos; }
    }

    @ConfigSerializable
    public static class ItemsNode {
        @Setting private MenuItem volver = new MenuItem("&#00f5ff<bold>VOLVER</bold>", List.of());
        @Setting private MenuItem info = new MenuItem("&#00f5ffTu Conocimiento: &#E6CCFF%kp% KP", List.of("&#E6CCFFUsa tus puntos de conocimiento", "&#E6CCFFpara desbloquear nuevas habilidades."));
        public MenuItem volver() { return volver; }
        public MenuItem info() { return info; }
    }

    @ConfigSerializable
    public static class NodosNode {
        @Setting private MenuItem mineria = new MenuItem("&#00f5ffPico del Vacío", List.of("&#E6CCFFDesbloquea el Nivel 1 de Minería.", "", "&#ff00ffCosto: &#E6CCFF%cost% KP"));
        public MenuItem mineria() { return mineria; }
    }

    @ConfigSerializable
    public static class MenuItem {
        @Setting private String nombre;
        @Setting private List<String> lore;
        public MenuItem() {}
        public MenuItem(String nombre, List<String> lore) { this.nombre = nombre; this.lore = lore; }
        public String nombre() { return nombre; }
        public List<String> lore() { return lore; }
    }

    // =========================================
    // 🗂️ NODO: Mensajes Generales y Minijuegos
    // =========================================
    @ConfigSerializable
    public static class MensajesNode {
        @Setting private ErroresNode errores = new ErroresNode();
        @Setting private ExitoNode exito = new ExitoNode();
        @Setting private MinijuegosNode minijuegos = new MinijuegosNode();

        public ErroresNode errores() { return errores; }
        public ExitoNode exito() { return exito; }
        public MinijuegosNode minijuegos() { return minijuegos; }
    }

    @ConfigSerializable
    public static class ErroresNode {
        @Setting("sin-permiso") private String sinPermiso = "&#FF3366[!] El Vacío rechaza tu petición (Sin Permisos).";
        @Setting("sincronizacion-incompleta") private String sincronizacionIncompleta = "&#FF3366[!] Error: Perfil de usuario no sincronizado con la red.";
        @Setting("conocimiento-insuficiente") private String conocimientoInsuficiente = "&#FF3366[!] Conocimiento Insuficiente para realizar este ritual.";
        public String sinPermiso() { return sinPermiso; }
        public String sincronizacionIncompleta() { return sincronizacionIncompleta; }
        public String conocimientoInsuficiente() { return conocimientoInsuficiente; }
    }

    @ConfigSerializable
    public static class ExitoNode {
        @Setting("recarga-exitosa") private String recargaExitosa = "&#9933FF[✓] <bold>TEXTOS SAGRADOS RENOVADOS:</bold> &#E6CCFFMecánicas recargadas.";
        @Setting("nodo-desbloqueado") private String nodoDesbloqueado = "&#00f5ff[✓] <bold>HABILIDAD DESBLOQUEADA:</bold> &#E6CCFFHas adquirido un nuevo conocimiento.";
        @Setting("puntos-restantes") private String puntosRestantes = "&#00f5ff[i] Puntos restantes: &#ff00ff%kp% KP";
        public String recargaExitosa() { return recargaExitosa; }
        public String nodoDesbloqueado() { return nodoDesbloqueado; }
        public String puntosRestantes() { return puntosRestantes; }
    }

    // 🌟 NODO COMPLETO DE MINIJUEGOS 🌟
    @ConfigSerializable
    public static class MinijuegosNode {
        @Setting private TalaNode tala = new TalaNode();
        @Setting private MineriaNode mineria = new MineriaNode();
        @Setting private PescaNode pesca = new PescaNode();
        @Setting private AgriculturaNode agricultura = new AgriculturaNode();
        @Setting private EncantamientoNode encantamiento = new EncantamientoNode();
        @Setting private CombateNode combate = new CombateNode();
        @Setting private AlquimiaNode alquimia = new AlquimiaNode();

        public TalaNode tala() { return tala; }
        public MineriaNode mineria() { return mineria; }
        public PescaNode pesca() { return pesca; }
        public AgriculturaNode agricultura() { return agricultura; }
        public EncantamientoNode encantamiento() { return encantamiento; }
        public CombateNode combate() { return combate; }
        public AlquimiaNode alquimia() { return alquimia; }
    }

    @ConfigSerializable
    public static class TalaNode {
        @Setting("nucleo-destruido-energia") private String nucleoDestruidoEnergia = "&#00f5ff✨ Núcleo destruido. Energía recargada.";
        @Setting("nucleo-destruido") private String nucleoDestruido = "&#00f5ff✨ Núcleo destruido.";
        @Setting("anomalia-botanica") private String anomaliaBotanica = "&#ff00ff🌿 Anomalía Botánica detectada.";
        @Setting("biomasa-endurecida") private String biomasaEndurecida = "&#8b0000[!] La biomasa se ha endurecido.";

        public String nucleoDestruidoEnergia() { return nucleoDestruidoEnergia; }
        public String nucleoDestruido() { return nucleoDestruido; }
        public String anomaliaBotanica() { return anomaliaBotanica; }
        public String biomasaEndurecida() { return biomasaEndurecida; }
    }

    @ConfigSerializable
    public static class MineriaNode {
        @Setting("extraccion-rentable") private String extraccionRentable = "&#00f5ff💎 Extracción rentable: +%amount% Monedas.";
        @Setting("anomalia-geologica") private String anomaliaGeologica = "&#ff00ff⛏ Anomalía Geológica detectada.";
        @Setting("anomalia-estabilizada") private String anomaliaEstabilizada = "&#8b0000[!] La anomalía se ha estabilizado.";

        public String extraccionRentable() { return extraccionRentable; }
        public String anomaliaGeologica() { return anomaliaGeologica; }
        public String anomaliaEstabilizada() { return anomaliaEstabilizada; }
    }

    @ConfigSerializable
    public static class PescaNode {
        @Setting("extraccion-acuatica") private String extraccionAcuatica = "&#00f5ff[✓] <bold>EXTRACCIÓN ACUÁTICA:</bold> &#E6CCFFReservas recargadas &#ff00ff(+5⚡)";
        public String extraccionAcuatica() { return extraccionAcuatica; }
    }

    @ConfigSerializable
    public static class AgriculturaNode {
        @Setting("plaga-biologica") private String plagaBiologica = "&#8b0000🐛 Plaga Biológica";
        @Setting("anomalia-biologica-titulo") private String anomaliaBiologicaTitulo = "&#8b0000<bold>! ALERTA !</bold>";
        @Setting("anomalia-biologica-subtitulo") private String anomaliaBiologicaSubtitulo = "&#ff00ffPlaga detectada en tus cultivos.";
        @Setting("amenaza-erradicada") private String amenazaErradicada = "&#00f5ff[✓] Amenaza erradicada con éxito.";

        public String plagaBiologica() { return plagaBiologica; }
        public String anomaliaBiologicaTitulo() { return anomaliaBiologicaTitulo; }
        public String anomaliaBiologicaSubtitulo() { return anomaliaBiologicaSubtitulo; }
        public String amenazaErradicada() { return amenazaErradicada; }
    }

    @ConfigSerializable
    public static class EncantamientoNode {
        @Setting("puzzle-titulo") private String puzzleTitulo = "&#ff00ff<bold>¡PUZZLE DE ENLACE!</bold>";
        @Setting("puzzle-subtitulo") private String puzzleSubtitulo = "&#E6CCFFGolpea: &#00f5ffAzul &#E6CCFF> &#8b0000Rojo &#E6CCFF> &#00f5ffVerde";
        @Setting("sistema-hackeado") private String sistemaHackeado = "&#ff00ff✨ <bold>SISTEMA HACKEADO:</bold> &#E6CCFFTu próximo ensamblaje será 100% &#00f5ff<bold>GRATUITO</bold>&#E6CCFF.";
        @Setting("secuencia-incorrecta") private String secuenciaIncorrecta = "&#8b0000[!] Secuencia biométrica incorrecta. La energía se ha dispersado.";

        public String puzzleTitulo() { return puzzleTitulo; }
        public String puzzleSubtitulo() { return puzzleSubtitulo; }
        public String sistemaHackeado() { return sistemaHackeado; }
        public String secuenciaIncorrecta() { return secuenciaIncorrecta; }
    }

    @ConfigSerializable
    public static class CombateNode {
        @Setting("racha-combate") private String rachaCombate = "&#8b0000<bold>⚔ ¡RACHA DE COMBATE x%combo%!</bold>";
        @Setting("racha-finalizada") private String rachaFinalizada = "&#E6CCFFRacha de combate finalizada.";
        @Setting("frenesi-titulo") private String frenesiTitulo = "&#8b0000<bold>¡FRENESÍ!</bold>";
        @Setting("frenesi-subtitulo") private String frenesiSubtitulo = "&#ff00ffEnergía de Núcleo Infinita";
        @Setting("frenesi-desvanecido") private String frenesiDesvanecido = "&#E6CCFFEl Frenesí se ha desvanecido. Sistemas retornando a la normalidad.";

        public String rachaCombate() { return rachaCombate; }
        public String rachaFinalizada() { return rachaFinalizada; }
        public String frenesiTitulo() { return frenesiTitulo; }
        public String frenesiSubtitulo() { return frenesiSubtitulo; }
        public String frenesiDesvanecido() { return frenesiDesvanecido; }
    }

    @ConfigSerializable
    public static class AlquimiaNode {
        @Setting("peligro-titulo") private String peligroTitulo = "&#8b0000<bold>! PELIGRO !</bold>";
        @Setting("peligro-subtitulo") private String peligroSubtitulo = "&#ff00ffMezcla Inestable. Agáchate (Shift) x3 para estabilizar.";
        @Setting("mezcla-estabilizada") private String mezclaEstabilizada = "&#00f5ff[✓] <bold>MEZCLA ESTABILIZADA:</bold> &#E6CCFFLa pureza de los químicos ha sido potenciada.";

        public String peligroTitulo() { return peligroTitulo; }
        public String peligroSubtitulo() { return peligroSubtitulo; }
        public String mezclaEstabilizada() { return mezclaEstabilizada; }
    }
}