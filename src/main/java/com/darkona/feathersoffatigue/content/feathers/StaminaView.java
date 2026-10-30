package com.darkona.feathersoffatigue.content.feathers;

import com.darkona.feathersoffatigue.api.FeathersView;
import com.darkona.feathersoffatigue.api.Stamina;
import dev.darkona.jdlib.core.stat.Resource;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public abstract class StaminaView implements FeathersView {

    protected abstract @Nullable Resource<ResourceLocation> resource();

    @Override
    public boolean hasFeathers() {
        return resource() != null;
    }

    @Override
    public int stamina() {
        Resource<ResourceLocation> r = resource();
        return r != null ? r.stored() : 0;
    }

    @Override
    public int maxStamina() {
        Resource<ResourceLocation> r = resource();
        return r != null ? r.capacity() : 0;
    }

    @Override
    public int availableStamina() {
        Resource<ResourceLocation> r = resource();
        return r != null ? r.available() : 0;
    }

    @Override
    public int weight() {
        Resource<ResourceLocation> r = resource();
        return r != null ? Stamina.toFeathersCeil(r.reserved()) : 0;
    }

    @Override
    public int strain() {
        Resource<ResourceLocation> r = resource();
        return r != null ? r.overdraw() : 0;
    }

    @Override
    public int maxStrain() {
        Resource<ResourceLocation> r = resource();
        return r != null ? r.overdrawMax() : 0;
    }

    @Override
    public int bonusStamina() {
        Resource<ResourceLocation> r = resource();
        return r != null ? r.bonus() : 0;
    }

    @Override
    public int regenDelay() {
        Resource<ResourceLocation> r = resource();
        return r != null ? r.regenDelay() : 0;
    }

    @Override
    public boolean exhausted() {
        Resource<ResourceLocation> r = resource();
        return r != null && r.exhausted();
    }
}
