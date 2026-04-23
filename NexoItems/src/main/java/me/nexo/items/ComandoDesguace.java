package me.nexo.items;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.items.estaciones.DesguaceListener;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;

/**
 * 🎒 NexoItems - Comando de Desguace (Arquitectura Enterprise Java 21)
 * Rendimiento: Cero Dependencias Muertas (Dead Dependencies) e Inyección Nativa vía Lamp.
 */
@Singleton
public class ComandoDesguace {

    private final DesguaceListener desguaceListener;

    // 💉 PILAR 1: Inyección de Dependencias Estricta (Solo lo que se necesita)
    @Inject
    public ComandoDesguace(DesguaceListener desguaceListener) {
        this.desguaceListener = desguaceListener;
    }

    // 🌟 Lamp Framework se encarga automáticamente de verificar que el ejecutor sea un Player
    // y lo inyecta dinámicamente en el CommandMap del servidor.
    @Command({"desguace", "recycle", "salvage"})
    public void openDesguace(Player player) {

        // 🛡️ Delegamos la apertura al método seguro inyectado
        // ¡Así garantizamos que use el DesguaceMenuHolder anti-duplicación!
        desguaceListener.abrirMenu(player);

    }
}