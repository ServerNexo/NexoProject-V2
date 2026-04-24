package me.nexo.items.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.items.NexoItems;
import me.nexo.items.estaciones.UpgradeMenu;
import me.nexo.items.managers.ItemManager;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 🎒 NexoItems - Comando de Forja/Upgrade (Arquitectura Enterprise Java 21)
 * Rendimiento: CommandMap nativo, Setters Seguros, Inyección Transitiva y Cero Dependencias Muertas.
 */
@Singleton
public class ComandoUpgrade extends Command {

    // 🌟 DEPENDENCIAS PROPAGADAS
    private final NexoItems plugin;
    private final ItemManager itemManager;
    private final CrossplayUtils crossplayUtils;

    // 💉 PILAR 1: Inyección Estricta (Agregamos las dependencias que el menú ahora exige)
    @Inject
    public ComandoUpgrade(NexoItems plugin, ItemManager itemManager, CrossplayUtils crossplayUtils) {
        super("forja");

        // 🌟 FIX ERROR ALIASES: Usamos los Setters oficiales para mantener el encapsulamiento
        this.setDescription("Abre la interfaz de la Forja Cénit.");
        this.setAliases(List.of("upgrade", "upgradeitem"));

        // Descomenta esto si deseas aplicar el permiso globalmente desde el CommandMap
        // this.setPermission("nexo.items.forja.remota");
        // this.setPermissionMessage("El Vacío rechaza tu petición (Sin Permisos).");

        this.plugin = plugin;
        this.itemManager = itemManager;
        this.crossplayUtils = crossplayUtils;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {

        // 🌟 JAVA 21: Pattern Matching
        if (!(sender instanceof Player player)) {
            sender.sendMessage("[!] La forja solo está disponible para jugadores en el plano físico.");
            return true;
        }

        // 🌟 Invocamos el menú inyectando las instancias necesarias (Cero estáticos)
        new UpgradeMenu(player, plugin, itemManager, crossplayUtils).open();

        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.5f, 1.2f);
        return true;
    }
}