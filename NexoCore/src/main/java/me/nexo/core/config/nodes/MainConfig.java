package me.nexo.core.config.nodes;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.HashMap;
import java.util.Map;

/**
 * 🏛️ Nexo Network - Configurate Type-Safe Node (Arquitectura Enterprise)
 * Mapea directamente el archivo config.yml (Base de datos, Pesca, etc.) a la memoria RAM.
 */
@ConfigSerializable
public class MainConfig {

    @Setting
    private DatabaseNode database = new DatabaseNode();

    @Setting
    private PescaNode pesca = new PescaNode();

    public DatabaseNode database() { return database; }
    public PescaNode pesca() { return pesca; }

    // =========================================
    // 🗄️ NODO: Base de Datos (Supabase)
    // =========================================
    @ConfigSerializable
    public static class DatabaseNode {
        // 🔒 PROTECCIÓN ENTERPRISE: Solo usamos "placeholders" (textos falsos) en el código.
        // Las credenciales reales se llenarán a mano en el archivo config.yml generado en el servidor.
        
        @Setting
        private String url = "jdbc:postgresql://tu-url-de-supabase.com:5432/postgres";
        
        @Setting
        private String username = "tu_usuario_aqui";
        
        @Setting
        private String password = "CAMBIA_ESTO_EN_EL_CONFIG_YML";

        public String url() { return url; }
        public String username() { return username; }
        public String password() { return password; }
    }

    // =========================================
    // 🎣 NODO: Sistema de Pesca
    // =========================================
    @ConfigSerializable
    public static class PescaNode {
        @Setting("probabilidad_base_scc")
        private double probabilidadBaseScc = 5.0;

        // 🌟 MAGIA DINÁMICA: Carga cualquier criatura que agregues al YML automáticamente
        @Setting
        private Map<String, CriaturaNode> criaturas = new HashMap<>();

        public PescaNode() {
            // Valores por defecto (Modernizados a MiniMessage/Hex para Paper 1.21.5)
            criaturas.put("emperador", new CriaturaNode(25, "ELDER_GUARDIAN", "&#aa0000<bold>Emperador de las Profundidades</bold>", 500.0, "NONE"));
            criaturas.put("fango", new CriaturaNode(15, "DROWNED", "&#aa00aa<bold>Monstruo del Fango</bold>", 250.0, "TRIDENT"));
            criaturas.put("caminante", new CriaturaNode(5, "WITHER_SKELETON", "&#5555ffCaminante Acuático", 100.0, "STONE_SWORD"));
            criaturas.put("marinero", new CriaturaNode(1, "ZOMBIE", "&#55ff55Marinero Perdido", 50.0, "NONE"));
        }

        public double probabilidadBaseScc() { return probabilidadBaseScc; }
        public Map<String, CriaturaNode> criaturas() { return criaturas; }
    }

    // =========================================
    // 👾 NODO: Modelo de Criatura
    // =========================================
    @ConfigSerializable
    public static class CriaturaNode {
        @Setting("nivel_requerido")
        private int nivelRequerido;
        
        @Setting
        private String mob;
        
        @Setting
        private String nombre;
        
        @Setting
        private double vida;
        
        @Setting("arma_mano")
        private String armaMano;

        public CriaturaNode() {} // Constructor vacío requerido por el motor de reflexión de Configurate

        public CriaturaNode(int nivelRequerido, String mob, String nombre, double vida, String armaMano) {
            this.nivelRequerido = nivelRequerido;
            this.mob = mob;
            this.nombre = nombre;
            this.vida = vida;
            this.armaMano = armaMano;
        }

        public int nivelRequerido() { return nivelRequerido; }
        public String mob() { return mob; }
        public String nombre() { return nombre; }
        public double vida() { return vida; }
        public String armaMano() { return armaMano; }
    }
}