package me.nexo.clans.core;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.clans.NexoClans;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.database.DatabaseManager;
import me.nexo.core.user.NexoAPI;
import me.nexo.core.user.NexoUser;
import me.nexo.core.user.UserManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 👥 NexoClans - Cerebro y Base de Datos (Arquitectura Enterprise Java 25)
 * Rendimiento: Hilos Virtuales, Transacciones ACID con Rollback y Cero Lag I/O.
 */
@Singleton
public class ClanManager {

    private final NexoClans plugin;

    // ⚡ CACHÉS DE ALTA VELOCIDAD O(1)
    private final Cache<UUID, NexoClan> clanCache = Caffeine.newBuilder()
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build();

    private final Cache<UUID, UUID> invitaciones = Caffeine.newBuilder()
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .build();

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public ClanManager(NexoClans plugin) {
        this.plugin = plugin;
        crearTablaClanes();
    }

    // ==========================================
    // 🗄️ INICIALIZACIÓN DE BASE DE DATOS
    // ==========================================
    private void crearTablaClanes() {
        Thread.startVirtualThread(() -> {
            NexoAPI.getServices().get(DatabaseManager.class).ifPresent(db -> {
                String sql = """
                        CREATE TABLE IF NOT EXISTS nexo_clans (
                            id UUID PRIMARY KEY,
                            name VARCHAR(32) UNIQUE NOT NULL,
                            tag VARCHAR(5) UNIQUE NOT NULL,
                            monolith_level INT DEFAULT 1,
                            monolith_exp BIGINT DEFAULT 0,
                            bank_balance DECIMAL(15,2) DEFAULT 0.00,
                            public_home TEXT DEFAULT NULL,
                            friendly_fire BOOLEAN DEFAULT FALSE
                        );
                        """;
                try (var conn = db.getConnection();
                     var stmt = conn.createStatement()) {

                    stmt.execute(sql);
                    try { stmt.execute("ALTER TABLE nexo_clans ADD COLUMN IF NOT EXISTS friendly_fire BOOLEAN DEFAULT FALSE;"); } catch (Exception ignored) {}

                } catch (Exception e) {
                    plugin.getLogger().severe("❌ Error creando tabla nexo_clans: " + e.getMessage());
                }
            });
        });
    }

    // ==========================================
    // ⚙️ OPERACIONES ASÍNCRONAS CON HILOS VIRTUALES
    // ==========================================
    public void setClanHomeAsync(NexoClan clan, Player player, Location loc) {
        String locStr = loc.getWorld().getName() + ";" + loc.getX() + ";" + loc.getY() + ";" + loc.getZ() + ";" + loc.getYaw() + ";" + loc.getPitch();

        Thread.startVirtualThread(() -> {
            NexoAPI.getServices().get(DatabaseManager.class).ifPresent(db -> {
                String sql = "UPDATE nexo_clans SET public_home = ? WHERE id = CAST(? AS UUID)";
                try (var conn = db.getConnection();
                     var ps = conn.prepareStatement(sql)) {

                    ps.setString(1, locStr);
                    ps.setString(2, clan.getId().toString());
                    ps.executeUpdate();

                    clan.setPublicHome(locStr);
                    CrossplayUtils.sendMessage(player, "&#55FF55[✓] <bold>BASE ESTABLECIDA:</bold> &#E6CCFFLa nueva ubicación del clan ha sido guardada.");
                } catch (Exception e) {
                    plugin.getLogger().severe("❌ Error guardando Base: " + e.getMessage());
                }
            });
        });
    }

    public void toggleFriendlyFireAsync(NexoClan clan, Player player, boolean newValue) {
        Thread.startVirtualThread(() -> {
            NexoAPI.getServices().get(DatabaseManager.class).ifPresent(db -> {
                String sql = "UPDATE nexo_clans SET friendly_fire = ? WHERE id = CAST(? AS UUID)";
                try (var conn = db.getConnection();
                     var ps = conn.prepareStatement(sql)) {

                    ps.setBoolean(1, newValue);
                    ps.setString(2, clan.getId().toString());
                    ps.executeUpdate();

                    clan.setFriendlyFire(newValue);
                    String colorStatus = newValue ? "&#FF3366<bold>RIESGO DE SANGRE ACTIVO</bold>" : "&#55FF55<bold>SEGURO Y APAGADO</bold>";
                    CrossplayUtils.sendMessage(player, "&#FFAA00[!] Fuego Aliado (Friendly Fire) cambiado a: " + colorStatus);
                } catch (Exception ignored) {}
            });
        });
    }

