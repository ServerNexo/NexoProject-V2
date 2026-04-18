package me.nexo.colecciones.config.nodes;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

/**
 * 📚 NexoColecciones - Nodos de Configuración Tipados (Configurate)
 */
@ConfigSerializable
public class ColeccionesMessagesConfig {

    @Setting private MenusNode menus = new MenusNode();
    public MenusNode menus() { return menus; }

    @ConfigSerializable
    public static class MenusNode {
        @Setting("colecciones-titulo") private String coleccionesTitulo = "&#FFAA00📚 <bold>TUS COLECCIONES</bold>";
        @Setting("slayers-titulo") private String slayersTitulo = "&#FF5555⚔ <bold>CONTRATOS SLAYER</bold>";

        public String coleccionesTitulo() { return coleccionesTitulo; }
        public String slayersTitulo() { return slayersTitulo; }
    }
}