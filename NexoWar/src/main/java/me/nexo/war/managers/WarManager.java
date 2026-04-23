package me.nexo.war.managers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.clans.core.ClanManager;
import me.nexo.clans.core.NexoClan;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.database.DatabaseManager;
import me.nexo.core.user.NexoUser;
import me.nexo.core.user.UserManager;
import me.nexo.war.NexoWar;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ⚔️ NexoWar - Gestor Táctico (Arquitectura Enterprise)
 * Rendimiento: Hilos Virtuales Administrados, Inyección Directa y Prevención de Ítems Fantasma.
 */
@Singleton
public class WarManager {

    private final NexoWar plugin;
    private final ConfigManager configManager;
    private final UserManager userManager;
    private final ClanManager clanManager;
    
    // 🌟 Sinergia Multi-Módulo (Obtenidos del Child Injector)
    private final DatabaseManager dbManager;
    private final CrossplayUtils crossplayUtils;

    private final Map<UUID, WarContract> guerrasActivas = new ConcurrentHashMap<>();
    private final NamespacedKey voidEssenceKey;

    // 🚀 Motor concurrente para I/O de Base de Datos
    private final ExecutorService virtualExecutor;

    private final long GRACE_PERIOD_MILLIS = 5 * 60 * 1000L;
    private final int KILLS_TO_WIN = 20;
    private final int COSTO_SUMINISTROS = 100;

    // 💉 PILAR 1: Inyección Directa y Limpia
    @Inject
    public WarManager(NexoWar plugin, ConfigManager configManager, UserManager userManager, 
                      ClanManager clanManager, DatabaseManager dbManager, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.userManager = userManager;
        this.clanManager = clanManager;
        this.dbManager = dbManager;
        this.crossplayUtils = crossplayUtils;
        
        this.voidEssenceKey = new NamespacedKey(plugin, "void_essence");
        this.virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

        iniciarRelojDeGuerras();
    }

    public void iniciarDesafio(Player leader, NexoClan atacante, NexoClan defensor, BigDecimal apuesta) {
        if (atacante.getBankBalance().compareTo(apuesta) < 0 || defensor.getBankBalance().compareTo(apuesta) < 0) {
            String msg = configManager.getMessages().errores().objetivoSinFondos().replace("%apuesta%", apuesta.toPlainString());
            crossplayUtils.sendMessage(leader, msg);
            return;
        }

        // 🌟 FIX ITEMS: Cobro Atómico y Seguro de Esencia (Anti-Ítems Fantasma)
        int contadorSuministros = 0;
        for (ItemStack item : leader.getInventory().getContents()) {
            if (item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(voidEssenceKey, PersistentDataType.BYTE)) {
                contadorSuministros += item.getAmount();
            }
        }

        if (contadorSuministros < COSTO_SUMINISTROS) {
            crossplayUtils.sendMessage(leader, configManager.getMessages().errores().sinEsenciaGuerra());
            crossplayUtils.sendMessage(leader, configManager.getMessages().errores().requisitoEsencia().replace("%costo%", String.valueOf(COSTO_SUMINISTROS)));
            return;
        }

        int faltanPorCobrar = COSTO_SUMINISTROS;
        ItemStack[] contents = leader.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (faltanPorCobrar <= 0) break;

            if (item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(voidEssenceKey, PersistentDataType.BYTE)) {
                if (item.getAmount() <= faltanPorCobrar) {
                    faltanPorCobrar -= item.getAmount();
                    leader.getInventory().setItem(i, null); // 🛡️ Borrado seguro en Bukkit (Elimina el ítem físico)
                } else {
                    item.setAmount(item.getAmount() - faltanPorCobrar);
                    faltanPorCobrar = 0;
                }
            }
        }

        // 🛡️ Uso directo de ClanManager inyectado
        atacante.withdrawMoney(apuesta.doubleValue());
        defensor.withdrawMoney(apuesta.doubleValue());
        clanManager.saveBankAsync(atacante);
        clanManager.saveBankAsync(defensor);

        UUID warId = UUID.randomUUID();
        WarContract contrato = new WarContract(
                warId, atacante.getId(), defensor.getId(), apuesta,
                System.currentTimeMillis(), WarContract.WarStatus.GRACE_PERIOD, 0, 0
        );

        guerrasActivas.put(warId, contrato);
        saveWarToDatabase(contrato);

