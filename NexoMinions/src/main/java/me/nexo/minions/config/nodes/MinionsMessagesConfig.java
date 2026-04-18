package me.nexo.minions.config.nodes;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.List;

/**
 * 🤖 NexoMinions - Configurate Type-Safe Node (Arquitectura Enterprise)
 */
@ConfigSerializable
public class MinionsMessagesConfig {

    @Setting private ManagerNode manager = new ManagerNode();
    @Setting private MenuNode menu = new MenuNode();
    @Setting private ComandosNode comandos = new ComandosNode();

    public ManagerNode manager() { return manager; }
    public MenuNode menu() { return menu; }
    public ComandosNode comandos() { return comandos; }

    // =========================================
    // 🗂️ NODO: Manager (Textos Generales, Errores y Alertas)
    // =========================================
    @ConfigSerializable
    public static class ManagerNode {
        @Setting("iniciando-sistemas") private String iniciandoSistemas = "&#ff00ffIniciando Sistemas...";
        @Setting("extraccion-remota") private String extraccionRemota = "&#00f5ff[📦] Extracción Remota: &#E6CCFFHas recuperado &#ff00ff%items% ítems &#E6CCFFdel depósito del operario.";
        @Setting("desmantelamiento-exitoso") private String desmantelamientoExitoso = "&#00f5ff[✓] <bold>PROTOCOLO DE DESMANTELAMIENTO:</bold> &#E6CCFFOperario recogido con éxito. &#00f5ff(%placed%/%max%)";
        @Setting("alerta-desmantelamiento-admin") private String alertaDesmantelamientoAdmin = "&#8b0000[!] Alerta de Red: &#E6CCFFUn administrador corporativo ha desmantelado uno de tus operarios.";
        @Setting("desmantelamiento-admin") private String desmantelamientoAdmin = "&#00f5ff[✓] Operario desmantelado. Cuota operativa restaurada al propietario original.";
        @Setting("propietario-offline") private String propietarioOffline = "&#8b0000[!] Advertencia del Sistema: &#E6CCFFEl propietario de la unidad está desconectado. Sincronización de cuota pospuesta.";

        // 🌟 Agregados del Holograma
        @Setting("holograma-saciado") private String hologramaSaciado = "&#FF3366<bold>¡ENTIDAD SACIADA!</bold>\n&#E6CCFFMateria: &#CC66FF%items% / %max%";
        @Setting("holograma-normal") private String hologramaNormal = "&#9933FF<bold>Esclavo %name%</bold> &#E6CCFF(Nv. %tier%)\n&#E6CCFFMateria: &#CC66FF%items% / %max%";

        // 🌟 Agregados del InteractListener
        @Setting("interactuar-ajeno") private String interactuarAjeno = "&#FF3366[!] Herejía: &#E6CCFFEste esclavo obedece únicamente la voluntad de su Maestro.";
        @Setting("desterrar-ajeno") private String desterrarAjeno = "&#FF3366[!] Herejía: &#E6CCFFNo puedes desterrar al esclavo de otro Maestro.";

        // 🌟 Agregados del Listener Principal (Colocación y Mejoras)
        @Setting("limite-alcanzado") private String limiteAlcanzado = "&#FF3366[!] Límite de Almas Alcanzado: &#E6CCFFMáximo de %max% esclavos permitidos.";
        @Setting("esclavo-conjurado") private String esclavoConjurado = "&#9933FF[✓] <bold>ESCLAVO CONJURADO:</bold> &#E6CCFFUnidad %type% atada al mundo terrenal. &#CC66FF(%placed%/%max%)";
        @Setting("sello-corrupto") private String selloCorrupto = "&#FF3366[!] Fallo de Invocación: &#E6CCFFEl sello de este esclavo está corrupto o es incompatible.";
        @Setting("desestabilizar-ajeno") private String desestabilizarAjeno = "&#FF3366[!] Herejía: &#E6CCFFNo puedes desestabilizar el ritual de un esclavo ajeno.";
        @Setting("mejora-como-bloque") private String mejoraComoBloque = "&#FF3366[!] Herejía: &#E6CCFFEste sello arcano solo puede ser depositado dentro de un Esclavo.";
        @Setting("mejora-como-liquido") private String mejoraComoLiquido = "&#FF3366[!] Herejía: &#E6CCFFEsta materia inestable pertenece a las entidades del vacío.";
        @Setting("dominio-ajeno") private String dominioAjeno = "&#FF3366[!] Herejía: &#E6CCFFEste dominio pertenece a otra entidad.";

