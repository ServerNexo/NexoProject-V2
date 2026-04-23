package me.nexo.economy.bazar;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.database.DatabaseManager;
import me.nexo.economy.NexoEconomy;
import me.nexo.economy.core.EconomyManager;
import me.nexo.economy.core.NexoAccount;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 💰 NexoEconomy - Manager del Bazar Global (Arquitectura Enterprise Java 21)
 * Rendimiento: HFT Matching Engine, ACID SQL, Folia Region Scheduler y Virtual Threads.
 */
@Singleton
public class BazaarManager {

    // 🌟 DEPENDENCIAS PROPAGADAS
    private final NexoEconomy plugin;
    private final DatabaseManager db;
    private final EconomyManager economyManager;
    private final CrossplayUtils crossplayUtils;

    // 🚀 EL MOTOR DE RENDIMIENTO I/O: Hilos Virtuales (Unificado y sin 'static')
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // 🌟 GESTIÓN DE SESIONES (Desacopla el BazaarChatListener)
    private final Map<UUID, ChatOrderSession> chatSessions = new ConcurrentHashMap<>();

    @Inject
    public BazaarManager(NexoEconomy plugin, DatabaseManager db, EconomyManager economyManager, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.db = db;
        this.economyManager = economyManager;
        this.crossplayUtils = crossplayUtils;
        
        crearTablasBazar();
    }

