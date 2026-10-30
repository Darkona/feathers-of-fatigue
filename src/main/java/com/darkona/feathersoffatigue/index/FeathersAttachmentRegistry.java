package com.darkona.feathersoffatigue.index;

import com.darkona.feathersoffatigue.api.Stamina;
import com.darkona.feathersoffatigue.api.registry.FeathersAttributes;
import com.darkona.feathersoffatigue.content.feathers.FeathersData;
import com.darkona.feathersoffatigue.api.registry.FeathersIds;
import com.darkona.feathersoffatigue.content.feathers.StaminaHooks;
import dev.darkona.jdlib.api.stat.JDResources;
import dev.darkona.jdlib.api.stat.ResourceKind;
import dev.darkona.jdlib.core.stat.Resource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class FeathersAttachmentRegistry {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, FeathersIds.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<FeathersData>> FEATHERS =
            ATTACHMENT_TYPES.register("feathers", () -> AttachmentType.serializable(FeathersData::new).build());

    public static final ResourceKind<LivingEntity> STAMINA = JDResources.declare(ResourceKind.builder(FeathersIds.id("stamina"), LivingEntity.class)
            .rules(StaminaHooks::rules)
            .scale(Stamina.PER_FEATHER)
            .capacity(FeathersAttributes.MAX_FEATHERS)
            .reserved(FeathersAttributes.ARMOR_WEIGHT)
            .overdrawMax(FeathersAttributes.MAX_STRAIN)
            .rate(FeathersAttachmentRegistry::regenPerTick)
            .hooks(StaminaHooks.INSTANCE)
            .step(Stamina.PER_FEATHER)
            .ownerTicks()
            .build());

    private static long regenPerTick(LivingEntity entity) {
        AttributeInstance regen = entity.getAttribute(FeathersAttributes.FEATHERS_PER_SECOND);
        return regen != null ? Resource.perSecond(regen.getValue() * Stamina.PER_FEATHER) : 0L;
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }

    private FeathersAttachmentRegistry() {}
}
