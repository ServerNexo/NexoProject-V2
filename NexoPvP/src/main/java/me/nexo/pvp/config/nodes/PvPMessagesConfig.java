package me.nexo.pvp.config.nodes;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.List;

/**
 * 🏛️ Nexo Network - Configurate Type-Safe Node (PvP)
 * Esto convierte tu messages.yml de NexoPvP directamente en objetos de Java.
 */
@ConfigSerializable
public class PvPMessagesConfig {

    @Setting private MenusNode menus = new MenusNode();
    @Setting private MensajesNode mensajes = new MensajesNode();

    public MenusNode menus() { return menus; }
    public MensajesNode mensajes() { return mensajes; }

    // =========================================
    // NODO: Menus
    // =========================================
    @ConfigSerializable
    public static class MenusNode {
        @Setting private TemploNode templo = new TemploNode();
        public TemploNode templo() { return templo; }
    }

    @ConfigSerializable
    public static class TemploNode {
        @Setting private String titulo = "&#ff00ff✧ &#00f5ffTemplo del Vacío";
        @Setting private ItemsNode items = new ItemsNode();

        public String titulo() { return titulo; }
        public ItemsNode items() { return items; }
    }

    @ConfigSerializable
    public static class ItemsNode {
        @Setting("bendicion-menor")
        private ItemData bendicionMenor = new ItemData("&#00f5ffBendición Menor", List.of("&#E6CCFFProtección básica"));

        @Setting("bendicion-premium")
        private ItemData bendicionPremium = new ItemData("&#ff00ffBendición Premium", List.of("&#E6CCFFProtección total"));

        public ItemData bendicionMenor() { return bendicionMenor; }
        public ItemData bendicionPremium() { return bendicionPremium; }
    }

    @ConfigSerializable
    public static class ItemData {
        @Setting private String nombre;
        @Setting private List<String> lore;

        public ItemData() {} // Obligatorio para Configurate
        public ItemData(String nombre, List<String> lore) { this.nombre = nombre; this.lore = lore; }

        public String nombre() { return nombre; }
        public List<String> lore() { return lore; }
    }

    // =========================================
    // NODO: Mensajes
    // =========================================
    @ConfigSerializable
    public static class MensajesNode {
        @Setting private ErroresNode errores = new ErroresNode();
        @Setting private ExitoNode exito = new ExitoNode();
        @Setting private PenalizacionesNode penalizaciones = new PenalizacionesNode();

        // ⚔️ NODO DE COMBATE
        @Setting private PvpNode pvp = new PvpNode();

        public ErroresNode errores() { return errores; }
        public ExitoNode exito() { return exito; }
        public PenalizacionesNode penalizaciones() { return penalizaciones; }

        // ⚔️ GETTER DE COMBATE
        public PvpNode pvp() { return pvp; }
    }

    @ConfigSerializable
    public static class ErroresNode {
        @Setting("bendicion-activa") private String bendicionActiva = "&#8b0000[!] Ya tienes una bendición activa.";
        @Setting("sin-monedas") private String sinMonedas = "&#8b0000[!] No tienes suficientes monedas.";
        @Setting("sin-gemas") private String sinGemas = "&#8b0000[!] No tienes suficientes gemas.";
        @Setting("solo-jugadores") private String soloJugadores = "&#8b0000[!] Este comando solo puede ser usado por jugadores.";

        public String bendicionActiva() { return bendicionActiva; }
        public String sinMonedas() { return sinMonedas; }
        public String sinGemas() { return sinGemas; }
        public String soloJugadores() { return soloJugadores; }
    }

    @ConfigSerializable
    public static class ExitoNode {
        @Setting("compra-menor") private String compraMenor = "&#00f5ff[✓] Has adquirido la Bendición Menor.";
        @Setting("compra-premium") private String compraPremium = "&#ff00ff[✓] Has adquirido la Bendición Premium.";

        public String compraMenor() { return compraMenor; }
        public String compraPremium() { return compraPremium; }
    }

    @ConfigSerializable
    public static class PenalizacionesNode {
        @Setting("muerte-protegida") private String muerteProtegida = "&#00f5ff✧ Tu bendición te ha protegido de la penalización de muerte.";
        @Setting("cobro-resurreccion") private String cobroResurreccion = "&#8b0000[!] Has pagado %amount% monedas al morir.";
        @Setting("perdida-progreso") private String perdidaProgreso = "&#8b0000[!] Has perdido parte de tu progreso por morir.";
        @Setting("consejo-bendicion") private String consejoBendicion = "&#E6CCFFConsejo: Usa una Esencia del Vacío en el Templo para protegerte.";

        public String muerteProtegida() { return muerteProtegida; }
        public String cobroResurreccion() { return cobroResurreccion; }
        public String perdidaProgreso() { return perdidaProgreso; }
        public String consejoBendicion() { return consejoBendicion; }
    }

    // ⚔️ CLASE EXPANDIDA: Textos para todo el módulo PvP
    @ConfigSerializable
    public static class PvpNode {
        // PvPManager
        @Setting("error-en-combate") private String errorEnCombate = "&#8b0000[!] Error de Seguridad: No puedes desactivar la hostilidad con un enlace de combate activo.";
        @Setting("protocolo-paz") private String protocoloPaz = "&#00f5ff[✓] <bold>PROTOCOLO DE PAZ:</bold> &#E6CCFFHostilidad desactivada. Escudos neuronales activos.";
        @Setting("protocolo-guerra") private String protocoloGuerra = "&#8b0000[!] <bold>PROTOCOLO DE GUERRA:</bold> &#E6CCFFHostilidad activada. Sistemas de armamento en línea.";
        @Setting("alerta-combate") private String alertaCombate = "&#8b0000<bold>¡ALERTA DE COMBATE!</bold> &#E6CCFFEnlace táctico detectado (15s). No te desconectes.";
        @Setting("fin-combate") private String finCombate = "&#00f5ff[✓] Enlace de combate finalizado. Sistemas estabilizados.";

