package dev.emctable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Everyone's EMC balance and the items they have learnt, saved with the world. */
public class EmcData extends SavedData {

    public static final Codec<EmcData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.LONG).fieldOf("emc").forGetter(data -> data.rawEmc()),
            Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf()).fieldOf("knowledge")
                    .forGetter(data -> data.rawKnowledge())
    ).apply(instance, EmcData::new));

    public static final SavedDataType<EmcData> TYPE =
            new SavedDataType<>("emctable", EmcData::new, CODEC);

    private final Map<UUID, Long> emc = new HashMap<>();
    private final Map<UUID, Set<String>> knowledge = new HashMap<>();

    public EmcData() {
    }

    private EmcData(Map<String, Long> savedEmc, Map<String, List<String>> savedKnowledge) {
        savedEmc.forEach((id, value) -> emc.put(UUID.fromString(id), value));
        savedKnowledge.forEach((id, items) -> knowledge.put(UUID.fromString(id), new HashSet<>(items)));
    }

    private Map<String, Long> rawEmc() {
        Map<String, Long> out = new HashMap<>();
        emc.forEach((id, value) -> out.put(id.toString(), value));
        return out;
    }

    private Map<String, List<String>> rawKnowledge() {
        Map<String, List<String>> out = new HashMap<>();
        knowledge.forEach((id, items) -> out.put(id.toString(), new ArrayList<>(items)));
        return out;
    }

    /** The one instance for this world, kept on the overworld. */
    public static EmcData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public long balance(ServerPlayer player) {
        return emc.getOrDefault(player.getUUID(), 0L);
    }

    public void add(ServerPlayer player, long amount) {
        emc.merge(player.getUUID(), amount, Long::sum);
        setDirty();
    }

    /** Spends EMC if the player has enough. Returns false and changes nothing otherwise. */
    public boolean spend(ServerPlayer player, long amount) {
        long have = balance(player);
        if (amount <= 0 || have < amount) {
            return false;
        }
        emc.put(player.getUUID(), have - amount);
        setDirty();
        return true;
    }

    public Set<String> learned(ServerPlayer player) {
        return knowledge.getOrDefault(player.getUUID(), Set.of());
    }

    public boolean knows(ServerPlayer player, String itemId) {
        return learned(player).contains(itemId);
    }

    /** Returns true if this was newly learnt. */
    public boolean learn(ServerPlayer player, String itemId) {
        boolean added = knowledge.computeIfAbsent(player.getUUID(), id -> new HashSet<>()).add(itemId);
        if (added) {
            setDirty();
        }
        return added;
    }
}
