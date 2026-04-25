package me.nexo.minions.data;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.UUID;

/**
 * 🔑 NexoMinions - Llaves y Componentes de Datos Nativos (Paper 1.21+)
 * Rendimiento: Traducción binaria directa usando Custom PersistentDataType.
 */
public final class MinionKeys {

    private static final String NAMESPACE = "nexominions";

    // ==========================================
    // 🗝️ LLAVES DEL PDC (Entidades/UI)
    // ==========================================
    public static final NamespacedKey HOLO_ID = new NamespacedKey(NAMESPACE, "minion_holo_id");
    public static final NamespacedKey INTERACTION_ID = new NamespacedKey(NAMESPACE, "minion_interaction_id");
    public static final NamespacedKey PLACED_LIMIT = new NamespacedKey(NAMESPACE, "minions_placed_limit");

    // Slots de Upgrades
    public static final NamespacedKey[] UPGRADES = new NamespacedKey[4];

    static {
        for (int i = 0; i < 4; i++) {
            UPGRADES[i] = new NamespacedKey(NAMESPACE, "minion_upgrade_" + i);
        }
    }

    // ==========================================
    // 🧬 ADN BINARIO CUSTOM (ALTO RENDIMIENTO)
    // ==========================================
    public static final NamespacedKey DNA_KEY = new NamespacedKey(NAMESPACE, "minion_dna");

    public static final PersistentDataType<byte[], MinionDNA> DNA_TYPE = new PersistentDataType<>() {
        @Override
        public @NotNull Class<byte[]> getPrimitiveType() {
            return byte[].class;
        }

        @Override
        public @NotNull Class<MinionDNA> getComplexType() {
            return MinionDNA.class;
        }

        @Override
        public byte @NotNull [] toPrimitive(@NotNull MinionDNA dna, @NotNull PersistentDataAdapterContext context) {
            // Convierte el Dna a Bytes puros (Cero Lag)
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 DataOutputStream dos = new DataOutputStream(baos)) {

                dos.writeLong(dna.ownerId().getMostSignificantBits());
                dos.writeLong(dna.ownerId().getLeastSignificantBits());
                dos.writeUTF(dna.type().name());
                dos.writeInt(dna.tier());
                dos.writeDouble(dna.speedMutation());
                dos.writeDouble(dna.strikeProbability());
                dos.writeDouble(dna.fatigueResistance());
                dos.writeInt(dna.storedItems());
                dos.writeLong(dna.nextActionTime());

                return baos.toByteArray();
            } catch (IOException e) {
                throw new RuntimeException("Error fatal serializando el ADN del Minion", e);
            }
        }

        @Override
        public @NotNull MinionDNA fromPrimitive(byte @NotNull [] bytes, @NotNull PersistentDataAdapterContext context) {
            // Reconstruye el DNA desde los bytes
            try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                 DataInputStream dis = new DataInputStream(bais)) {

                UUID ownerId = new UUID(dis.readLong(), dis.readLong());
                MinionType type = MinionType.valueOf(dis.readUTF());
                int tier = dis.readInt();
                double speed = dis.readDouble();
                double strike = dis.readDouble();
                double fatigue = dis.readDouble();
                int stored = dis.readInt();
                long nextAction = dis.readLong();

                return new MinionDNA(ownerId, type, tier, speed, strike, fatigue, stored, nextAction);
            } catch (IOException e) {
                throw new RuntimeException("Error fatal deserializando el ADN del Minion", e);
            }
        }
    };

    private MinionKeys() {
        throw new UnsupportedOperationException("Clase estática.");
    }
}