package me.nexo.core.config.nodes;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

/**
 * 🏛️ Nexo Network - Configurate Type-Safe Node (Arquitectura Enterprise)
 * Mapea directamente el archivo messages.yml a objetos de memoria RAM seguros.
 * Nota: Como es un nodo de datos, es instanciado por Configurate, no por Guice.
 */
@ConfigSerializable
public class MessagesConfig {

    @Setting
    private Comandos comandos = new Comandos();

    public Comandos comandos() { return comandos; }

    // =========================================
    // NODO: Comandos
    // =========================================
    @ConfigSerializable
    public static class Comandos {
        @Setting
        private NexoCoreNode nexocore = new NexoCoreNode();

        @Setting
        private WebCommandNode web = new WebCommandNode(); // 🌐 El nodo de tu comando web

        public NexoCoreNode nexocore() { return nexocore; }
        public WebCommandNode web() { return web; }
    }

    // =========================================
    // NODO: NexoCore
    // =========================================
    @ConfigSerializable
    public static class NexoCoreNode {
        @Setting private Errores errores = new Errores();
        @Setting private String uso = "&#E6CCFFUso del sistema: &#ff00ff/nexocore <darxp|darcombatexp> <operario> <cantidad>";
        @Setting private Exito exito = new Exito();
        @Setting private Feedback feedback = new Feedback();

        // 💡 @Setting mapea el nombre YAML ("subida-nivel") a la variable Java ("subidaNivel")
        @Setting("subida-nivel")
        private SubidaNivel subidaNivel = new SubidaNivel();

        public Errores errores() { return errores; }
        public String uso() { return uso; }
        public Exito exito() { return exito; }
        public Feedback feedback() { return feedback; }
        public SubidaNivel subidaNivel() { return subidaNivel; }
    }

    // =========================================
    // NODO: Categorías Finales
    // =========================================
    @ConfigSerializable
    public static class Errores {
        @Setting private String offline = "&#8b0000[!] Error: El operario objetivo no está en línea.";
        @Setting private String cargando = "&#8b0000[!] Sincronizando datos con la red. Intente de nuevo en unos segundos...";
        @Setting("formato-invalido")
        private String formatoInvalido = "&#8b0000[!] Error de formato: La cantidad debe ser numérica.";

        public String offline() { return offline; }
        public String cargando() { return cargando; }
        public String formatoInvalido() { return formatoInvalido; }
    }

    @ConfigSerializable
    public static class Exito {
        @Setting("dar-xp")
        private String darXp = "&#00f5ff[✓] Transferencia de %amount% Nexo XP a %target% completada.";
        @Setting("dar-combate-xp")
        private String darCombateXp = "&#00f5ff[✓] Transferencia de %amount% Combate XP a %target% completada.";

        public String darXp() { return darXp; }
        public String darCombateXp() { return darCombateXp; }
    }

    @ConfigSerializable
    public static class Feedback {
        @Setting("recibir-combate-xp")
        private String recibirCombateXp = "&#8b0000⚔ +%amount% XP de Combate &#E6CCFF(%xp%/%xpreq%)";

        public String recibirCombateXp() { return recibirCombateXp; }
    }

    @ConfigSerializable
    public static class SubidaNivel {
        @Setting private NivelData nexo = new NivelData("&#ff00ff<bold>¡NEXO NIVEL %level%!</bold>", "&#00f5ffHas ascendido en la jerarquía del servidor");
        @Setting private NivelData combate = new NivelData("&#8b0000<bold>¡COMBATE NIVEL %level%!</bold>", "&#E6CCFFTus instintos bélicos se agudizan...");

        public NivelData nexo() { return nexo; }
        public NivelData combate() { return combate; }
    }

    @ConfigSerializable
    public static class NivelData {
        @Setting private String titulo;
        @Setting private String subtitulo;

        public NivelData() {} // Constructor requerido por el motor de reflexión de Configurate
        
        public NivelData(String titulo, String subtitulo) { 
            this.titulo = titulo; 
            this.subtitulo = subtitulo; 
        }

        public String titulo() { return titulo; }
        public String subtitulo() { return subtitulo; }
    }

    // =========================================
    // NODO: WebCommand (Textos del Comando Web)
    // =========================================
    @ConfigSerializable
    public static class WebCommandNode {
        @Setting private String exito1 = "&#ff00ff<bold>[Nexo Web]</bold> &#00f5ff¡Tu Clave del Vacío ha sido registrada con éxito!";
        @Setting private String exito2 = "&#E6CCFFYa puedes iniciar sesión en el panel web.";
        @Setting("error-db")
        private String errorDb = "&#8b0000[!] Error: No se encontró tu perfil en la base de datos. ¡Vuelve a entrar al servidor!";
        @Setting("error-critico")
        private String errorCritico = "&#8b0000[!] Ocurrió un error crítico de seguridad al registrar tu clave.";

        public String exito1() { return exito1; }
        public String exito2() { return exito2; }
        public String errorDb() { return errorDb; }
        public String errorCritico() { return errorCritico; }
    }
}