    private void crearTablasBazar() {
        virtualExecutor.submit(() -> {
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

    private boolean tieneNivelComercial(Player player, String itemId) {
        if (!plugin.getServer().getPluginManager().isPluginEnabled("NexoColecciones")) return true;
        try {
            // Reemplazo seguro sin usar Service Locator estático
            var colPlugin = (me.nexo.colecciones.NexoColecciones) plugin.getServer().getPluginManager().getPlugin("NexoColecciones");
            if (colPlugin == null) return true;
            
            var colManager = colPlugin.getCollectionManager();
            var itemData = colManager.getItemGlobal(itemId);
            if (itemData == null) return true;

            var profile = colManager.getProfile(player.getUniqueId());
            if (profile == null) return false;

            int nivel = colManager.calcularNivel(itemData, profile.getProgress(itemId));
            return nivel >= 1;
        } catch (Exception e) {
            return true;
        }
    }

    // 🌟 SE EJECUTA EN EL MAIN THREAD O REGION THREAD (Porque altera inventarios)
    public void crearOrdenVenta(Player player, String itemId, int amount, BigDecimal pricePerUnit) {
        if (!tieneNivelComercial(player, itemId)) {
            crossplayUtils.sendMessage(player, "&#FF5555[!] Necesitas alcanzar el Nivel 1 en la colección de este ítem para comerciarlo.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        Material mat = Material.matchMaterial(itemId);
        if (mat == null || !player.getInventory().containsAtLeast(new ItemStack(mat), amount)) {
            crossplayUtils.sendMessage(player, "&#FF5555[!] No tienes suficientes ítems en tu inventario.");
            return;
        }

        quitarItems(player, mat, amount);

        // 🚀 Despachamos a un Hilo Virtual para no congelar al jugador
        virtualExecutor.execute(() -> {
            guardarOrdenYEmparejar(player.getUniqueId(), BazaarOrder.OrderType.SELL, itemId, amount, pricePerUnit);

            // 🌟 FOLIA SYNC: Volvemos al hilo de la región del jugador solo para mensajes
            player.getScheduler().run(plugin, task -> {
                crossplayUtils.sendMessage(player, "&#55FF55[✓] <bold>BAZAR:</bold> &#E6CCFFHas creado una oferta de venta por &#55FF55" + amount + "x " + mat.name() + " &#E6CCFFa &#FFAA00" + pricePerUnit.toPlainString() + " Monedas &#E6CCFFc/u.");
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            }, null);
        });
    }

    public void crearOrdenCompra(Player player, String itemId, int amount, BigDecimal pricePerUnit) {
        if (!tieneNivelComercial(player, itemId)) {
            crossplayUtils.sendMessage(player, "&#FF5555[!] Necesitas alcanzar el Nivel 1 en la colección de este ítem para comerciarlo.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        BigDecimal totalCost = pricePerUnit.multiply(BigDecimal.valueOf(amount));

        economyManager.updateBalanceAsync(player.getUniqueId(), NexoAccount.AccountType.PLAYER, NexoAccount.Currency.COINS, totalCost, false).thenAcceptAsync(success -> {
            if (success) {
                guardarOrdenYEmparejar(player.getUniqueId(), BazaarOrder.OrderType.BUY, itemId, amount, pricePerUnit);
                
                player.getScheduler().run(plugin, task -> {
                    crossplayUtils.sendMessage(player, "&#55FF55[✓] <bold>BAZAR:</bold> &#E6CCFFHas creado una petición de compra por &#55FF55" + amount + "x " + itemId + " &#E6CCFFreteniendo &#FFAA00" + totalCost.toPlainString() + " Monedas&#E6CCFF.");
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                }, null);
            } else {
                player.getScheduler().run(plugin, task -> {
                    crossplayUtils.sendMessage(player, "&#FF5555[!] Fondos insuficientes para crear esta orden.");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }, null);
            }
        }, virtualExecutor);
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

            ejecutarMotorDeCruce(conn, itemId); 

        } catch (Exception e) {
            plugin.getLogger().severe("❌ Error guardando orden: " + e.getMessage());
        }
    }

    private void ejecutarMotorDeCruce(java.sql.Connection conn, String itemId) {
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
                BigDecimal totalTransferencia = matchPrice.multiply(BigDecimal.valueOf(cantidadIntercambiada));

                // 1% Tax
                BigDecimal tax = totalTransferencia.multiply(new BigDecimal("0.01"));
                BigDecimal netoParaVendedor = totalTransferencia.subtract(tax);

                economyManager.updateBalanceAsync(sellerId, NexoAccount.AccountType.PLAYER, NexoAccount.Currency.COINS, netoParaVendedor, true);

                enviarABuzonSync(conn, buyerId, itemId, cantidadIntercambiada, BigDecimal.ZERO);

                actualizarOrden(conn, buyId, buyAmount - cantidadIntercambiada);
                actualizarOrden(conn, sellId, sellAmount - cantidadIntercambiada);

                plugin.getLogger().info("📈 [BAZAR] Cruce exitoso de " + cantidadIntercambiada + "x " + itemId + " por " + totalTransferencia.toPlainString() + " Monedas.");

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

            Player p = Bukkit.getPlayer(ownerId);
            if (p != null && (amount > 0 || coins.compareTo(BigDecimal.ZERO) > 0)) {
                // 🌟 FOLIA SYNC
                p.getScheduler().run(plugin, task -> {
                    crossplayUtils.sendMessage(p, "&#555555--------------------------------");
                    crossplayUtils.sendMessage(p, "&#FFAA00📦 <bold>ENTREGA DEL BAZAR</bold>");
                    crossplayUtils.sendMessage(p, "&#E6CCFFUna de tus órdenes se ha completado.");
                    crossplayUtils.sendMessage(p, "&#E6CCFFRevisa tus entregas en el menú del Bazar.");
                    crossplayUtils.sendMessage(p, "&#555555--------------------------------");
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.2f);
                }, null);
            }
        }
    }

    private void quitarItems(Player player, Material mat, int amountToRemove) {
        int removed = 0;
        var contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            var item = contents[i];
            if (item != null && !item.isEmpty() && item.getType() == mat) {
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
        virtualExecutor.execute(() -> {
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
                        economyManager.updateBalanceAsync(player.getUniqueId(), NexoAccount.AccountType.PLAYER, NexoAccount.Currency.COINS, coins, true);
                        
                        player.getScheduler().run(plugin, task -> {
                            crossplayUtils.sendMessage(player, "&#55FF55[✓] <bold>BAZAR:</bold> &#E6CCFFHas reclamado &#FFAA00+" + coins.toPlainString() + " Monedas&#E6CCFF.");
                            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                        }, null);
                    } else {
                        player.getScheduler().run(plugin, task -> {
                            Material mat = Material.matchMaterial(itemId);
                            if (mat != null) {
                                var item = new ItemStack(mat, amount);
                                if (player.getInventory().firstEmpty() == -1) {
                                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                                } else {
                                    player.getInventory().addItem(item);
                                }
                                crossplayUtils.sendMessage(player, "&#55FF55[✓] <bold>BAZAR:</bold> &#E6CCFFHas reclamado &#00f5ff" + amount + "x " + mat.name() + "&#E6CCFF.");
                                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
                            }
                        }, null);
                    }
                }

                if (!tieneCosas) {
                    player.getScheduler().run(plugin, task -> crossplayUtils.sendMessage(player, "&#FF5555[!] Tu buzón de entregas está vacío."), null);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("❌ Error reclamando buzón: " + e.getMessage());
            }
        });
    }

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

    // 🌟 REFACTOR A RECORD DE JAVA 21
    public record ActiveOrderDTO(int id, String itemId, int amount, BigDecimal price, String type) {}

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

    public void cancelarOrden(Player player, int orderId) {
        virtualExecutor.execute(() -> {
            try (var conn = db.getConnection()) {
                conn.setAutoCommit(false); 

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
                        BigDecimal totalCoins = price.multiply(BigDecimal.valueOf(amount));
                        enviarABuzonSync(conn, player.getUniqueId(), "COINS", 0, totalCoins);
                    }

                    conn.commit(); 

                    player.getScheduler().run(plugin, task -> {
                        crossplayUtils.sendMessage(player, "&#55FF55[✓] <bold>BAZAR:</bold> &#E6CCFFOrden cancelada. Revisa tu buzón de entregas.");
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);
                    }, null);
                } else {
                    conn.rollback();
                    player.getScheduler().run(plugin, task -> crossplayUtils.sendMessage(player, "&#FF5555[!] Esa orden no existe o ya fue completada."), null);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("❌ Error cancelando orden: " + e.getMessage());
            }
        });
    }

    // ==========================================
    // 🌟 MANEJO DE SESIONES DE CHAT (Desacople del Listener)
    // ==========================================
    public record ChatOrderSession(String itemId, String orderType) {}

    public void iniciarSesionChat(UUID playerId, String itemId, String orderType) {
        chatSessions.put(playerId, new ChatOrderSession(itemId, orderType));
    }

    public ChatOrderSession getChatSession(UUID playerId) {
        return chatSessions.get(playerId);
    }

    public void removeChatSession(UUID playerId) {
        chatSessions.remove(playerId);
    }

    public void saveMarketSync() {
        plugin.getLogger().info("✅ El estado del Bazar Global ya se encuentra sincronizado con la Base de Datos central.");
    }
}