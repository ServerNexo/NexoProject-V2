package me.nexo.items.config.nodes;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.List;

/**
 * 🎒 NexoItems - Configurate Type-Safe Node (Arquitectura Enterprise)
 */
@ConfigSerializable
public class ItemsMessagesConfig {

    @Setting private MensajesNode mensajes = new MensajesNode();
    @Setting private ComandosNode comandos = new ComandosNode();
    @Setting private EstacionesNode estaciones = new EstacionesNode();
    @Setting private MochilasNode mochilas = new MochilasNode();
    @Setting private MenusNode menus = new MenusNode();
    @Setting private EventosNode eventos = new EventosNode();

    public MensajesNode mensajes() { return mensajes; }
    public ComandosNode comandos() { return comandos; }
    public EstacionesNode estaciones() { return estaciones; }
    public MochilasNode mochilas() { return mochilas; }
    public MenusNode menus() { return menus; }
    public EventosNode eventos() { return eventos; }

    // =========================================
    // 🗂️ NODO: Menus (Inventarios Custom)
    // =========================================
    @ConfigSerializable
    public static class MenusNode {
        @Setting private DesguaceMenuNode desguace = new DesguaceMenuNode();
        @Setting private HerreriaMenuNode herreria = new HerreriaMenuNode();

        public DesguaceMenuNode desguace() { return desguace; }
        public HerreriaMenuNode herreria() { return herreria; }
    }

    @ConfigSerializable
    public static class HerreriaMenuNode {
        @Setting private String titulo = "&#ff8c00⚒ <bold>FORJA DE ASCENSIÓN</bold>";
        @Setting private BotonNode boton = new BotonNode();

        public String titulo() { return titulo; }
        public BotonNode boton() { return boton; }
    }

    @ConfigSerializable
    public static class DesguaceMenuNode {
        @Setting private String titulo = "&#ff00ff♻ <bold>MESA DE DESGUACE</bold>";
        @Setting private BotonNode boton = new BotonNode();

        public String titulo() { return titulo; }
        public BotonNode boton() { return boton; }
    }

    @ConfigSerializable
    public static class BotonNode {
        @Setting private String titulo = "&#00f5ff✨ <bold>PROCESAR ARTEFACTO</bold>";
        @Setting private List<String> lore = List.of(
                "&#E6CCFFHaz clic aquí para iniciar",
                "&#E6CCFFel proceso mágico de la estación.",
                "",
                "&#00f5ff► Clic para confirmar"
        );

        public String titulo() { return titulo; }
        public List<String> lore() { return lore; }
    }

    // =========================================
    // 🗂️ NODO: Mensajes Generales
    // =========================================
    @ConfigSerializable
    public static class MensajesNode {
        @Setting private ErroresNode errores = new ErroresNode();
        @Setting private ExitoNode exito = new ExitoNode();

        public ErroresNode errores() { return errores; }
        public ExitoNode exito() { return exito; }
    }

    @ConfigSerializable
    public static class ErroresNode {
        @Setting("sin-permiso") private String sinPermiso = "&#FF3366[!] El Vacío rechaza tu petición (Sin Permisos).";
        @Setting("jugador-offline") private String jugadorOffline = "&#FF3366[!] Ese jugador no se encuentra en este plano.";
        @Setting("item-invalido") private String itemInvalido = "&#FF3366[!] Este artefacto no es compatible o no existe.";
        @Setting("inventario-lleno") private String inventarioLleno = "&#FF3366[!] Tu inventario está lleno. Artefacto caído al suelo.";

        public String sinPermiso() { return sinPermiso; }
        public String jugadorOffline() { return jugadorOffline; }
        public String itemInvalido() { return itemInvalido; }
        public String inventarioLleno() { return inventarioLleno; }
    }

    @ConfigSerializable
    public static class ExitoNode {
        @Setting("recarga-exitosa") private String recargaExitosa = "&#9933FF[✓] <bold>TEXTOS SAGRADOS RENOVADOS:</bold> &#E6CCFFMecánicas de ítems recargadas.";
        public String recargaExitosa() { return recargaExitosa; }
    }

    // =========================================
    // 🗂️ NODO: Comandos
    // =========================================
    @ConfigSerializable
    public static class ComandosNode {
        @Setting("item-otorgado") private String itemOtorgado = "&#00f5ff[📦] Artefacto conjurado y entregado a %player%.";
        @Setting("item-recibido") private String itemRecibido = "&#00f5ff[📦] Has recibido un nuevo Artefacto: &#E6CCFF%item%";

        public String itemOtorgado() { return itemOtorgado; }
        public String itemRecibido() { return itemRecibido; }
    }

    // =========================================
    // 🗂️ NODO: Estaciones (Forja, Desguace, etc)
    // =========================================
    @ConfigSerializable
    public static class EstacionesNode {
        @Setting("mejora-exitosa") private String mejoraExitosa = "&#00f5ff✨ <bold>FORJA EXITOSA:</bold> &#E6CCFFTu artefacto ha ascendido a Nivel %level%.";
        @Setting("mejora-fallida") private String mejoraFallida = "&#8b0000[!] La forja ha fallado. Los materiales se han hecho polvo.";
        @Setting("desguace-exitoso") private String desguaceExitoso = "&#ff00ff[♻] <bold>DESGUACE COMPLETADO:</bold> &#E6CCFFMateria prima recuperada.";

