package me.nexo.clans.config.nodes;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

/**
 * 👥 NexoClans - Nodos de Configuración Tipados (Configurate)
 * Estructura de datos limpia, Type-Safe y lista para MiniMessage.
 */
@ConfigSerializable
public class ClansMessagesConfig {

    @Setting private MenusNode menus = new MenusNode();
    
    public MenusNode menus() { 
        return menus; 
    }

    // =========================================
    // NODO: Menus
    // =========================================
    @ConfigSerializable
    public static class MenusNode {
        
        @Setting("clanes-titulo") 
        private String clanesTitulo = "&#FFAA00🛡️ <bold>TU CLAN</bold>";
        
        public String clanesTitulo() { 
            return clanesTitulo; 
        }
    }
}