    public void getMiembrosAsync(UUID clanId, Consumer<List<ClanMember>> callback) {
        Thread.startVirtualThread(() -> {
            List<ClanMember> miembros = new ArrayList<>();
            NexoAPI.getServices().get(DatabaseManager.class).ifPresent(db -> {
                String sql = "SELECT uuid, name, clan_role FROM jugadores WHERE clan_id = CAST(? AS UUID) ORDER BY clan_role DESC";
                try (var conn = db.getConnection();
                     var ps = conn.prepareStatement(sql)) {

                    ps.setString(1, clanId.toString());
                    var rs = ps.executeQuery();
                    while (rs.next()) {
                        miembros.add(new ClanMember(
                                UUID.fromString(rs.getString("uuid")),
                                rs.getString("name"),
                                rs.getString("clan_role")
                        ));
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("❌ Error cargando miembros: " + e.getMessage());
                }
            });
            callback.accept(miembros);
        });
    }

    // 🌟 FIX ACID: Transacción atómica con Rollback de seguridad
    public void crearClanAsync(Player player, NexoUser user, String tag, String nombre) {
        Thread.startVirtualThread(() -> {
            NexoAPI.getServices().get(DatabaseManager.class).ifPresent(db -> {
                try (var conn = db.getConnection()) {
                    conn.setAutoCommit(false);

                    try {
                        String checkSQL = "SELECT id FROM nexo_clans WHERE tag = ? OR name = ?";
                        try (var psCheck = conn.prepareStatement(checkSQL)) {
                            psCheck.setString(1, tag);
                            psCheck.setString(2, nombre);
                            if (psCheck.executeQuery().next()) {
                                CrossplayUtils.sendMessage(player, "&#FF5555[!] Ese nombre o etiqueta de clan ya ha sido reclamado.");
                                conn.rollback(); // 🛡️ Rollback vital
                                return;
                            }
                        }

                        UUID nuevoClanId = UUID.randomUUID();
                        String insertClan = "INSERT INTO nexo_clans (id, name, tag) VALUES (CAST(? AS UUID), ?, ?)";
                        try (var psInsert = conn.prepareStatement(insertClan)) {
                            psInsert.setString(1, nuevoClanId.toString());
                            psInsert.setString(2, nombre);
                            psInsert.setString(3, tag);
                            psInsert.executeUpdate();
                        }

                        // 🌟 FIX POSTGRESQL: Casting UUID en el WHERE
                        String updateUser = "UPDATE jugadores SET clan_id = CAST(? AS UUID), clan_role = 'LIDER' WHERE uuid = CAST(? AS UUID)";
                        try (var psUser = conn.prepareStatement(updateUser)) {
                            psUser.setString(1, nuevoClanId.toString());
                            psUser.setString(2, player.getUniqueId().toString());
                            psUser.executeUpdate();
                        }

                        conn.commit(); // 🌟 Ambas queries exitosas, consolidamos.

                        // Actualizamos memoria RAM
                        NexoClan nuevoClan = new NexoClan(nuevoClanId, nombre, tag, 1, 0L, BigDecimal.ZERO, null, false);
                        clanCache.put(nuevoClanId, nuevoClan);
                        user.setClanId(nuevoClanId);
                        user.setClanRole("LIDER");

                        CrossplayUtils.sendMessage(player, "&#55FF55[✓] <bold>CLAN FUNDADO:</bold> &#E6CCFFLarga vida a &#FFAA00" + nombre + " [" + tag + "]&#E6CCFF!");

                    } catch (Exception innerEx) {
                        conn.rollback(); // 🛡️ Si algo falla, revertimos y prevenimos bloqueos en Supabase
                        throw innerEx;
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("❌ Error creando clan: " + e.getMessage());
                }
            });
        });
    }

    public void loadClanAsync(UUID clanId, Consumer<NexoClan> callback) {
        NexoClan cached = clanCache.getIfPresent(clanId);
        if (cached != null) {
            callback.accept(cached);
            return;
        }

        Thread.startVirtualThread(() -> {
            NexoAPI.getServices().get(DatabaseManager.class).ifPresent(db -> {
                String sql = "SELECT * FROM nexo_clans WHERE id = CAST(? AS UUID)";
                try (var conn = db.getConnection();
                     var ps = conn.prepareStatement(sql)) {

                    ps.setString(1, clanId.toString());
                    var rs = ps.executeQuery();
                    if (rs.next()) {
                        NexoClan loadedClan = new NexoClan(
                                clanId, rs.getString("name"), rs.getString("tag"),
                                rs.getInt("monolith_level"), rs.getLong("monolith_exp"),
                                rs.getBigDecimal("bank_balance"), rs.getString("public_home"),
                                rs.getBoolean("friendly_fire")
                        );
                        clanCache.put(clanId, loadedClan);
                        callback.accept(loadedClan);
                    } else {
                        callback.accept(null);
                    }
                } catch (Exception e) {
                    callback.accept(null);
                }
            });
        });
    }

    public void unirseClanAsync(Player player, NexoUser user, NexoClan clan) {
        Thread.startVirtualThread(() -> {
            NexoAPI.getServices().get(DatabaseManager.class).ifPresent(db -> {
                String updateSQL = "UPDATE jugadores SET clan_id = CAST(? AS UUID), clan_role = 'MIEMBRO' WHERE uuid = CAST(? AS UUID)";
                try (var conn = db.getConnection();
                     var ps = conn.prepareStatement(updateSQL)) {

                    ps.setString(1, clan.getId().toString());
                    ps.setString(2, player.getUniqueId().toString());
                    ps.executeUpdate();

                    user.setClanId(clan.getId());
                    user.setClanRole("MIEMBRO");
                    invitaciones.invalidate(player.getUniqueId());

                    CrossplayUtils.sendMessage(player, "&#55FF55[✓] Te has unido a las filas de &#FFAA00" + clan.getName() + "&#55FF55.");
                } catch (Exception ignored) {}
            });
        });
    }

    public void abandonarClanAsync(Player player, NexoUser user) {
        Thread.startVirtualThread(() -> {
            NexoAPI.getServices().get(DatabaseManager.class).ifPresent(db -> {
                String updateSQL = "UPDATE jugadores SET clan_id = NULL, clan_role = 'NONE' WHERE uuid = CAST(? AS UUID)";
                try (var conn = db.getConnection();
                     var ps = conn.prepareStatement(updateSQL)) {

                    ps.setString(1, player.getUniqueId().toString());
                    ps.executeUpdate();

                    user.setClanId(null);
                    user.setClanRole("NONE");
                    CrossplayUtils.sendMessage(player, "&#FF5555[!] Has desertado y abandonado tu clan.");
                } catch (Exception ignored) {}
            });
        });
    }

    public void expulsarJugadorAsync(Player ejector, Player target, NexoUser targetUser) {
        Thread.startVirtualThread(() -> {
            NexoAPI.getServices().get(DatabaseManager.class).ifPresent(db -> {
                String updateSQL = "UPDATE jugadores SET clan_id = NULL, clan_role = 'NONE' WHERE uuid = CAST(? AS UUID)";
                try (var conn = db.getConnection();
                     var ps = conn.prepareStatement(updateSQL)) {

                    ps.setString(1, target.getUniqueId().toString());
                    ps.executeUpdate();

                    targetUser.setClanId(null);
                    targetUser.setClanRole("NONE");

                    CrossplayUtils.sendMessage(target, "&#FF5555[!] Has sido exiliado del clan por " + ejector.getName() + ".");
                    CrossplayUtils.sendMessage(ejector, "&#55FF55[✓] Has expulsado a " + target.getName() + " del clan.");
                } catch (Exception ignored) {}
            });
        });
    }

    // 🌟 FIX: Transacción Atómica Completa y Protección AsyncCatcher
    public void disolverClanAsync(Player lider, NexoUser liderUser, UUID clanId) {
        Thread.startVirtualThread(() -> {
            NexoAPI.getServices().get(DatabaseManager.class).ifPresent(db -> {
                try (var conn = db.getConnection()) {
                    conn.setAutoCommit(false);

                    try {
                        String updateUsers = "UPDATE jugadores SET clan_id = NULL, clan_role = 'NONE' WHERE clan_id = CAST(? AS UUID)";
                        try (var ps = conn.prepareStatement(updateUsers)) {
                            ps.setString(1, clanId.toString());
                            ps.executeUpdate();
                        }

                        String deleteClan = "DELETE FROM nexo_clans WHERE id = CAST(? AS UUID)";
                        try (var ps = conn.prepareStatement(deleteClan)) {
                            ps.setString(1, clanId.toString());
                            ps.executeUpdate();
                        }

                        conn.commit();
                        clanCache.invalidate(clanId);

                        // 🌟 FIX ASYNCCATCHER: Volvemos al Main Thread ANTES de llamar a Bukkit.getOnlinePlayers()
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            NexoAPI.getServices().get(UserManager.class).ifPresent(userManager -> {
                                for (Player p : Bukkit.getOnlinePlayers()) {
                                    NexoUser u = userManager.getUserOrNull(p.getUniqueId());
                                    if (u != null && u.hasClan() && clanId.equals(u.getClanId())) {
                                        u.setClanId(null);
                                        u.setClanRole("NONE");
                                        CrossplayUtils.sendMessage(p, "&#FF5555[!] Tu clan ha sido disuelto permanentemente por el líder.");
                                    }
                                }
                            });
                        });
                    } catch (Exception innerEx) {
                        conn.rollback(); // 🛡️ Rollback vital para la salud de Supabase
                        throw innerEx;
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("❌ Error disolviendo clan: " + e.getMessage());
                }
            });
        });
    }

    // ==========================================
    // 📨 GESTIÓN DE INVITACIONES (RAM)
    // ==========================================
    public void invitarJugador(Player lider, Player invitado, NexoClan clan) {
        invitaciones.put(invitado.getUniqueId(), clan.getId());
        CrossplayUtils.sendMessage(invitado, "&#FFAA00✉ <bold>CARTA DE RECLUTAMIENTO:</bold> &#E6CCFFEl clan &#55FF55" + clan.getName() + " &#E6CCFFte ha invitado a unirte. Escribe /clan join para aceptar.");
        CrossplayUtils.sendMessage(lider, "&#55FF55[✓] Invitación enviada a " + invitado.getName() + ".");
    }

    public UUID getInvitacionPendiente(Player player) { return invitaciones.getIfPresent(player.getUniqueId()); }
    public Optional<NexoClan> getClanFromCache(UUID clanId) { return Optional.ofNullable(clanCache.getIfPresent(clanId)); }

    // ==========================================
    // 💾 GUARDADOS CRÍTICOS
    // ==========================================
    public void saveBankAsync(NexoClan clan) {
        Thread.startVirtualThread(() -> {
            NexoAPI.getServices().get(DatabaseManager.class).ifPresent(db -> {
                String sql = "UPDATE nexo_clans SET bank_balance = ? WHERE id = CAST(? AS UUID)";
                try (var conn = db.getConnection();
                     var ps = conn.prepareStatement(sql)) {
                    ps.setBigDecimal(1, clan.getBankBalance());
                    ps.setString(2, clan.getId().toString());
                    ps.executeUpdate();
                } catch (Exception e) {
                    plugin.getLogger().severe("❌ Error guardando el banco del clan: " + e.getMessage());
                }
            });
        });
    }

    public void saveAllClansSync() {
        NexoAPI.getServices().get(DatabaseManager.class).ifPresent(db -> {
            String sql = "UPDATE nexo_clans SET monolith_exp = ?, monolith_level = ?, bank_balance = ?, public_home = ?, friendly_fire = ? WHERE id = CAST(? AS UUID)";
            try (var conn = db.getConnection();
                 var ps = conn.prepareStatement(sql)) {

                conn.setAutoCommit(false);

                for (NexoClan clan : clanCache.asMap().values()) {
                    ps.setLong(1, clan.getMonolithExp());
                    ps.setInt(2, clan.getMonolithLevel());
                    ps.setBigDecimal(3, clan.getBankBalance());
                    ps.setString(4, clan.getPublicHome());
                    ps.setBoolean(5, clan.isFriendlyFire());
                    ps.setString(6, clan.getId().toString());
                    ps.addBatch();
                }

                ps.executeBatch();
                conn.commit();
                plugin.getLogger().info("💾 [AUTO-SAVE] Progreso de todos los clanes guardado en lote.");
            } catch (Exception e) {
                plugin.getLogger().severe("❌ Error en el guardado síncrono de clanes: " + e.getMessage());
            }
        });
    }
}