        @Setting("inserta-activo") private String insertaActivo = "&#FF3366[!] Debes colocar un artefacto válido en la ranura izquierda.";
        @Setting("necesitas-polvo") private String necesitasPolvo = "&#FF3366[!] Necesitas Polvo de Mejora en la ranura derecha.";
        @Setting("no-soporta-mejoras") private String noSoportaMejoras = "&#FF3366[!] Este artefacto antiguo no soporta la forja de ascensión.";
        @Setting("mejora-maxima") private String mejoraMaxima = "&#FF3366[!] Este artefacto ya irradiia su máximo poder (Nivel 10).";

        public String mejoraExitosa() { return mejoraExitosa; }
        public String mejoraFallida() { return mejoraFallida; }
        public String desguaceExitoso() { return desguaceExitoso; }
        public String insertaActivo() { return insertaActivo; }
        public String necesitasPolvo() { return necesitasPolvo; }
        public String noSoportaMejoras() { return noSoportaMejoras; }
        public String mejoraMaxima() { return mejoraMaxima; }
    }

    // =========================================
    // 🗂️ NODO: Mochilas y Guardarropa
    // =========================================
    @ConfigSerializable
    public static class MochilasNode {
        @Setting("abriendo-mochila") private String abriendoMochila = "&#9933FFAbriendo Bóveda del Vacío #%id%...";
        @Setting("mochila-bloqueada") private String mochilaBloqueada = "&#FF3366[!] Aún no has desbloqueado esta bóveda.";

        public String abriendoMochila() { return abriendoMochila; }
        public String mochilaBloqueada() { return mochilaBloqueada; }
    }

    // =========================================
    // 🗂️ NODO: Eventos (Crafteos, Minería, Habilidades)
    // =========================================
    @ConfigSerializable
    public static class EventosNode {
        @Setting private CrafteoNode crafteo = new CrafteoNode();
        @Setting private MineriaNode mineria = new MineriaNode(); // 🌟 NUEVO
        @Setting private HabilidadesNode habilidades = new HabilidadesNode(); // 🌟 NUEVO

        public CrafteoNode crafteo() { return crafteo; }
        public MineriaNode mineria() { return mineria; }
        public HabilidadesNode habilidades() { return habilidades; }
    }

    @ConfigSerializable
    public static class CrafteoNode {
        @Setting("ensamblaje-denegado") private String ensamblajeDenegado = "&#FF3366[!] No tienes el nivel de Colección necesario para ensamblar este objeto.";
        public String ensamblajeDenegado() { return ensamblajeDenegado; }
    }

    // 🌟 NUEVO NODO PARA BLOCKBREAKLISTENER
    @ConfigSerializable
    public static class MineriaNode {
        @Setting("enlace-caido") private String enlaceCaido = "&#FF5555[!] Enlace neuronal con el Nexo perdido.";
        @Setting("energia-agotada") private String energiaAgotada = "&#FF5555[!] Energía de Minería agotada.";
        @Setting("oro-sintetico") private String oroSintetico = "&#FFAA00Oro Sintético";
        @Setting("doble-material") private String dobleMaterial = "&#55FF55✨ ¡Doble Drop! &8(&f%suerte%%&8)";
        @Setting("ascenso-cenit-titulo") private String ascensoCenitTitulo = "&#FF55FF¡ASCENSO CÉNIT!";
        @Setting("ascenso-cenit-subtitulo") private String ascensoCenitSubtitulo = "&#FFAA00Nivel %level%";

        public String enlaceCaido() { return enlaceCaido; }
        public String energiaAgotada() { return energiaAgotada; }
        public String oroSintetico() { return oroSintetico; }
        public String dobleMaterial() { return dobleMaterial; }
        public String ascensoCenitTitulo() { return ascensoCenitTitulo; }
        public String ascensoCenitSubtitulo() { return ascensoCenitSubtitulo; }
    }

    // 🌟 NUEVO NODO PARA INTERACTLISTENER
    @ConfigSerializable
    public static class HabilidadesNode {
        @Setting("enfriamiento") private String enfriamiento = "&#FF5555❄ Enfriamiento de Sistema: %time%s";
        @Setting("sincronizando") private String sincronizando = "&#FF5555[!] Sincronizando interfaz neuronal. Espera...";
        @Setting("energia-insuficiente") private String energiaInsuficiente = "&#FF5555⚡ Energía Insuficiente (%cost% requeridos)";
        @Setting("sin-objetivos") private String sinObjetivos = "&#FF5555[!] Sin objetivos hostiles válidos en el rango.";
        @Setting("destino-invalido") private String destinoInvalido = "&#FF5555[!] Destino inválido para salto traslacional.";
        @Setting("habilidad-desplegada") private String habilidadDesplegada = "&#00E5FF✨ Habilidad Desplegada: &#FFFFFF%skill% &#555555(-%cost%⚡)";

        public String enfriamiento() { return enfriamiento; }
        public String sincronizando() { return sincronizando; }
        public String energiaInsuficiente() { return energiaInsuficiente; }
        public String sinObjetivos() { return sinObjetivos; }
        public String destinoInvalido() { return destinoInvalido; }
        public String habilidadDesplegada() { return habilidadDesplegada; }
    }
}