        // 🌟 Agregados del Menu Principal
        @Setting("mejora-invalida") private String mejoraInvalida = "&#FF3366[!] Herejía: &#E6CCFFEse objeto no es un sello de mejora compatible.";
        @Setting("ranuras-llenas") private String ranurasLlenas = "&#FF3366[!] Las ranuras de mejora están llenas.";
        @Setting("fauces-vacias") private String faucesVacias = "&#FF3366[!] Herejía: &#E6CCFFLas fauces de la criatura están vacías.";
        @Setting("conocimiento-arcano") private String conocimientoArcano = "&#9933FF✨ Conocimiento Arcano: +%xp% XP en %skill%";
        @Setting("tributo-cosechado") private String tributoCosechado = "&#CC66FF[✓] <bold>TRIBUTO COSECHADO:</bold> &#E6CCFFHas reclamado las ofrendas%compactadas%.";
        @Setting("texto-compactadas") private String textoCompactadas = " &#9933FF(Compactadas)";
        @Setting("error-arcano") private String errorArcano = "&#FF3366[!] Error Arcano: &#E6CCFFEl ritual no está en los textos sagrados.";
        @Setting("ritual-fallido") private String ritualFallido = "&#FF3366[!] Ritual Fallido: &#E6CCFFNo posees los sacrificios necesarios para la ascensión.";
        @Setting("ritual-completado") private String ritualCompletado = "&#9933FF[✓] <bold>RITUAL COMPLETADO:</bold> &#E6CCFFEl esclavo ha ascendido a Nivel %tier%.";


        // --- GETTERS ---
        public String iniciandoSistemas() { return iniciandoSistemas; }
        public String extraccionRemota() { return extraccionRemota; }
        public String desmantelamientoExitoso() { return desmantelamientoExitoso; }
        public String alertaDesmantelamientoAdmin() { return alertaDesmantelamientoAdmin; }
        public String desmantelamientoAdmin() { return desmantelamientoAdmin; }
        public String propietarioOffline() { return propietarioOffline; }

        public String hologramaSaciado() { return hologramaSaciado; }
        public String hologramaNormal() { return hologramaNormal; }
        public String interactuarAjeno() { return interactuarAjeno; }
        public String desterrarAjeno() { return desterrarAjeno; }

        public String limiteAlcanzado() { return limiteAlcanzado; }
        public String esclavoConjurado() { return esclavoConjurado; }
        public String selloCorrupto() { return selloCorrupto; }
        public String desestabilizarAjeno() { return desestabilizarAjeno; }
        public String mejoraComoBloque() { return mejoraComoBloque; }
        public String mejoraComoLiquido() { return mejoraComoLiquido; }
        public String dominioAjeno() { return dominioAjeno; }

        public String mejoraInvalida() { return mejoraInvalida; }
        public String ranurasLlenas() { return ranurasLlenas; }
        public String faucesVacias() { return faucesVacias; }
        public String conocimientoArcano() { return conocimientoArcano; }
        public String tributoCosechado() { return tributoCosechado; }
        public String textoCompactadas() { return textoCompactadas; }
        public String errorArcano() { return errorArcano; }
        public String ritualFallido() { return ritualFallido; }
        public String ritualCompletado() { return ritualCompletado; }
    }

