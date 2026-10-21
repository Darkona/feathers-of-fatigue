package com.darkona.feathersoffatigue.api.client;

import com.darkona.feathersoffatigue.api.FeathersView;
import dev.darkona.jdlib.core.registry.PriorityList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class FeatherAnimations {

    public static final int STATUS_PRIORITY = 0;

    private static final PriorityList<ResourceLocation, FeatherAnimationProvider> PROVIDERS =
            new PriorityList<>(PriorityList.Order.HIGHEST_FIRST, PriorityList.Ties.REGISTRATION);

    private FeatherAnimations() {}

    public static void registerProvider(ResourceLocation id, int priority, FeatherAnimationProvider provider) {
        PROVIDERS.put(Objects.requireNonNull(id), priority, Objects.requireNonNull(provider));
    }

    public static @Nullable FeatherAnimation select(LivingEntity entity, FeathersView feathers) {
        PriorityList.Snapshot<ResourceLocation, FeatherAnimationProvider> providers = PROVIDERS.snapshot();
        for (int i = 0; i < providers.size(); i++) {
            FeatherAnimation animation = providers.value(i).animationFor(entity, feathers);
            if (animation != null) return animation;
        }
        return null;
    }
}
