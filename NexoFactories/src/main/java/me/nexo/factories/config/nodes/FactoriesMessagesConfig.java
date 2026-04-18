package me.nexo.factories.config.nodes;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;
import java.util.List;

/**
 * 🏭 NexoFactories - Nodos de Configuración Tipados (Configurate)
 */
@ConfigSerializable
public class FactoriesMessagesConfig {

    @Setting private MenusNode menus = new MenusNode();
    @Setting private EventosNode eventos = new EventosNode();
    @Setting private ComandosNode comandos = new ComandosNode();

    public MenusNode menus() { return menus; }
    public EventosNode eventos() { return eventos; }
    public ComandosNode comandos() { return comandos; }

    // =========================================
    // 🗂️ NODO: Menus (Interfaces)
    // =========================================
    @ConfigSerializable
    public static class MenusNode {
        @Setting private FactoryMenuNode factory = new FactoryMenuNode();
        @Setting private LogicMenuNode logic = new LogicMenuNode();

        public FactoryMenuNode factory() { return factory; }
        public LogicMenuNode logic() { return logic; }
    }

    @ConfigSerializable
    public static class FactoryMenuNode {
        @Setting private String titulo = "&#ff00ff🏭 <bold>FÁBRICA: %type%</bold>";
        public String titulo() { return titulo; }
    }

    @ConfigSerializable
    public static class LogicMenuNode {
        @Setting private String titulo = "&#FF5555⚙ <bold>COMPILADOR LÓGICO</bold>";
        public String titulo() { return titulo; }
    }

    // =========================================
    // 🗂️ NODO: Eventos
    // =========================================
    @ConfigSerializable
    public static class EventosNode {
        @Setting("extraccion-exitosa") private String extraccionExitosa = "&#55FF55[✓] Has extraído %amount% unidades de la fábrica.";
        @Setting("script-compilado") private String scriptCompilado = "&#55FF55[✓] Script lógico compilado e inyectado en la máquina.";

        public String extraccionExitosa() { return extraccionExitosa; }
        public String scriptCompilado() { return scriptCompilado; }
    }

    // =========================================
    // 🗂️ NODO: Comandos
    // =========================================
    @ConfigSerializable
    public static class ComandosNode {
        @Setting("no-jugador") private String noJugador = "&#FF5555[!] La consola no puede instanciar fábricas.";
        public String noJugador() { return noJugador; }
    }
}