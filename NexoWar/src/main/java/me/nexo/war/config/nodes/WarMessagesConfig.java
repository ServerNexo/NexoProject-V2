package me.nexo.war.config.nodes;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.List;

/**
 * ⚔️ NexoWar - Configurate Type-Safe Node (Arquitectura Enterprise)
 * Archivo consolidado al 100%.
 */
@ConfigSerializable
public class WarMessagesConfig {

    @Setting private ErroresNode errores = new ErroresNode();
    @Setting private ExitoNode exito = new ExitoNode();
    @Setting private ProcesosNode procesos = new ProcesosNode();
    @Setting private AlertasNode alertas = new AlertasNode();
    @Setting private AyudaNode ayuda = new AyudaNode();

    public ErroresNode errores() { return errores; }
    public ExitoNode exito() { return exito; }
    public ProcesosNode procesos() { return procesos; }
    public AlertasNode alertas() { return alertas; }
    public AyudaNode ayuda() { return ayuda; }

    @ConfigSerializable
    public static class ErroresNode {
        @Setting("sin-clan") private String sinClan = "&#8b0000[!] No perteneces a ningún clan.";
        @Setting("rango-insuficiente") private String rangoInsuficiente = "&#8b0000[!] Rango insuficiente. Solo LIDER o OFICIAL.";
        @Setting("servicio-clanes-offline") private String servicioClanesOffline = "&#8b0000[!] El servicio de clanes está fuera de línea.";
        @Setting("sintaxis-challenge") private String sintaxisChallenge = "&#8b0000[!] Uso correcto: /war challenge <tag> <apuesta>";
        @Setting("apuesta-invalida") private String apuestaInvalida = "&#8b0000[!] La apuesta debe ser mayor a 0.";
        @Setting("fondos-insuficientes") private String fondosInsuficientes = "&#8b0000[!] Tu clan no tiene fondos suficientes para esta apuesta.";
        @Setting("auto-ataque") private String autoAtaque = "&#8b0000[!] No puedes declarar la guerra a tu propio clan.";
        @Setting("objetivo-sin-fondos") private String objetivoSinFondos = "&#8b0000[!] El clan objetivo no puede cubrir la apuesta de %apuesta%.";
        @Setting("objetivo-no-encontrado") private String objetivoNoEncontrado = "&#8b0000[!] El clan %tag% no fue encontrado en los registros.";
        @Setting("error-base-datos") private String errorBaseDatos = "&#8b0000[!] Error de conexión con la red táctica.";
        @Setting("sin-contratos") private String sinContratos = "&#8b0000[!] No hay contratos de guerra pendientes para tu clan.";

        // 🌟 Requisitos
        @Setting("sin-esencia-guerra") private String sinEsenciaGuerra = "&#8b0000[!] Tu inventario no cuenta con los catalizadores requeridos.";
        @Setting("requisito-esencia") private String requisitoEsencia = "&#E6CCFFRequerido: &#ff00ff%costo%x Esencia del Vacío &#E6CCFF(Obtenible en Monolitos).";

        // 🌟 Crossplay Matchmaking
        @Setting("escuadron-lleno") private String escuadronLleno = "&#FF5555🛡️ Protocolo Nexo: El escuadrón de %plataforma% de tu Sindicato ya está lleno.";
        @Setting("combate-plataformas") private String combatePlataformas = "&#FF5555🛡️ Protocolo Nexo: Combate prohibido entre diferentes plataformas.";

        public String sinClan() { return sinClan; }
        public String rangoInsuficiente() { return rangoInsuficiente; }
        public String servicioClanesOffline() { return servicioClanesOffline; }
        public String sintaxisChallenge() { return sintaxisChallenge; }
        public String apuestaInvalida() { return apuestaInvalida; }
        public String fondosInsuficientes() { return fondosInsuficientes; }
        public String autoAtaque() { return autoAtaque; }
        public String objetivoSinFondos() { return objetivoSinFondos; }
        public String objetivoNoEncontrado() { return objetivoNoEncontrado; }
        public String errorBaseDatos() { return errorBaseDatos; }
        public String sinContratos() { return sinContratos; }
        public String sinEsenciaGuerra() { return sinEsenciaGuerra; }
        public String requisitoEsencia() { return requisitoEsencia; }
        public String escuadronLleno() { return escuadronLleno; }
        public String combatePlataformas() { return combatePlataformas; }
    }

