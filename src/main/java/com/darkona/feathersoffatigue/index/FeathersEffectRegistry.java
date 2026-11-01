package com.darkona.feathersoffatigue.index;

import com.darkona.feathersoffatigue.content.feathers.EnduranceEffect;
import com.darkona.feathersoffatigue.content.feathers.FeathersMobEffect;
import com.darkona.feathersoffatigue.api.registry.FeathersIds;
import com.darkona.feathersoffatigue.api.registry.FeathersMobEffects;
import com.darkona.feathersoffatigue.compat.coldsweat.ColdSweatCompat;
import com.darkona.feathersoffatigue.config.ServerConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

import static com.darkona.feathersoffatigue.api.registry.FeathersAttributes.FEATHERS_PER_SECOND;
import static com.darkona.feathersoffatigue.api.registry.FeathersAttributes.MAX_FEATHERS;
import static com.darkona.feathersoffatigue.api.registry.FeathersAttributes.USAGE_MULTIPLIER;
import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;
import static net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION;
import static net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_TOTAL;

public final class FeathersEffectRegistry {
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, FeathersIds.MOD_ID);

    static {
        EFFECTS.register("endurance", EnduranceEffect::new);

        EFFECTS.register("cold", () -> new FeathersMobEffect(MobEffectCategory.HARMFUL, 0xB6FFFF) {
            @Override
            public boolean canApply(LivingEntity entity) {
                if (!super.canApply(entity) || !ServerConfig.ENABLE_COLD.get()) return false;
                if (!ColdSweatCompat.canApplyCold(entity)) return false;

                return !entity.hasEffect(FeathersMobEffects.ENERGIZED.get()) && !entity.hasEffect(FeathersMobEffects.HOT.get());
            }
        }.withModifier(FEATHERS_PER_SECOND, id("effect.cold"), -0.5, MULTIPLY_TOTAL));

        EFFECTS.register("energized", () -> new FeathersMobEffect(MobEffectCategory.BENEFICIAL, 0x71CEFF) {
            @Override
            public boolean canApply(LivingEntity entity) {
                return super.canApply(entity) && ServerConfig.ENABLE_ENERGIZED.get();
            }
        }.withModifier(FEATHERS_PER_SECOND, id("effect.energized"), 1.0, MULTIPLY_TOTAL));

        EFFECTS.register("hot", () -> new FeathersMobEffect(MobEffectCategory.HARMFUL, 0x7E5D48) {
            @Override
            public boolean canApply(LivingEntity entity) {
                if (!super.canApply(entity) || !ServerConfig.ENABLE_HEAT.get() || isProtectedFromHeat(entity)) return false;
                if (!ColdSweatCompat.canApplyHeat(entity)) return false;
                return !entity.hasEffect(FeathersMobEffects.MOMENTUM.get()) && !entity.hasEffect(FeathersMobEffects.COLD.get());
            }
        }.withModifier(USAGE_MULTIPLIER, id("effect.hot"), 1.0, ADDITION));

        EFFECTS.register("fatigued", () -> new FeathersMobEffect(MobEffectCategory.HARMFUL, 0xFF0048) {
            @Override
            public boolean canApply(LivingEntity entity) {
                return super.canApply(entity) && ServerConfig.ENABLE_FATIGUE.get() && !isProtectedFromHeat(entity);
            }
        }.withModifier(MAX_FEATHERS, id("effect.fatigued"), -4.0, ADDITION));

        EFFECTS.register("momentum", () -> new FeathersMobEffect(MobEffectCategory.BENEFICIAL, 0x7E5684) {
            @Override
            public boolean canApply(LivingEntity entity) {
                return super.canApply(entity) && ServerConfig.ENABLE_MOMENTUM.get();
            }

            @Override
            public double getAttributeModifierValue(int amplifier, AttributeModifier modifier) {
                return Math.max(-0.8, -0.5 - 0.15 * amplifier);
            }
        }.withModifier(USAGE_MULTIPLIER, id("effect.momentum"), -0.5, MULTIPLY_TOTAL));

        EFFECTS.register("strain", () -> new FeathersMobEffect(MobEffectCategory.HARMFUL, 0x7E4488) {
            @Override
            public boolean canApply(LivingEntity entity) {
                return super.canApply(entity) && ServerConfig.ENABLE_STRAIN.get();
            }
        }.withModifier(FEATHERS_PER_SECOND, id("effect.strain"), -0.75, MULTIPLY_TOTAL));

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

    public static void bindAttributeModifiers() {
        for (RegistryObject<MobEffect> effect : EFFECTS.getEntries()) {
            if (effect.get() instanceof FeathersMobEffect feathers) feathers.bindModifiers();
        }
    }

    private FeathersEffectRegistry() {}
}
