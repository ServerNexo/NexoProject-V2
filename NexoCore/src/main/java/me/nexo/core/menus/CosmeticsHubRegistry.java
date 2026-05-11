package me.nexo.core.menus;

import com.google.inject.Singleton;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 🏭 Nexo Network - Registro Dinámico del Hub de Cosméticos
 * Permite que módulos externos (NexoChat, NexoCosmetics) inyecten botones
 * en el menú principal sin acoplar código en el Core.
 */
@Singleton
public class CosmeticsHubRegistry {

    // 🌟 JAVA 21 RECORD: Estructura inmutable para definir un botón
    public record HubButton(int slot, Material material, String name, List<String> lore, Consumer<Player> onClick) {}

    // Caché Thread-Safe O(1) de botones registrados por Slot
    private final Map<Integer, HubButton> registeredButtons = new ConcurrentHashMap<>();

    /**
     * Permite a un plugin externo registrar una categoría en el menú principal.
     */
    public void registerCategory(int slot, Material material, String name, List<String> lore, Consumer<Player> onClickAction) {
        registeredButtons.put(slot, new HubButton(slot, material, name, lore, onClickAction));
    }

    public HubButton getButton(int slot) {
        return registeredButtons.get(slot);
    }

    public Collection<HubButton> getAllButtons() {
        return registeredButtons.values();
    }
}