    @ConfigSerializable
    public static class ProcesosNode {
        @Setting("escaneando-red") private String escaneandoRed = "&#00f5ff[⚡] Escaneando la red de clanes...";
        @Setting("iniciando-despliegue") private String iniciandoDespliegue = "&#ff00ff[⚡] Contrato aceptado. Iniciando despliegue militar...";

        public String escaneandoRed() { return escaneandoRed; }
        public String iniciandoDespliegue() { return iniciandoDespliegue; }
    }

    @ConfigSerializable
    public static class ExitoNode {
        @Setting("contrato-emitido") private String contratoEmitido = "&#00f5ff[✓] Contrato de guerra emitido contra %defensor%.";
        @Setting("baja-confirmada") private String bajaConfirmada = "&#ff00ff⚔ <bold>BAJA CONFIRMADA:</bold> &#E6CCFFProgreso Táctico: &#00f5ff%actual% / %meta%";

        public String contratoEmitido() { return contratoEmitido; }
        public String bajaConfirmada() { return bajaConfirmada; }
    }

    @ConfigSerializable
    public static class AlertasNode {
        @Setting("declaracion-guerra") private List<String> declaracionGuerra = List.of(
                "&c&m==================================",
                "&e&l¡ALERTA DE GUERRA!",
                "&7El clan &c%atacante% &7ha emitido un contrato.",
                "&7Apuesta en juego: &e%apuesta%",
                "&7Usa &a/war accept &7para ir al campo de batalla.",
                "&c&m=================================="
        );
        @Setting("pacto-iniciado") private List<String> pactoIniciado = List.of(
                "&#ff00ff&m==================================",
                "&#ff00ff⚔ <bold>PACTO DE SANGRE INICIADO</bold> ⚔",
                "&#E6CCFFEl clan &#8b0000%atacante% &#E6CCFFha desafiado a &#8b0000%defensor%&#E6CCFF.",
                "&#E6CCFFBotín acumulado: &#00f5ff%total% Monedas.",
                "&#E6CCFFPeríodo de gracia de 5 minutos activo.",
                "&#ff00ff&m=================================="
        );
        @Setting("guerra-activa") private List<String> guerraActiva = List.of(
                "&#8b0000<bold>¡EL PERÍODO DE GRACIA HA TERMINADO!</bold>",
                "&#E6CCFFEl PvP letal está habilitado. El primer clan en alcanzar &#ff00ff%kills% bajas &#E6CCFFreclamará el botín."
        );
        @Setting("victoria") private List<String> victoria = List.of(
                "&#00f5ff&m==================================",
                "&#00f5ff🏆 <bold>VICTORIA DECISIVA</bold> 🏆",
                "&#E6CCFFEl clan &#ff00ff%ganador% &#E6CCFFha dominado el campo de batalla.",
                "&#E6CCFFBotín transferido: &#00f5ff%premio% Monedas.",
                "&#00f5ff&m=================================="
        );

        public List<String> declaracionGuerra() { return declaracionGuerra; }
        public List<String> pactoIniciado() { return pactoIniciado; }
        public List<String> guerraActiva() { return guerraActiva; }
        public List<String> victoria() { return victoria; }
    }

    @ConfigSerializable
    public static class AyudaNode {
        @Setting("comando-war") private List<String> comandoWar = List.of(
                "&#ff00ff&m==================================",
                "&#00f5ff&lSISTEMA DE GUERRA",
                "&8» &e/war challenge <tag> <apuesta>",
                "&8» &a/war accept",
                "&#ff00ff&m=================================="
        );
        public List<String> comandoWar() { return comandoWar; }
    }
}