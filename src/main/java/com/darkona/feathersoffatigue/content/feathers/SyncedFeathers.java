package com.darkona.feathersoffatigue.content.feathers;

import com.darkona.feathersoffatigue.api.RestState;
import com.darkona.feathersoffatigue.content.weight.WeightSplit;
import com.darkona.feathersoffatigue.index.FeathersAttachmentRegistry;
import com.darkona.feathersoffatigue.network.SyncPayload;
import dev.darkona.jdlib.api.stat.JDResources;
import dev.darkona.jdlib.core.stat.Resource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class SyncedFeathers extends StaminaView {

    private static final WeightSplit NO_SPLIT = new WeightSplit();

    protected @Nullable LivingEntity entity;
    protected @Nullable Resource<ResourceLocation> resource;
    private int extrasOf = -1;
    private RestState rest = RestState.NONE;
    private final WeightSplit weightSplit = new WeightSplit();

    void apply(SyncPayload p) {
        extrasOf = p.entityId();
        rest = p.rest();
        weightSplit.readFrom(p.weightSplit());
    }

    void follow(@Nullable LivingEntity followed) {
        entity = followed;
        resource = followed != null && JDResources.holds(followed, FeathersAttachmentRegistry.STAMINA)
                ? JDResources.of(followed, FeathersAttachmentRegistry.STAMINA) : null;
    }

    void predict() {
        LivingEntity e = entity;
        if (e != null && resource != null) JDResources.tick(e, FeathersAttachmentRegistry.STAMINA, e.level());
    }

    void clear() {
        entity = null;
        resource = null;
        extrasOf = -1;
        rest = RestState.NONE;
        weightSplit.clear();
    }

    private boolean extrasMatch() {
        LivingEntity e = entity;
        return e != null && e.getId() == extrasOf;
    }

    public @Nullable LivingEntity entity() {
        return entity;
    }

    @SuppressWarnings("unused")
    public int weightPart(int part) {
        return weightSplit().part(part);
    }

    public WeightSplit weightSplit() {
        return extrasMatch() ? weightSplit : NO_SPLIT;
    }

    @Override
    protected @Nullable Resource<ResourceLocation> resource() {
        return resource;
    }

    @Override
    public RestState restState() {
        return extrasMatch() ? rest : RestState.NONE;
    }
}
