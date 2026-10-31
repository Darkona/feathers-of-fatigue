package com.darkona.feathersoffatigue.index;

import com.darkona.feathersoffatigue.index.FeathersEffectRegistry;
import com.darkona.feathersoffatigue.content.feathers.FeathersMobEffect;
import com.darkona.feathersoffatigue.content.feathers.EnduranceEffect;
import com.darkona.feathersoffatigue.api.registry.FeathersIds;
import com.darkona.feathersoffatigue.compat.coldsweat.ColdSweatCompat;
import com.darkona.feathersoffatigue.config.ServerConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.darkona.feathersoffatigue.api.registry.FeathersAttributes.FEATHERS_PER_SECOND;
import static com.darkona.feathersoffatigue.api.registry.FeathersAttributes.MAX_FEATHERS;
import static com.darkona.feathersoffatigue.api.registry.FeathersAttributes.USAGE_MULTIPLIER;
import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;
import static com.darkona.feathersoffatigue.api.registry.FeathersMobEffects.COLD;
import static com.darkona.feathersoffatigue.api.registry.FeathersMobEffects.ENERGIZED;
import static com.darkona.feathersoffatigue.api.registry.FeathersMobEffects.HOT;
import static com.darkona.feathersoffatigue.api.registry.FeathersMobEffects.MOMENTUM;
import static net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
import static net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE;

public final class FeathersEffectRegistry {

    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, FeathersIds.MOD_ID);

    static {
        EFFECTS.register("endurance", EnduranceEffect::new);

        EFFECTS.register("cold", () -> new FeathersMobEffect(MobEffectCategory.HARMFUL, 0xB6FFFF) {
            @Override
            public boolean canApply(LivingEntity entity) {
                if (!super.canApply(entity) || !ServerConfig.ENABLE_COLD.get()) return false;
                if (!ColdSweatCompat.canApplyCold(entity)) return false;
                return !entity.hasEffect(ENERGIZED) && !entity.hasEffect(HOT);
            }
        }.addAttributeModifier(FEATHERS_PER_SECOND, id("effect.cold"), -0.5, ADD_MULTIPLIED_TOTAL));

        EFFECTS.register("energized", () -> new FeathersMobEffect(MobEffectCategory.BENEFICIAL, 0x71CEFF) {
            @Override
            public boolean canApply(LivingEntity entity) {
                return super.canApply(entity) && ServerConfig.ENABLE_ENERGIZED.get();
            }
        }.addAttributeModifier(FEATHERS_PER_SECOND, id("effect.energized"), 1.0, ADD_MULTIPLIED_TOTAL));

        EFFECTS.register("hot", () -> new FeathersMobEffect(MobEffectCategory.HARMFUL, 0x7E5D48) {
            @Override
            public boolean canApply(LivingEntity entity) {
                if (!super.canApply(entity) || !ServerConfig.ENABLE_HEAT.get() || isProtectedFromHeat(entity)) return false;
                if (!ColdSweatCompat.canApplyHeat(entity)) return false;
                return !entity.hasEffect(MOMENTUM) && !entity.hasEffect(COLD);
            }
        }.addAttributeModifier(USAGE_MULTIPLIER, id("effect.hot"), 1.0, ADD_VALUE));

        EFFECTS.register("fatigued", () -> new FeathersMobEffect(MobEffectCategory.HARMFUL, 0xFF0048) {
            @Override
            public boolean canApply(LivingEntity entity) {
                return super.canApply(entity) && ServerConfig.ENABLE_FATIGUE.get() && !isProtectedFromHeat(entity);
            }
        }.addAttributeModifier(MAX_FEATHERS, id("effect.fatigued"), -4.0, ADD_VALUE));

        EFFECTS.register("momentum", () -> new FeathersMobEffect(MobEffectCategory.BENEFICIAL, 0x7E5684) {
            @Override
            public boolean canApply(LivingEntity entity) {
                return super.canApply(entity) && ServerConfig.ENABLE_MOMENTUM.get();
            }
        }.addAttributeModifier(USAGE_MULTIPLIER, id("effect.momentum"), ADD_MULTIPLIED_TOTAL, amplifier -> Math.max(-0.8, -0.5 - 0.15 * amplifier)));

        EFFECTS.register("strain", () -> new FeathersMobEffect(MobEffectCategory.HARMFUL, 0x7E4488) {
            @Override
            public boolean canApply(LivingEntity entity) {
                return super.canApply(entity) && ServerConfig.ENABLE_STRAIN.get();
            }
        }.addAttributeModifier(FEATHERS_PER_SECOND, id("effect.strain"), -0.75, ADD_MULTIPLIED_TOTAL));

        EFFECTS.register("cooling", () -> new FeathersMobEffect(MobEffectCategory.BENEFICIAL, 0x5FE0B4) {
            @Override
            public boolean canApply(LivingEntity entity) {
                return super.canApply(entity) && ServerConfig.ENABLE_COOLING.get();
            }
        });
    }

    public static void register(IEventBus modEventBus) {
        EFFECTS.register(modEventBus);
    }

    private FeathersEffectRegistry() {}
}
