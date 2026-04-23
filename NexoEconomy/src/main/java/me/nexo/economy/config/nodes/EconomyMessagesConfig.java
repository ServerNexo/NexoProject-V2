package me.nexo.economy.config.nodes;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

/**
 * 💰 NexoEconomy - Nodos de Configuración Tipados (Configurate)
 * Arquitectura: Data Transfer Object (DTO) Activo.
 * Nota: No requiere inyección de Guice (@Singleton) ya que es instanciado por reflexión.
 */
@ConfigSerializable
public class EconomyMessagesConfig {

    @Setting private GeneralNode general = new GeneralNode();
    @Setting private ComandosNode comandos = new ComandosNode();
    @Setting private MenusNode menus = new MenusNode();

    public GeneralNode general() { return general; }
    public ComandosNode comandos() { return comandos; }
    public MenusNode menus() { return menus; }

    @ConfigSerializable
    public static class GeneralNode {
        @Setting("moneda-simbolo") private String monedaSimbolo = "⛃";
        @Setting("sin-fondos") private String sinFondos = "&#FF5555[!] Fondos insuficientes en tu cuenta del Nexo.";
        
        public String monedaSimbolo() { return monedaSimbolo; }
        public String sinFondos() { return sinFondos; }
    }

    @ConfigSerializable
    public static class ComandosNode {
        @Setting("no-jugador") private String noJugador = "&#FF5555[!] La consola no posee una cuenta bancaria.";
        @Setting("sin-permiso") private String sinPermiso = "&#FF5555[!] El Vacío rechaza tu petición (Sin Permisos).";
        
        public String noJugador() { return noJugador; }
        public String sinPermiso() { return sinPermiso; }
    }

    @ConfigSerializable
    public static class MenusNode {
        @Setting("bazar-titulo") private String bazarTitulo = "&#FFAA00⚖ <bold>EL BAZAR GLOBAL</bold>";
        @Setting("trade-titulo") private String tradeTitulo = "&#00f5ff🤝 <bold>INTERCAMBIO SEGURO</bold>";
        @Setting("blackmarket-titulo") private String blackmarketTitulo = "&#8b0000🌑 <bold>MERCADO NEGRO</bold>";

        public String bazarTitulo() { return bazarTitulo; }
        public String tradeTitulo() { return tradeTitulo; }
        public String blackmarketTitulo() { return blackmarketTitulo; }
    }
}