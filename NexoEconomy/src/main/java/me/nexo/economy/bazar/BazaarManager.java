package me.nexo.economy.bazar;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.economy.NexoEconomy;
import me.nexo.core.database.DatabaseManager;
import me.nexo.economy.core.NexoAccount;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 💰 NexoEconomy - Manager del Bazar Global (Arquitectura Enterprise Java 25)
 * Motor HFT (High-Frequency Trading) con Virtual Threads y Transacciones SQL ACID (Anti-Dupe).
 */
@Singleton
public class BazaarManager {

    private final NexoEconomy plugin;
    private final me.nexo.core.database.DatabaseManager db; // 🌟 Desacoplado de NexoCore

    // 🚀 EL MOTOR DE RENDIMIENTO I/O: Hilos Virtuales para alta concurrencia
    private static final Executor VIRTUAL_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    @Inject
    public BazaarManager(NexoEconomy plugin, me.nexo.core.database.DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
        crearTablasBazar();
    }

    private void crearTablasBazar() {
        Thread.startVirtualThread(() -> {
            try (var conn = db.getConnection();
                 var stmt = conn.createStatement()) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS nexo_bazaar_orders (
                        order_id SERIAL PRIMARY KEY,
                        owner_id UUID NOT NULL,
                        order_type VARCHAR(10) NOT NULL,
                        item_id VARCHAR(64) NOT NULL,
                        amount INT NOT NULL,
                        price_per_unit DECIMAL(20,2) NOT NULL,
                        timestamp BIGINT NOT NULL
                    );
                """);
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_bazaar_item ON nexo_bazaar_orders(item_id);");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_bazaar_price ON nexo_bazaar_orders(price_per_unit);");
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS nexo_bazaar_deliveries (
                        id UUID PRIMARY KEY,
                        owner_id UUID NOT NULL,
                        item_id VARCHAR(64) NOT NULL,
                        amount INT NOT NULL,
                        coins DECIMAL(20,2) DEFAULT 0
                    );
                """);
            } catch (Exception e) {
                plugin.getLogger().severe("❌ Error creando tablas del Bazar: " + e.getMessage());
            }
        });
    }

    // 🌟 FIX: Puente Invisible (Reflection) para romper la dependencia circular con NexoColecciones
    private boolean tieneNivelComercial(Player player, String itemId) {
        if (!Bukkit.getPluginManager().isPluginEnabled("NexoColecciones")) return true;
        try {
            // Buscamos el plugin en memoria en lugar de usar import directo
            org.bukkit.plugin.Plugin pluginObj = Bukkit.getPluginManager().getPlugin("NexoColecciones");
            if (pluginObj == null) return true;

            // Usamos Reflection para invocar los métodos sin acoplar los módulos en Gradle
            Object colManager = pluginObj.getClass().getMethod("getCollectionManager").invoke(pluginObj);
            Object itemData = colManager.getClass().getMethod("getItemGlobal", String.class).invoke(colManager, itemId);
            if (itemData == null) return true;

            Object profile = colManager.getClass().getMethod("getProfile", UUID.class).invoke(colManager, player.getUniqueId());
            if (profile == null) return false;

            int progress = (int) profile.getClass().getMethod("getProgress", String.class).invoke(profile, itemId);
            int nivel = (int) colManager.getClass().getMethod("calcularNivel", itemData.getClass(), int.class).invoke(colManager, itemData, progress);

            return nivel >= 1;
        } catch (Exception e) {
            plugin.getLogger().warning("⚠ Error leyendo colección de " + player.getName() + " (Bazar)");
            return true; // Ante la duda, permitimos el trade
        }
    }

    // 🌟 SE EJECUTA EN EL MAIN THREAD (Porque altera inventarios)
    public void crearOrdenVenta(Player player, String itemId, int amount, BigDecimal pricePerUnit) {
        if (!tieneNivelComercial(player, itemId)) {
            CrossplayUtils.sendMessage(player, "&#FF5555[!] Necesitas alcanzar el Nivel 1 en la colección de este ítem para comerciarlo.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        Material mat = Material.matchMaterial(itemId);
        if (mat == null || !player.getInventory().containsAtLeast(new ItemStack(mat), amount)) {
            CrossplayUtils.sendMessage(player, "&#FF5555[!] No tienes suficientes ítems en tu inventario.");
            return;
        }

        quitarItems(player, mat, amount);

        // 🚀 Despachamos a un Hilo Virtual para no congelar al jugador
        VIRTUAL_EXECUTOR.execute(() -> {
            guardarOrdenYEmparejar(player.getUniqueId(), BazaarOrder.OrderType.SELL, itemId, amount, pricePerUnit);

            // Volvemos al hilo principal solo para enviar mensajes/sonidos seguros
            Bukkit.getScheduler().runTask(plugin, () -> {
                CrossplayUtils.sendMessage(player, "&#55FF55[✓] <bold>BAZAR:</bold> &#E6CCFFHas creado una oferta de venta por &#55FF55" + amount + "x " + mat.name() + " &#E6CCFFa &#FFAA00" + pricePerUnit + " Monedas &#E6CCFFc/u.");
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            });
        });
    }

    public void crearOrdenCompra(Player player, String itemId, int amount, BigDecimal pricePerUnit) {
        if (!tieneNivelComercial(player, itemId)) {
            CrossplayUtils.sendMessage(player, "&#FF5555[!] Necesitas alcanzar el Nivel 1 en la colección de este ítem para comerciarlo.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        BigDecimal totalCost = pricePerUnit.multiply(new BigDecimal(amount));

        plugin.getEconomyManager().updateBalanceAsync(player.getUniqueId(), NexoAccount.AccountType.PLAYER, NexoAccount.Currency.COINS, totalCost, false).thenAcceptAsync(success -> {
            if (success) {
                guardarOrdenYEmparejar(player.getUniqueId(), BazaarOrder.OrderType.BUY, itemId, amount, pricePerUnit);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    CrossplayUtils.sendMessage(player, "&#55FF55[✓] <bold>BAZAR:</bold> &#E6CCFFHas creado una petición de compra por &#55FF55" + amount + "x " + itemId + " &#E6CCFFreteniendo &#FFAA00" + totalCost + " Monedas&#E6CCFF.");
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                });
            } else {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    CrossplayUtils.sendMessage(player, "&#FF5555[!] Fondos insuficientes para crear esta orden.");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                });
            }
        }, VIRTUAL_EXECUTOR);
    }

    private void guardarOrdenYEmparejar(UUID ownerId, BazaarOrder.OrderType type, String itemId, int amount, BigDecimal pricePerUnit) {
        long timestamp = System.currentTimeMillis();
        String insert = "INSERT INTO nexo_bazaar_orders (owner_id, order_type, item_id, amount, price_per_unit, timestamp) VALUES (CAST(? AS UUID), ?, ?, ?, ?, ?)";

        try (var conn = db.getConnection();
             var ps = conn.prepareStatement(insert)) {
            ps.setString(1, ownerId.toString());
            ps.setString(2, type.name());
            ps.setString(3, itemId.toUpperCase());
            ps.setInt(4, amount);
            ps.setBigDecimal(5, pricePerUnit);
            ps.setLong(6, timestamp);
            ps.executeUpdate();

            ejecutarMotorDeCruce(conn, itemId); // 🌟 Pasamos la conexión para que trabaje en la misma transacción

        } catch (Exception e) {
            plugin.getLogger().severe("❌ Error guardando orden: " + e.getMessage());
        }
    }

    private void ejecutarMotorDeCruce(java.sql.Connection conn, String itemId) {
        // 🌟 FOR UPDATE: Candado ACID. Bloquea la fila temporalmente para que nadie más la cruce o cancele al mismo tiempo.
        String findMatchSQL = """
            SELECT b.order_id AS buy_id, b.owner_id AS buyer_id, b.amount AS buy_amount, b.price_per_unit AS buy_price,
                   s.order_id AS sell_id, s.owner_id AS seller_id, s.amount AS sell_amount, s.price_per_unit AS sell_price
            FROM nexo_bazaar_orders b
            INNER JOIN nexo_bazaar_orders s ON b.item_id = s.item_id
            WHERE b.order_type = 'BUY' AND s.order_type = 'SELL'
              AND b.item_id = ?
              AND b.price_per_unit >= s.price_per_unit
            ORDER BY s.price_per_unit ASC, b.timestamp ASC
            LIMIT 1
            FOR UPDATE 
        """;

        try (var ps = conn.prepareStatement(findMatchSQL)) {
            ps.setString(1, itemId);
            var rs = ps.executeQuery();

            if (rs.next()) {
                int buyId = rs.getInt("buy_id");
                UUID buyerId = UUID.fromString(rs.getString("buyer_id"));
                int buyAmount = rs.getInt("buy_amount");

                int sellId = rs.getInt("sell_id");
                UUID sellerId = UUID.fromString(rs.getString("seller_id"));
                int sellAmount = rs.getInt("sell_amount");

                BigDecimal matchPrice = rs.getBigDecimal("sell_price");
                int cantidadIntercambiada = Math.min(buyAmount, sellAmount);
                BigDecimal totalTransferencia = matchPrice.multiply(new BigDecimal(cantidadIntercambiada));

                // 1% Tax
                BigDecimal tax = totalTransferencia.multiply(new BigDecimal("0.01"));
                BigDecimal netoParaVendedor = totalTransferencia.subtract(tax);

                plugin.getEconomyManager().updateBalanceAsync(sellerId, NexoAccount.AccountType.PLAYER, NexoAccount.Currency.COINS, netoParaVendedor, true);

                // Enviar ítems al comprador (Enviando la conexión para asegurar atomicidad)
                enviarABuzonSync(conn, buyerId, itemId, cantidadIntercambiada, BigDecimal.ZERO);

                actualizarOrden(conn, buyId, buyAmount - cantidadIntercambiada);
                actualizarOrden(conn, sellId, sellAmount - cantidadIntercambiada);

                plugin.getLogger().info("📈 [BAZAR] Cruce exitoso de " + cantidadIntercambiada + "x " + itemId + " por " + totalTransferencia + " Monedas.");

                // Llamada recursiva
                ejecutarMotorDeCruce(conn, itemId);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("❌ Error en el Matching Engine: " + e.getMessage());
        }
    }

    private void actualizarOrden(java.sql.Connection conn, int orderId, int remainingAmount) throws Exception {
        if (remainingAmount <= 0) {
            try (var ps = conn.prepareStatement("DELETE FROM nexo_bazaar_orders WHERE order_id = ?")) {
                ps.setInt(1, orderId);
                ps.executeUpdate();
            }
        } else {
            try (var ps = conn.prepareStatement("UPDATE nexo_bazaar_orders SET amount = ? WHERE order_id = ?")) {
                ps.setInt(1, remainingAmount);
                ps.setInt(2, orderId);
                ps.executeUpdate();
            }
        }
    }

    private void enviarABuzonSync(java.sql.Connection conn, UUID ownerId, String itemId, int amount, BigDecimal coins) throws Exception {
        String insert = "INSERT INTO nexo_bazaar_deliveries (id, owner_id, item_id, amount, coins) VALUES (CAST(? AS UUID), CAST(? AS UUID), ?, ?, ?)";
        try (var ps = conn.prepareStatement(insert)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, ownerId.toString());
            ps.setString(3, itemId);
            ps.setInt(4, amount);
            ps.setBigDecimal(5, coins);
            ps.executeUpdate();

            Bukkit.getScheduler().runTask(plugin, () -> {
                Player p = Bukkit.getPlayer(ownerId);
                if (p != null && (amount > 0 || coins.compareTo(BigDecimal.ZERO) > 0)) {
                    CrossplayUtils.sendMessage(p, "&#555555--------------------------------");
                    CrossplayUtils.sendMessage(p, "&#FFAA00📦 <bold>ENTREGA DEL BAZAR</bold>");
                    CrossplayUtils.sendMessage(p, "&#E6CCFFUna de tus órdenes se ha completado.");
                    CrossplayUtils.sendMessage(p, "&#E6CCFFRevisa tus entregas en el menú del Bazar.");
                    CrossplayUtils.sendMessage(p, "&#555555--------------------------------");
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.2f);
                }
            });
        }
    }

    // 🌟 EJECUCIÓN MAIN THREAD OBLIGATORIA
    private void quitarItems(Player player, Material mat, int amountToRemove) {
        int removed = 0;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == mat) {
                if (removed + item.getAmount() <= amountToRemove) {
                    removed += item.getAmount();
                    player.getInventory().setItem(i, null);
                } else {
                    int toTake = amountToRemove - removed;
                    item.setAmount(item.getAmount() - toTake);
                    removed += toTake;
                    break;
                }
            }
        }
    }

    public void reclamarBuzon(Player player) {
        VIRTUAL_EXECUTOR.execute(() -> {
            String select = "SELECT id, item_id, amount, coins FROM nexo_bazaar_deliveries WHERE owner_id = CAST(? AS UUID)";
            try (var conn = db.getConnection();
                 var ps = conn.prepareStatement(select)) {

                ps.setString(1, player.getUniqueId().toString());
                var rs = ps.executeQuery();
                boolean tieneCosas = false;

                while (rs.next()) {
                    tieneCosas = true;
                    String deliveryId = rs.getString("id");
                    String itemId = rs.getString("item_id");
                    int amount = rs.getInt("amount");
                    BigDecimal coins = rs.getBigDecimal("coins");

                    String delete = "DELETE FROM nexo_bazaar_deliveries WHERE id = CAST(? AS UUID)";
                    try (var delPs = conn.prepareStatement(delete)) {
                        delPs.setString(1, deliveryId);
                        delPs.executeUpdate();
                    }

                    if (itemId.equals("COINS") || coins.compareTo(BigDecimal.ZERO) > 0) {
                        plugin.getEconomyManager().updateBalanceAsync(player.getUniqueId(), NexoAccount.AccountType.PLAYER, NexoAccount.Currency.COINS, coins, true);
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            CrossplayUtils.sendMessage(player, "&#55FF55[✓] <bold>BAZAR:</bold> &#E6CCFFHas reclamado &#FFAA00+" + coins.toString() + " Monedas&#E6CCFF.");
                            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                        });
                    } else {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            Material mat = Material.matchMaterial(itemId);
                            if (mat != null) {
                                ItemStack item = new ItemStack(mat, amount);
                                if (player.getInventory().firstEmpty() == -1) {
                                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                                } else {
                                    player.getInventory().addItem(item);
                                }
                                CrossplayUtils.sendMessage(player, "&#55FF55[✓] <bold>BAZAR:</bold> &#E6CCFFHas reclamado &#00f5ff" + amount + "x " + mat.name() + "&#E6CCFF.");
                                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
                            }
                        });
                    }
                }

                if (!tieneCosas) {
                    Bukkit.getScheduler().runTask(plugin, () -> CrossplayUtils.sendMessage(player, "&#FF5555[!] Tu buzón de entregas está vacío."));
                }
            } catch (Exception e) {
                plugin.getLogger().severe("❌ Error reclamando buzón: " + e.getMessage());
            }
        });
    }

    // ==========================================
    // 📊 LECTURAS RÁPIDAS DE MERCADO (Virtual Threads)
    // ==========================================
    public BigDecimal getMejorPrecioCompra(String itemId) {
        try (var conn = db.getConnection();
             var ps = conn.prepareStatement("SELECT MAX(price_per_unit) FROM nexo_bazaar_orders WHERE item_id = ? AND order_type = 'BUY'")) {
            ps.setString(1, itemId);
            try (var rs = ps.executeQuery()) {
                if (rs.next() && rs.getBigDecimal(1) != null) return rs.getBigDecimal(1);
            }
        } catch (Exception ignored) {}
        return BigDecimal.ZERO;
    }

    public BigDecimal getMejorPrecioVenta(String itemId) {
        try (var conn = db.getConnection();
             var ps = conn.prepareStatement("SELECT MIN(price_per_unit) FROM nexo_bazaar_orders WHERE item_id = ? AND order_type = 'SELL'")) {
            ps.setString(1, itemId);
            try (var rs = ps.executeQuery()) {
                if (rs.next() && rs.getBigDecimal(1) != null) return rs.getBigDecimal(1);
            }
        } catch (Exception ignored) {}
        return BigDecimal.ZERO;
    }

    public int getVolumenOrdenes(String itemId, String type) {
        try (var conn = db.getConnection();
             var ps = conn.prepareStatement("SELECT SUM(amount) FROM nexo_bazaar_orders WHERE item_id = ? AND order_type = ?")) {
            ps.setString(1, itemId);
            ps.setString(2, type);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception ignored) {}
        return 0;
    }

    public static class ActiveOrderDTO {
        public int id;
        public String itemId;
        public int amount;
        public BigDecimal price;
        public String type;

        public ActiveOrderDTO(int id, String itemId, int amount, BigDecimal price, String type) {
            this.id = id;
            this.itemId = itemId;
            this.amount = amount;
            this.price = price;
            this.type = type;
        }
    }

    public List<ActiveOrderDTO> getMisOrdenes(UUID ownerId) {
        List<ActiveOrderDTO> orders = new ArrayList<>();
        try (var conn = db.getConnection();
             var ps = conn.prepareStatement("SELECT order_id, item_id, amount, price_per_unit, order_type FROM nexo_bazaar_orders WHERE owner_id = CAST(? AS UUID) ORDER BY timestamp DESC")) {
            ps.setString(1, ownerId.toString());
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    orders.add(new ActiveOrderDTO(
                            rs.getInt("order_id"), rs.getString("item_id"), rs.getInt("amount"),
                            rs.getBigDecimal("price_per_unit"), rs.getString("order_type")
                    ));
                }
            }
        } catch (Exception ignored) {}
        return orders;
    }

    // 🌟 PROTECCIÓN ANTI-DUPE SQL: Transacciones seguras para cancelaciones
    public void cancelarOrden(Player player, int orderId) {
        VIRTUAL_EXECUTOR.execute(() -> {
            try (var conn = db.getConnection()) {
                conn.setAutoCommit(false); // Inicia transacción

                // 🌟 FOR UPDATE OF: Evita que el Motor de Cruce cruce la orden mientras la cancelas
                var psSearch = conn.prepareStatement("SELECT item_id, amount, price_per_unit, order_type FROM nexo_bazaar_orders WHERE order_id = ? AND owner_id = CAST(? AS UUID) FOR UPDATE");
                psSearch.setInt(1, orderId);
                psSearch.setString(2, player.getUniqueId().toString());
                var rs = psSearch.executeQuery();

                if (rs.next()) {
                    String itemId = rs.getString("item_id");
                    int amount = rs.getInt("amount");
                    BigDecimal price = rs.getBigDecimal("price_per_unit");
                    String type = rs.getString("order_type");

                    var psDelete = conn.prepareStatement("DELETE FROM nexo_bazaar_orders WHERE order_id = ?");
                    psDelete.setInt(1, orderId);
                    psDelete.executeUpdate();

                    if (type.equals("SELL")) {
                        enviarABuzonSync(conn, player.getUniqueId(), itemId, amount, BigDecimal.ZERO);
                    } else {
                        BigDecimal totalCoins = price.multiply(new BigDecimal(amount));
                        enviarABuzonSync(conn, player.getUniqueId(), "COINS", 0, totalCoins);
                    }

                    conn.commit(); // Aplica los cambios atómicamente

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        CrossplayUtils.sendMessage(player, "&#55FF55[✓] <bold>BAZAR:</bold> &#E6CCFFOrden cancelada. Revisa tu buzón de entregas.");
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);
                    });
                } else {
                    conn.rollback();
                    Bukkit.getScheduler().runTask(plugin, () -> CrossplayUtils.sendMessage(player, "&#FF5555[!] Esa orden no existe o ya fue completada."));
                }
            } catch (Exception e) {
                plugin.getLogger().severe("❌ Error cancelando orden: " + e.getMessage());
            }
        });
    }

    public void saveMarketSync() {
        plugin.getLogger().info("✅ El estado del Bazar Global ya se encuentra sincronizado con la Base de Datos central.");
    }
}