    // =========================================
    // 🗂️ NODO: Comandos
    // =========================================
    @ConfigSerializable
    public static class ComandosNode {
        @Setting("sin-permiso") private String sinPermiso = "&#FF3366[!] Herejía: &#E6CCFFNo posees el poder para invocar esclavos del vacío.";
        @Setting("uso-correcto") private String usoCorrecto = "&#CC66FFUso correcto: &#E6CCFF/minion give <Jugador> <Tipo> [Nivel]";
        @Setting("reload-exito") private String reloadExito = "&#9933FF[✓] <bold>TEXTOS SAGRADOS RENOVADOS:</bold> &#E6CCFFConfiguraciones de esclavos recargadas con éxito.";
        @Setting("jugador-offline") private String jugadorOffline = "&#FF3366[!] Error: &#E6CCFFEsa alma no se encuentra en este reino.";
        @Setting("tipo-invalido") private String tipoInvalido = "&#FF3366[!] Identidad de Esclavo inválida. &#E6CCFF(Ej: WHEAT, ORE_DIAMOND)";
        @Setting("nivel-invalido") private String nivelInvalido = "&#FF3366[!] El Nivel de Vínculo debe estar entre 1 y 12.";
        @Setting("sello-no-existe") private String selloNoExiste = "&#FF3366[!] Error Arcano: &#E6CCFFEl sello '%id%' no existe en los registros de Nexo.";
        @Setting("materia-vacia") private String materiaVacia = "&#FF3366[!] Fallo de Invocación: &#E6CCFFEl ensamblador generó materia vacía (AIR).";
        @Setting("fallo-nbt") private String falloNbt = "&#FF3366[!] Fallo de Escritura NBT: El material base no acepta inyección de datos.";
        @Setting("invocacion-aprobada") private String invocacionAprobada = "&#9933FF[📦] Invocación Aprobada: &#E6CCFFHas conjurado un %type% Nivel %tier% para %target%.";
        @Setting("pacto-forjado") private String pactoForjado = "&#CC66FF[✓] <bold>PACTO FORJADO:</bold> &#E6CCFFUn nuevo esclavo del vacío ha sido encadenado a tu inventario.";

        @Setting("item-nombre") private String itemNombre = "&#9933FF⭐ <bold>%type%</bold> &#E6CCFF(Nv. %tier%)";
        @Setting("item-lore") private List<String> itemLore = List.of(
                "&#E6CCFFUn alma encadenada a este sello,",
                "&#E6CCFFlista para servir en tu dominio.",
                " ",
                "&#CC66FF► Clic derecho en el suelo para invocar al Esclavo"
        );

        public String sinPermiso() { return sinPermiso; }
        public String usoCorrecto() { return usoCorrecto; }
        public String reloadExito() { return reloadExito; }
        public String jugadorOffline() { return jugadorOffline; }
        public String tipoInvalido() { return tipoInvalido; }
        public String nivelInvalido() { return nivelInvalido; }
        public String selloNoExiste() { return selloNoExiste; }
        public String materiaVacia() { return materiaVacia; }
        public String falloNbt() { return falloNbt; }
        public String invocacionAprobada() { return invocacionAprobada; }
        public String pactoForjado() { return pactoForjado; }
        public String itemNombre() { return itemNombre; }
        public List<String> itemLore() { return itemLore; }
    }

    // =========================================
    // 🗂️ NODO: Menu
    // =========================================
    @ConfigSerializable
    public static class MenuNode {
        @Setting private String titulo = "&#E6CCFF<bold>»</bold> &#ff00ffSello del Esclavo";
        @Setting("iconos-guia") private IconosGuiaNode iconosGuia = new IconosGuiaNode();
        @Setting private StatsNode stats = new StatsNode();
        @Setting private EvolucionNode evolucion = new EvolucionNode();
        @Setting private CosecharNode cosechar = new CosecharNode();
        @Setting private DesterrarNode desterrar = new DesterrarNode();

        public String titulo() { return titulo; }
        public IconosGuiaNode iconosGuia() { return iconosGuia; }
        public StatsNode stats() { return stats; }
        public EvolucionNode evolucion() { return evolucion; }
        public CosecharNode cosechar() { return cosechar; }
        public DesterrarNode desterrar() { return desterrar; }
    }

    @ConfigSerializable
    public static class IconosGuiaNode {
        @Setting private MenuItem combustible = new MenuItem("&#8b0000🔥 Llama Abismal", List.of("Sacrifica combustible puro aquí", "para acelerar el tormento del esclavo.", "&#ff00ff⬇ Deposita la ofrenda ⬇"));
        @Setting private MenuItem compresion = new MenuItem("&#ff00ff📦 Runas de Compresión", List.of("Inscribe runas antiguas para", "fusionar la materia en su forma pura.", "&#ff00ff⬇ Deposita la runa ⬇"));
        @Setting private MenuItem almacenamiento = new MenuItem("&#ff00ff🧰 Fauces Insaciables", List.of("Otorga cofres y reliquias para", "expandir el estómago de esta criatura.", "&#ff00ff⬇ Deposita la mejora ⬇"));
        @Setting private MenuItem vinculo = new MenuItem("&#ff00ff🔄 Vínculo Umbrío", List.of("Forja un pacto de sangre para", "drenar la materia a tus propios cofres.", "&#ff00ff⬇ Deposita el vínculo ⬇"));