        // PvPListener (Muerte, Cazarrecompensas, Desconexiones)
        @Setting("bloqueo-armamento") private String bloqueoArmamento = "&#8b0000[!] Bloqueo de Armamento: &#E6CCFFEl objetivo se encuentra en una zona neutral.";
        @Setting("objetivo-neutralizado") private String objetivoNeutralizado = "&#ff00ff⚔ <bold>OBJETIVO NEUTRALIZADO:</bold> &#E6CCFF%victima% &#00f5ff(+1 Honor)";
        @Setting("cazarrecompensas-global") private String cazarrecompensasGlobal = "&#00f5ff<bold>[CAZARRECOMPENSAS]</bold> &#E6CCFF%asesino% &#E6CCFFha cobrado el contrato por la cabeza de &#8b0000%victima%&#E6CCFF!";
        @Setting("bounty-reclamado") private String bountyReclamado = "&#00f5ff[💎] <bold>Bounty Reclamado:</bold> &#E6CCFFTransferencia de +5 Honor y recurso primario completada.";
        @Setting("racha-tres-global") private String rachaTresGlobal = "&#8b0000<bold>[OBJETIVO PRIORITARIO]</bold> &#E6CCFF%asesino% &#E6CCFFestá en racha letal (3 Kills). ¡Contrato de caza emitido!";
        @Setting("racha-mayor-global") private String rachaMayorGlobal = "&#8b0000<bold>[AMENAZA NIVEL OMEGA]</bold> &#E6CCFF%asesino% &#E6CCFFha alcanzado %kills% Kills consecutivas!";
        @Setting("desconexion-cobarde") private String desconexionCobarde = "&#8b0000☠ <bold>DESCONEXIÓN COBARDE:</bold> &#E6CCFF%jugador% &#E6CCFFevadió el combate y sus sistemas fueron purgados.";

        // 🛡️ PasivasManager & PasivasListener
        @Setting("escudo-agotado") private String escudoAgotado = "&#8b0000[!] Escudo de Emergencia Agotado: &#E6CCFFTu inmunidad táctica se ha desvanecido.";
        @Setting("escudo-emergencia-titulo") private String escudoEmergenciaTitulo = "&#8b0000<bold>¡ESCUDO DE EMERGENCIA!</bold>";
        @Setting("escudo-emergencia-sub") private String escudoEmergenciaSub = "&#E6CCFFDaño letal anulado. Sistemas en enfriamiento.";
        @Setting("pesca-cuantica") private String pescaCuantica = "&#00f5ff[✓] <bold>EXTRACCIÓN DUPLICADA:</bold> &#E6CCFFRedimensionamiento cuántico aplicado.";
        @Setting("retencion-energia") private String retencionEnergia = "&#ff00ff✨ <bold>RETENCIÓN DE ENERGÍA:</bold> &#E6CCFFCosto de ensamblaje reintegrado.";

        // 🛡️ Clases de Armadura (ArmorClassListener)
        @Setting("set-asesino-activo") private String setAsesinoActivo = "&#8b0000[☠] Set de Sombra Activo: Velocidad Máxima, Salud Crítica.";
        @Setting("set-inquisidor-activo") private String setInquisidorActivo = "&#ff00ff[✧] Set de Inquisidor Activo: Canalización de Maná amplificada.";

        // 🎯 Campo de Entrenamiento (TrainingStationListener)
        @Setting("entrenamiento-maximo") private String entrenamientoMaximo = "&#8b0000[!] Ya eres muy avanzado (Nivel %nivel%+). ¡Sal al mundo real!";
        @Setting("entrenamiento-xp") private String entrenamientoXp = "&#ff00ff%icon% Entrenamiento: &#00f5ff+%xp% XP";

        public String errorEnCombate() { return errorEnCombate; }
        public String protocoloPaz() { return protocoloPaz; }
        public String protocoloGuerra() { return protocoloGuerra; }
        public String alertaCombate() { return alertaCombate; }
        public String finCombate() { return finCombate; }

        public String bloqueoArmamento() { return bloqueoArmamento; }
        public String objetivoNeutralizado() { return objetivoNeutralizado; }
        public String cazarrecompensasGlobal() { return cazarrecompensasGlobal; }
        public String bountyReclamado() { return bountyReclamado; }
        public String rachaTresGlobal() { return rachaTresGlobal; }
        public String rachaMayorGlobal() { return rachaMayorGlobal; }
        public String desconexionCobarde() { return desconexionCobarde; }

        public String escudoAgotado() { return escudoAgotado; }
        public String escudoEmergenciaTitulo() { return escudoEmergenciaTitulo; }
        public String escudoEmergenciaSub() { return escudoEmergenciaSub; }
        public String pescaCuantica() { return pescaCuantica; }
        public String retencionEnergia() { return retencionEnergia; }

        public String setAsesinoActivo() { return setAsesinoActivo; }
        public String setInquisidorActivo() { return setInquisidorActivo; }

        public String entrenamientoMaximo() { return entrenamientoMaximo; }
        public String entrenamientoXp() { return entrenamientoXp; }
    }
}