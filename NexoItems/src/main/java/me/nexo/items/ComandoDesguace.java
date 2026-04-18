package me.nexo.items;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.items.NexoItems;
import me.nexo.items.estaciones.DesguaceListener;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;

/**
 * 🎒 NexoItems - Comando de Desguace (Arquitectura Enterprise)
 */
@Singleton
public class ComandoDesguace {

    private final NexoItems plugin;
    private final DesguaceListener desguaceListener;

    // 💉 PILAR 3: Inyección de Dependencias
    // Le pedimos a Guice que nos traiga el Listener que ya tiene la lógica segura
    @Inject
    public ComandoDesguace(NexoItems plugin, DesguaceListener desguaceListener) {
        this.plugin = plugin;
        this.desguaceListener = desguaceListener;
    }

    // 🌟 Lamp se encarga automáticamente de verificar que el ejecutor sea un Player
    // Ponemos el comando directamente en el método para evitar enredos de subcomandos
    @Command({"desguace", "recycle", "salvage"})
    public void openDesguace(Player player) {

        // 🛡️ Delegamos la apertura al método seguro que creamos antes
        // ¡Así garantizamos que use el DesguaceMenuHolder anti-duplicación!
        desguaceListener.abrirMenu(player);

    }
}