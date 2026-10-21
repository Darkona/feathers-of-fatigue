package com.darkona.feathersoffatigue.effect;

import com.darkona.feathersoffatigue.api.FeathersAPI;
import com.darkona.feathersoffatigue.api.Stamina;
import com.darkona.feathersoffatigue.api.registry.FeathersIds;
import com.darkona.feathersoffatigue.api.registry.FeathersMobEffects;
import com.darkona.feathersoffatigue.compatibility.coldsweat.ColdSweatCompat;
import com.darkona.feathersoffatigue.config.FeathersServerConfig;
import com.darkona.feathersoffatigue.core.FeathersServiceImpl;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

import static com.darkona.feathersoffatigue.api.registry.FeathersAttributes.FEATHERS_PER_SECOND;
import static com.darkona.feathersoffatigue.api.registry.FeathersAttributes.MAX_FEATHERS;
import static com.darkona.feathersoffatigue.api.registry.FeathersAttributes.USAGE_MULTIPLIER;
import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;
import static net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
import static net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE;


public final class ModEffects {

    public static final ResourceLocation ENDURANCE_BONUS = id("endurance_effect");

    private static int enduranceBonus(MobEffectInstance instance) {
        return Stamina.ofFeathers((instance.getAmplifier() + 1) * 8);
    }

    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, FeathersIds.MOD_ID);

    static {
        EFFECTS.register("endurance", () -> new FeathersMobEffect(MobEffectCategory.BENEFICIAL, 0xFFFF00) {
            @Override
            public boolean canApply(LivingEntity entity) {
                return super.canApply(entity) && FeathersServerConfig.ENABLE_ENDURANCE.get();
            }

            @Override
            public void onApplied(LivingEntity entity, MobEffectInstance instance, @Nullable MobEffectInstance previous) {
                if (!FeathersAPI.hasFeathers(entity)) return;
                int full = enduranceBonus(instance);
                int left = FeathersServiceImpl.data(entity).bonusStamina(ENDURANCE_BONUS);
                int amount = previous != null && left > 0 ? Math.min(full, left + Math.max(0, full - enduranceBonus(previous))) : full;
                FeathersAPI.addBonusStamina(entity, ENDURANCE_BONUS, amount, instance.isInfiniteDuration() ? -1 : instance.getDuration());
            }


            @Override
            public void onEnded(LivingEntity entity, MobEffectInstance instance) {
                FeathersAPI.removeBonusStamina(entity, ENDURANCE_BONUS);
            }
        });

        EFFECTS.register("cold", () -> new FeathersMobEffect(MobEffectCategory.HARMFUL, 0xB6FFFF) {
            @Override
            public boolean canApply(LivingEntity entity) {
                if (!super.canApply(entity) || !FeathersServerConfig.ENABLE_COLD.get()) return false;
                if (!ColdSweatCompat.canApplyCold(entity)) return false;
                return !entity.hasEffect(FeathersMobEffects.ENERGIZED) && !entity.hasEffect(FeathersMobEffects.HOT);
            }
        }.addAttributeModifier(FEATHERS_PER_SECOND, id("effect.cold"), -0.5, ADD_MULTIPLIED_TOTAL));

        EFFECTS.register("energized", () -> new FeathersMobEffect(MobEffectCategory.BENEFICIAL, 0x71CEFF)
                .addAttributeModifier(FEATHERS_PER_SECOND, id("effect.energized"), 1.0, ADD_MULTIPLIED_TOTAL));

        EFFECTS.register("hot", () -> new FeathersMobEffect(MobEffectCategory.HARMFUL, 0x7E5D48) {
            @Override
            public boolean canApply(LivingEntity entity) {
                if (!super.canApply(entity) || !FeathersServerConfig.ENABLE_HEAT.get() || isProtectedFromHeat(entity)) return false;
                if (!ColdSweatCompat.canApplyHeat(entity)) return false;
                return !entity.hasEffect(FeathersMobEffects.MOMENTUM) && !entity.hasEffect(FeathersMobEffects.COLD);
            }
        }.addAttributeModifier(USAGE_MULTIPLIER, id("effect.hot"), 1.0, ADD_VALUE));

        EFFECTS.register("fatigued", () -> new FeathersMobEffect(MobEffectCategory.HARMFUL, 0xFF0048) {
            @Override
            public boolean canApply(LivingEntity entity) {
                return super.canApply(entity) && FeathersServerConfig.ENABLE_FATIGUE.get() && !isProtectedFromHeat(entity);
            }
        }.addAttributeModifier(MAX_FEATHERS, id("effect.fatigued"), -4.0, ADD_VALUE));

        EFFECTS.register("momentum", () -> new FeathersMobEffect(MobEffectCategory.BENEFICIAL, 0x7E5684) {
            @Override
            public boolean canApply(LivingEntity entity) {
                return super.canApply(entity) && FeathersServerConfig.ENABLE_MOMENTUM.get();
            }
        }.addAttributeModifier(USAGE_MULTIPLIER, id("effect.momentum"), ADD_MULTIPLIED_TOTAL, amplifier -> Math.max(-0.8, -0.5 - 0.15 * amplifier)));

        EFFECTS.register("strain", () -> new FeathersMobEffect(MobEffectCategory.HARMFUL, 0x7E4488) {
            @Override
            public boolean canApply(LivingEntity entity) {
                return super.canApply(entity) && FeathersServerConfig.ENABLE_STRAIN.get();
            }
        }.addAttributeModifier(FEATHERS_PER_SECOND, id("effect.strain"), -0.75, ADD_MULTIPLIED_TOTAL));

        EFFECTS.register("cooling", () -> new FeathersMobEffect(MobEffectCategory.BENEFICIAL, 0x5FE0B4));
    }

    public static void register(IEventBus modEventBus) {
        EFFECTS.register(modEventBus);
    }

    private ModEffects() {}
}
