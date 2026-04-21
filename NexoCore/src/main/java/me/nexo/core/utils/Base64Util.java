package me.nexo.core.utils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

/**
 * 📦 NexoCore - Utilidad de Serialización Base64 (Arquitectura Enterprise)
 * Convertido a un servicio inyectable (Singleton) para evitar el anti-patrón de métodos estáticos.
 */
@Singleton
public class Base64Util {

    // 💉 PILAR 1: Inyección de Dependencias
    @Inject
    public Base64Util() {
        // Constructor vacío listo para ser instanciado una sola vez por Guice
    }

    // 📦 CONVIERTE UN INVENTARIO A TEXTO (Para guardar en PostgreSQL)
    public String itemStackArrayToBase64(ItemStack[] items) {
        if (items == null || items.length == 0) return "";
        
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

            // Escribimos el tamaño del inventario
            dataOutput.writeInt(items.length);

            // Escribimos cada ítem uno por uno
            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }

            dataOutput.close();
            // Lo pasamos a Base64 estándar de Java
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    // 🪄 CONVIERTE EL TEXTO DE VUELTA A UN INVENTARIO (Para cuando el jugador abre la mochila)
    public ItemStack[] itemStackArrayFromBase64(String data) {
        // Si no hay datos, devolvemos un inventario vacío
        if (data == null || data.isEmpty()) return new ItemStack[0];

        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);

            // Leemos el tamaño original del inventario
            ItemStack[] items = new ItemStack[dataInput.readInt()];

            // Reconstruimos los ítems uno a uno
            for (int i = 0; i < items.length; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }

            dataInput.close();
            return items;
        } catch (Exception e) {
            e.printStackTrace();
            return new ItemStack[0]; // Si hay error, evitamos que crashee
        }
    }
}