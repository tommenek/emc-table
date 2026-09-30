package dev.emctable;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** The packets the transmutation table uses. */
public final class Payloads {

    private Payloads() {
    }

    /** One learnt item, worked out server-side so the client just draws it. */
    public record Known(String id, String name, long value) {
    }

    /** Server -> client: the player's balance and everything they have learnt. */
    public record TableData(long balance, List<Known> known) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<TableData> TYPE =
                new CustomPacketPayload.Type<>(EmcTableMod.id("table_data"));

        public static final StreamCodec<FriendlyByteBuf, TableData> CODEC =
                StreamCodec.of(TableData::write, TableData::read);

        private static void write(FriendlyByteBuf buf, TableData payload) {
            buf.writeLong(payload.balance);
            buf.writeVarInt(payload.known.size());
            for (Known entry : payload.known) {
                buf.writeUtf(entry.id());
                buf.writeUtf(entry.name());
                buf.writeLong(entry.value());
            }
        }

        private static TableData read(FriendlyByteBuf buf) {
            long balance = buf.readLong();
            int size = buf.readVarInt();
            List<Known> known = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                known.add(new Known(buf.readUtf(), buf.readUtf(), buf.readLong()));
            }
            return new TableData(balance, known);
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> server: withdraw {@code count} of this item, paying its EMC. */
    public record Withdraw(String id, int count) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<Withdraw> TYPE =
                new CustomPacketPayload.Type<>(EmcTableMod.id("withdraw"));

        public static final StreamCodec<FriendlyByteBuf, Withdraw> CODEC =
                StreamCodec.of(Withdraw::write, Withdraw::read);

        private static void write(FriendlyByteBuf buf, Withdraw payload) {
            buf.writeUtf(payload.id);
            buf.writeVarInt(payload.count);
        }

        private static Withdraw read(FriendlyByteBuf buf) {
            return new Withdraw(buf.readUtf(), buf.readVarInt());
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
