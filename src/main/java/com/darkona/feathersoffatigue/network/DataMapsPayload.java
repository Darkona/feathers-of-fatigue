package com.darkona.feathersoffatigue.network;

import com.darkona.feathersoffatigue.api.MountStats;
import com.darkona.feathersoffatigue.index.FeathersDataMapRegistry;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

public record DataMapsPayload(FeathersDataMapRegistry.Raw raw) {
    private static final int MAX_ENTRIES = 1 << 16;

    void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(raw.armor().size());
        for (FeathersDataMapRegistry.ArmorEntry entry : raw.armor()) {
            buf.writeUtf(entry.key());
            buf.writeVarInt(entry.weight());
        }
        buf.writeVarInt(raw.mounts().size());
        for (FeathersDataMapRegistry.MountEntry entry : raw.mounts()) {
            buf.writeUtf(entry.key());
            buf.writeJsonWithCodec(MountStats.CODEC, entry.stats());
        }
    }

    static DataMapsPayload decode(FriendlyByteBuf buf) {
        int armorCount = count(buf);
        List<FeathersDataMapRegistry.ArmorEntry> armor = new ArrayList<>(armorCount);
        for (int i = 0; i < armorCount; i++) armor.add(new FeathersDataMapRegistry.ArmorEntry(buf.readUtf(), buf.readVarInt()));
        int mountCount = count(buf);
        List<FeathersDataMapRegistry.MountEntry> mounts = new ArrayList<>(mountCount);
        for (int i = 0; i < mountCount; i++) mounts.add(new FeathersDataMapRegistry.MountEntry(buf.readUtf(), buf.readJsonWithCodec(MountStats.CODEC)));
        return new DataMapsPayload(new FeathersDataMapRegistry.Raw(armor, mounts));
    }

    private static int count(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        if (count < 0 || count > MAX_ENTRIES) throw new DecoderException("Bad data map size " + count);
        return count;
    }
}
