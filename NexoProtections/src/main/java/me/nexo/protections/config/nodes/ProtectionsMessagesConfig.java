package me.nexo.protections.config.nodes;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.List;

/**
 * 🛡️ NexoProtections - Configurate Type-Safe Node (Arquitectura Enterprise)
 * Estructura de datos inmutable, Type-Safe y lista para MiniMessage.
 */
@ConfigSerializable
public class ProtectionsMessagesConfig {

    @Setting private MenusNode menus = new MenusNode();
    @Setting private MensajesNode mensajes = new MensajesNode();

    public MenusNode menus() { return menus; }
    public MensajesNode mensajes() { return mensajes; }

    // =========================================
    // 🗂️ NODO: Menus
    // =========================================
    @ConfigSerializable
    public static class MenusNode {
        @Setting private PrincipalMenuNode principal = new PrincipalMenuNode();
        @Setting private MiembrosMenuNode miembros = new MiembrosMenuNode();
        @Setting private LeyesMenuNode leyes = new LeyesMenuNode();

        public PrincipalMenuNode principal() { return principal; }
        public MiembrosMenuNode miembros() { return miembros; }
        public LeyesMenuNode leyes() { return leyes; }
    }

    @ConfigSerializable
    public static class PrincipalMenuNode {
        @Setting private String titulo = "&#E6CCFF<bold>»</bold> &#00f5ffMonolito del Vacío";
        @Setting private MenuItem acolitos = new MenuItem();
        @Setting private MenuItem nucleo = new MenuItem();
        @Setting private MenuItem leyes = new MenuItem();
        @Setting private MenuItem recarga = new MenuItem();

        public String titulo() { return titulo; }
        public MenuItem acolitos() { return acolitos; }
        public MenuItem nucleo() { return nucleo; }
        public MenuItem leyes() { return leyes; }
        public MenuItem recarga() { return recarga; }
    }

    @ConfigSerializable
    public static class MiembrosMenuNode {
        @Setting private String titulo = "&#E6CCFF<bold>»</bold> &#00f5ffAcólitos del Pacto";
        @Setting private MiembrosItemsNode items = new MiembrosItemsNode();

        public String titulo() { return titulo; }
        public MiembrosItemsNode items() { return items; }
    }

    @ConfigSerializable
    public static class MiembrosItemsNode {
        @Setting private MenuItem cabeza = new MenuItem();
        @Setting private MenuItem volver = new MenuItem();
        @Setting private MenuItem invocar = new MenuItem();

        public MenuItem cabeza() { return cabeza; }
        public MenuItem volver() { return volver; }
        public MenuItem invocar() { return invocar; }
    }

    @ConfigSerializable
    public static class LeyesMenuNode {
        @Setting private String titulo = "&#E6CCFF<bold>»</bold> &#00f5ffLeyes del Dominio";
        @Setting private LeyesItemsNode items = new LeyesItemsNode();
        @Setting private FlagsTranslationsNode flags = new FlagsTranslationsNode();

        public String titulo() { return titulo; }
        public LeyesItemsNode items() { return items; }
        public FlagsTranslationsNode flags() { return flags; }
    }

    @ConfigSerializable
    public static class LeyesItemsNode {
        @Setting private MenuItem volver = new MenuItem();
        @Setting private FlagItemNode flag = new FlagItemNode();

        public MenuItem volver() { return volver; }
        public FlagItemNode flag() { return flag; }
    }

    @ConfigSerializable
    public static class FlagItemNode {
        @Setting private List<String> lore;
        @Setting("estado-permitido") private String estadoPermitido = "&#00f5ff[ PERMITIDO ]";
        @Setting("estado-bloqueado") private String estadoBloqueado = "&#8b0000[ BLOQUEADO ]";

        public List<String> lore() { return lore; }
        public String estadoPermitido() { return estadoPermitido; }
        public String estadoBloqueado() { return estadoBloqueado; }
    }

    @ConfigSerializable
    public static class FlagsTranslationsNode {
        @Setting private String pvp = "Daño PvP";
        @Setting("mob-spawning") private String mobSpawning = "Aparición de Monstruos";
        @Setting("tnt-damage") private String tntDamage = "Daño de Explosiones";
        @Setting("fire-spread") private String fireSpread = "Propagación de Fuego";
        @Setting("animal-damage") private String animalDamage = "Asesinato de Animales";
        @Setting private String interact = "Uso de Puertas/Botones";
        @Setting private String containers = "Abrir Cofres/Hornos";
        @Setting("item-pickup") private String itemPickup = "Robar Ítems del Suelo";
        @Setting("item-drop") private String itemDrop = "Tirar Basura (Drop)";
        @Setting("ENTRY") private String entry = "Entrada de Forasteros";

