package me.nexo.items;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.items.estaciones.DesguaceListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * 🎒 NexoItems - Comando de Desguace (Arquitectura NATIVA)
 * Fusión de Ejecución directa, bypassing estricto de PaperMC.
 */
@Singleton
public class ComandoDesguace extends Command {

    private final NexoItems plugin;
    private final DesguaceListener desguaceListener;

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public ComandoDesguace(NexoItems plugin, DesguaceListener desguaceListener) {
        super("desguace"); // 🌟 Nombre nativo base
        this.setAliases(List.of("recycle", "salvage")); // Alias nativos

        this.plugin = plugin;
        this.desguaceListener = desguaceListener;
    }

    // ==========================================
    // ⚙️ MOTOR DE EJECUCIÓN NATIVO
    // ==========================================
    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {

        // 💻 PROTECCIÓN DE CONSOLA
        if (!(sender instanceof Player player)) {
            sender.sendMessage("[!] El terminal no posee un inventario para abrir la interfaz de desguace.");
            return true;
        }

        // 🛡️ Delegamos la apertura al método seguro que creaste antes
        // ¡Garantizamos que use el DesguaceMenuHolder anti-duplicación!
        desguaceListener.abrirMenu(player);

        return true;
    }

    // ==========================================
    // 🧠 MOTOR DE AUTOCOMPLETADO NATIVO DIRECTO
    // ==========================================
    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        // Este comando no lleva argumentos, así que devolvemos una lista vacía
        return Collections.emptyList();
    }
}