        // 🌟 MODERNIZACIÓN PAPER 1.21.5: Broadcasts nativos con Kyori Component parsing
        for (String line : configManager.getMessages().alertas().pactoIniciado()) {
            String broadcast = line.replace("%atacante%", atacante.getName())
                    .replace("%defensor%", defensor.getName())
                    .replace("%total%", apuesta.multiply(BigDecimal.valueOf(2)).toPlainString());
            Bukkit.broadcast(crossplayUtils.parseCrossplay(null, broadcast));
        }
    }

    private void iniciarRelojDeGuerras() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                for (WarContract guerra : guerrasActivas.values()) {
                    if (guerra.status() == WarContract.WarStatus.GRACE_PERIOD) {
                        if (now - guerra.startTime() >= GRACE_PERIOD_MILLIS) {

                            WarContract activa = new WarContract(
                                    guerra.warId(), guerra.clanAtacante(), guerra.clanDefensor(),
                                    guerra.apuestaMonedas(), now, WarContract.WarStatus.ACTIVE, 0, 0
                            );
                            guerrasActivas.put(guerra.warId(), activa);
                            actualizarGuerraEnBD(activa);

                            for (String line : configManager.getMessages().alertas().guerraActiva()) {
                                Bukkit.broadcast(crossplayUtils.parseCrossplay(null, 
                                        line.replace("%kills%", String.valueOf(KILLS_TO_WIN))
                                ));
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Reloj del Main Thread: 1 segundo
    }

    public Optional<WarContract> getGuerraEntre(UUID clan1, UUID clan2) {
        return guerrasActivas.values().stream()
                .filter(w -> (w.clanAtacante().equals(clan1) && w.clanDefensor().equals(clan2)) ||
                        (w.clanAtacante().equals(clan2) && w.clanDefensor().equals(clan1)))
                .findFirst(); // Seguro, Stream trabaja bien con ConcurrentHashMap
    }

    public boolean estanEnGuerraActiva(UUID player1, UUID player2) {
        NexoUser u1 = userManager.getUserOrNull(player1);
        NexoUser u2 = userManager.getUserOrNull(player2);

        if (u1 == null || !u1.hasClan() || u2 == null || !u2.hasClan()) return false;

        Optional<WarContract> guerra = getGuerraEntre(u1.getClanId(), u2.getClanId());
        return guerra.isPresent() && guerra.get().status() == WarContract.WarStatus.ACTIVE;
    }

    public void registrarBaja(WarContract guerra, UUID clanAsesino, Player asesino, Player victima) {
        boolean esAtacante = clanAsesino.equals(guerra.clanAtacante());
        int killsA = guerra.killsAtacante() + (esAtacante ? 1 : 0);
        int killsD = guerra.killsDefensor() + (!esAtacante ? 1 : 0);

        WarContract actualizada = new WarContract(
                guerra.warId(), guerra.clanAtacante(), guerra.clanDefensor(),
                guerra.apuestaMonedas(), guerra.startTime(), guerra.status(), killsA, killsD
        );
        guerrasActivas.put(guerra.warId(), actualizada);

        int killsActuales = esAtacante ? killsA : killsD;
        crossplayUtils.sendMessage(asesino, configManager.getMessages().exito().bajaConfirmada()
                .replace("%actual%", String.valueOf(killsActuales))
                .replace("%meta%", String.valueOf(KILLS_TO_WIN)));

        if (killsA >= KILLS_TO_WIN || killsD >= KILLS_TO_WIN) {
            terminarGuerra(actualizada, clanAsesino);
        } else {
            actualizarGuerraEnBD(actualizada);
        }
    }

    private void terminarGuerra(WarContract guerra, UUID clanGanador) {
        WarContract guerraFinalizada = new WarContract(
                guerra.warId(), guerra.clanAtacante(), guerra.clanDefensor(),
                guerra.apuestaMonedas(), guerra.startTime(), WarContract.WarStatus.FINISHED,
                guerra.killsAtacante(), guerra.killsDefensor()
        );

        guerrasActivas.remove(guerraFinalizada.warId());
        actualizarGuerraEnBD(guerraFinalizada);

        // 🛡️ Uso directo de ClanManager inyectado
        clanManager.loadClanAsync(clanGanador, clan -> {
            if (clan != null) {
                BigDecimal premio = guerraFinalizada.apuestaMonedas().multiply(BigDecimal.valueOf(2));
                clan.depositMoney(premio.doubleValue());
                clanManager.saveBankAsync(clan);

                for (String line : configManager.getMessages().alertas().victoria()) {
                    Bukkit.broadcast(crossplayUtils.parseCrossplay(null, 
                            line.replace("%ganador%", clan.getName()).replace("%premio%", premio.toPlainString())
                    ));
                }
            }
        });
    }

    // 🌟 FIX VIRTUAL THREADS: Guardado de datos administrado sin consumir hilos de Bukkit
    private void saveWarToDatabase(WarContract war) {
        virtualExecutor.submit(() -> {
            String sql = "INSERT INTO nexo_wars (id, attacker_id, defender_id, bet_amount, status, kills_attacker, kills_defender) VALUES (CAST(? AS UUID), CAST(? AS UUID), CAST(? AS UUID), ?, ?, ?, ?)";
            try (java.sql.Connection conn = dbManager.getConnection();
                 java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, war.warId().toString());
                ps.setString(2, war.clanAtacante().toString());
                ps.setString(3, war.clanDefensor().toString());
                ps.setBigDecimal(4, war.apuestaMonedas());
                ps.setString(5, war.status().name());
                ps.setInt(6, war.killsAtacante());
                ps.setInt(7, war.killsDefensor());
                ps.executeUpdate();
            } catch (Exception ignored) {}
        });
    }

    private void actualizarGuerraEnBD(WarContract war) {
        virtualExecutor.submit(() -> {
            String sql = "UPDATE nexo_wars SET status = ?, kills_attacker = ?, kills_defender = ? WHERE id = CAST(? AS UUID)";
            try (java.sql.Connection conn = dbManager.getConnection();
                 java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, war.status().name());
                ps.setInt(2, war.killsAtacante());
                ps.setInt(3, war.killsDefensor());
                ps.setString(4, war.warId().toString());
                ps.executeUpdate();
            } catch (Exception ignored) {}
        });
    }
}