        public MenuItem combustible() { return combustible; }
        public MenuItem compresion() { return compresion; }
        public MenuItem almacenamiento() { return almacenamiento; }
        public MenuItem vinculo() { return vinculo; }
    }

    @ConfigSerializable
    public static class StatsNode {
        @Setting private String titulo = "&#ff00ff⭐ <bold>%type%</bold> &#E6CCFF(Nv. %tier%)";
        @Setting private StatsLoreNode lore = new StatsLoreNode();
        public String titulo() { return titulo; }
        public StatsLoreNode lore() { return lore; }
    }

    @ConfigSerializable
    public static class StatsLoreNode {
        @Setting("materia-devorada") private String materiaDevorada = "&#E6CCFFMateria Devorada: &#00f5ff%items% uds";
        @Setting private String eficiencia = "&#E6CCFFAgonía (Eficiencia): ";
        @Setting("eficiencia-activa") private String eficienciaActiva = "&#8b0000⚡ +%speed%%";
        @Setting("eficiencia-base") private String eficienciaBase = "&#E6CCFFLetargo (Base)";
        @Setting("sello-amalgama") private String selloAmalgama = "&#00f5ff📦 Sello de Amalgama [ACTIVO]";
        @Setting("nexo-logistico") private String nexoLogistico = "&#00f5ff🔄 Nexo Logístico [CONECTADO]";

        public String materiaDevorada() { return materiaDevorada; }
        public String eficiencia() { return eficiencia; }
        public String eficienciaActiva() { return eficienciaActiva; }
        public String eficienciaBase() { return eficienciaBase; }
        public String selloAmalgama() { return selloAmalgama; }
        public String nexoLogistico() { return nexoLogistico; }
    }

    @ConfigSerializable
    public static class EvolucionNode {
        @Setting("max-nivel") private String maxNivel = "&#ff00ff✨ <bold>CÚSPIDE DEL ABISMO ALCANZADA</bold>";
        @Setting private String titulo = "&#00f5ff⬆ <bold>ASCENDER AL VACÍO A NV. %level%</bold>";
        @Setting private List<String> lore = List.of("&#E6CCFFRealiza el ritual para empoderar a la entidad.", " ", "&#ff00ffTributo Requerido:");
        @Setting("costo-ritual") private String costoRitual = "&#8b0000▶ %amount%x %item%";
        @Setting("error-ritual") private String errorRitual = "&#8b0000[!] Ritual no detectado en tiers.yml";

        public String maxNivel() { return maxNivel; }
        public String titulo() { return titulo; }
        public List<String> lore() { return lore; }
        public String costoRitual() { return costoRitual; }
        public String errorRitual() { return errorRitual; }
    }

    @ConfigSerializable
    public static class CosecharNode {
        @Setting private String titulo = "&#00f5ff📦 <bold>COSECHAR TRIBUTO</bold>";
        @Setting private List<String> lore = List.of(
                "&#E6CCFFReclama toda la materia que el esclavo ha juntado.",
                " ",
                "&#E6CCFFOfrendas Listas: &#00f5ff%items% uds",
                "&#E6CCFFConocimiento Arcano (%skill%): &#ff00ff+%xp% XP",
                " ",
                "&#00f5ff► ¡Clic para reclamar tu ofrenda!"
        );
        public String titulo() { return titulo; }
        public List<String> lore() { return lore; }
    }

    @ConfigSerializable
    public static class DesterrarNode {
        @Setting private String titulo = "&#8b0000🧨 <bold>DESTERRAR ESCLAVO</bold>";
        public String titulo() { return titulo; }
    }

    // 📦 Estructura base para Items de Menú
    @ConfigSerializable
    public static class MenuItem {
        @Setting private String titulo;
        @Setting private List<String> lore;

        public MenuItem() {}
        public MenuItem(String titulo, List<String> lore) {
            this.titulo = titulo;
            this.lore = lore;
        }

        public String titulo() { return titulo; }
        public List<String> lore() { return lore; }
    }
}