        public String pvp() { return pvp; }
        public String mobSpawning() { return mobSpawning; }
        public String tntDamage() { return tntDamage; }
        public String fireSpread() { return fireSpread; }
        public String animalDamage() { return animalDamage; }
        public String interact() { return interact; }
        public String containers() { return containers; }
        public String itemPickup() { return itemPickup; }
        public String itemDrop() { return itemDrop; }
        public String entry() { return entry; }
    }

    // 📦 Estructura base para Items de Menú
    @ConfigSerializable
    public static class MenuItem {
        @Setting private String nombre;
        @Setting private List<String> lore;

        public String nombre() { return nombre; }
        public List<String> lore() { return lore; }
    }

    // =========================================
    // 🗂️ NODO: Mensajes
    // =========================================
    @ConfigSerializable
    public static class MensajesNode {
        @Setting private ErroresNode errores = new ErroresNode();
        @Setting private ExitoNode exito = new ExitoNode();
        @Setting private ItemsNode items = new ItemsNode();

        public ErroresNode errores() { return errores; }
        public ExitoNode exito() { return exito; }
        public ItemsNode items() { return items; }
    }

    @ConfigSerializable
    public static class ErroresNode {
        @Setting("solo-dueno") private String soloDueno = "&#FF3366[!] Solo el Señor del Dominio puede alterar las leyes naturales.";
        @Setting("monolito-lleno") private String monolitoLleno = "&#FF3366[!] El Monolito está completamente saciado.";
        @Setting("ofrenda-rechazada") private String ofrendaRechazada = "&#FF3366[!] Ofrenda Rechazada: &#E6CCFFNecesitas Diamantes o Fragmentos de Eco para alimentar el vacío.";
        @Setting("sin-monolito-home") private String sinMonolitoHome = "&#FF3366[!] Error: &#E6CCFFNo posees ningún Monolito en este mundo.";
        @Setting("fuera-fronteras") private String fueraFronteras = "&#FF3366[!] Error: &#E6CCFFNo te encuentras dentro de las fronteras de ningún Monolito.";
        @Setting("herejia-trust") private String herejiaTrust = "&#FF3366[!] Herejía: &#E6CCFFDebes estar dentro de tu Monolito para forjar un Pacto de Sangre.";
        @Setting("alma-offline") private String almaOffline = "&#FF3366[!] Error: &#E6CCFFEsa alma no se encuentra en este reino (Offline).";
        
        @Setting("no-destruir-ajeno") private String noDestruirAjeno = "&#8b0000[!] Herejía: &#E6CCFFSolo el Señor de este Dominio puede destruir el Monolito.";
        @Setting("dominio-sellado") private String dominioSellado = "&#8b0000[!] Dominio Sellado: &#E6CCFFEl vacío protege estas tierras. No puedes alterar su forma.";
        @Setting("sin-construir-ajeno") private String sinConstruirAjeno = "&#8b0000[!] Dominio Sellado: &#E6CCFFNo puedes invocar estructuras en tierras ajenas.";
        @Setting("colision-energia") private String colisionEnergia = "&#8b0000[!] Energía Corrupta: &#E6CCFFEl aura de este Monolito colisiona con otro sello cercano. Aléjate más.";
        @Setting("limite-alcanzado") private String limiteAlcanzado = "&#8b0000[!] Límite Alcanzado: &#E6CCFFTu alma no soporta mantener más Monolitos.";
        @Setting("monolito-rechaza") private String monolitoRechaza = "&#8b0000[!] Herejía: &#E6CCFFEl Monolito rechaza tu tacto.";
        @Setting("campo-fuerza") private String campoFuerza = "&#8b0000[!] Campo de Fuerza: &#E6CCFFEste dominio está cerrado para extraños.";
        
        @Setting("tesoros-sellados") private String tesorosSellados = "&#FF3366[!] Herejía: &#E6CCFFLos tesoros de este dominio están sellados.";
        @Setting("sin-interactuar") private String sinInteractuar = "&#FF3366[!] Herejía: &#E6CCFFNo tienes permiso para interactuar aquí.";
        @Setting("no-arrojar-ofrendas") private String noArrojarOfrendas = "&#FF3366[!] Dominio Puro: &#E6CCFFNo puedes arrojar ofrendas al suelo en tierras ajenas.";

        public String soloDueno() { return soloDueno; }
        public String monolitoLleno() { return monolitoLleno; }
        public String ofrendaRechazada() { return ofrendaRechazada; }
        public String sinMonolitoHome() { return sinMonolitoHome; }
        public String fueraFronteras() { return fueraFronteras; }
        public String herejiaTrust() { return herejiaTrust; }
        public String almaOffline() { return almaOffline; }
        public String noDestruirAjeno() { return noDestruirAjeno; }
        public String dominioSellado() { return dominioSellado; }
        public String sinConstruirAjeno() { return sinConstruirAjeno; }
        public String colisionEnergia() { return colisionEnergia; }
        public String limiteAlcanzado() { return limiteAlcanzado; }
        public String monolitoRechaza() { return monolitoRechaza; }
        public String campoFuerza() { return campoFuerza; }
        public String tesorosSellados() { return tesorosSellados; }
        public String sinInteractuar() { return sinInteractuar; }
        public String noArrojarOfrendas() { return noArrojarOfrendas; }
    }

    @ConfigSerializable
    public static class ExitoNode {
        @Setting("recarga-exitosa") private String recargaExitosa = "&#CC66FF[✓] <bold>ESENCIA DEVORADA:</bold> &#E6CCFFEl Monolito ha absorbido tu ofrenda.";
        @Setting("destierro") private String destierro = "&#FF3366[!] DESTIERRO: &#E6CCFFEl alma ha sido expulsada de tu Monolito.";
        @Setting("abismo-despierta") private String abismoDespierta = "&#9933FF[✓] <bold>EL ABISMO DESPIERTA:</bold> &#E6CCFFMonolitos y rituales recargados con éxito.";
        @Setting("viaje-espacial") private String viajeEspacial = "&#CC66FF[✓] <bold>VIAJE ESPACIAL:</bold> &#E6CCFFHas regresado a tu dominio.";
        @Setting("vision-vacio") private String visionVacio = "&#CC66FF[✓] <bold>VISIÓN DEL VACÍO:</bold> &#E6CCFFLas fronteras de este dominio han sido reveladas a tus ojos.";
        @Setting("pacto-forjado-owner") private String pactoForjadoOwner = "&#CC66FF[✓] <bold>PACTO FORJADO:</bold> &#E6CCFF%target% ahora es un Acólito de tu dominio.";
        @Setting("pacto-forjado-target") private String pactoForjadoTarget = "&#9933FF[⟳] Pacto de Sangre: &#E6CCFFHas sido invocado como Acólito en el dominio de %owner%.";
        @Setting("ritual-concedido") private String ritualConcedido = "&#9933FF[✓] <bold>RITUAL CONCEDIDO:</bold> &#E6CCFFSello del Abismo entregado a tu inventario.";
        
        @Setting("ritual-deshecho") private String ritualDeshecho = "&#ff00ff[✓] <bold>RITUAL DESHECHO:</bold> &#E6CCFFEl Monolito del Vacío ha sido desmantelado con éxito.";
        @Setting("sello-invocado") private String selloInvocado = "&#00f5ff[✓] <bold>SELLO INVOCADO:</bold> &#E6CCFFEl Vacío ahora reclama un radio de &#ff00ff%radio% bloques&#E6CCFF.";
        @Setting("zona-protegida") private String zonaProtegida = "&#00f5ff🌿 Has entrado al dominio de %owner%";
        @Setting("zona-salvaje") private String zonaSalvaje = "&#8b0000🌲 Has entrado a Zona Salvaje";

        public String recargaExitosa() { return recargaExitosa; }
        public String destierro() { return destierro; }
        public String abismoDespierta() { return abismoDespierta; }
        public String viajeEspacial() { return viajeEspacial; }
        public String visionVacio() { return visionVacio; }
        public String pactoForjadoOwner() { return pactoForjadoOwner; }
        public String pactoForjadoTarget() { return pactoForjadoTarget; }
        public String ritualConcedido() { return ritualConcedido; }
        public String ritualDeshecho() { return ritualDeshecho; }
        public String selloInvocado() { return selloInvocado; }
        public String zonaProtegida() { return zonaProtegida; }
        public String zonaSalvaje() { return zonaSalvaje; }
    }

    @ConfigSerializable
    public static class ItemsNode {
        @Setting("sello-abismo-nombre") private String selloAbismoNombre = "&#9933FF<bold>SELLO DEL ABISMO</bold>";
        @Setting("sello-abismo-lore") private List<String> selloAbismoLore = List.of(
                "&#E6CCFFColoca este altar antiguo para reclamar",
                "&#E6CCFFun fragmento del mundo y sellarlo",
                "&#E6CCFFcon el poder del Vacío.",
                " ",
                "&#CC66FF► Clic derecho para invocar el dominio"
        );

        public String selloAbismoNombre() { return selloAbismoNombre; }
        public List<String> selloAbismoLore() { return selloAbismoLore; }
    }
}