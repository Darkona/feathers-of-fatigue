package com.darkona.feathersoffatigue.content.feathers;

import com.darkona.feathersoffatigue.api.registry.FeathersMobEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static com.darkona.feathersoffatigue.content.feathers.FeathersTicker.modifierId;

public class FeathersMobEffect extends MobEffect {
    private record PendingModifier(Supplier<Attribute> attribute, ResourceLocation id, double amount, AttributeModifier.Operation operation) {}

    private final List<PendingModifier> pending = new ArrayList<>(1);

    public FeathersMobEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    public FeathersMobEffect withModifier(Supplier<Attribute> attribute, ResourceLocation id, double amount, AttributeModifier.Operation operation) {
        pending.add(new PendingModifier(attribute, id, amount, operation));
        return this;
    }

    public void bindModifiers() {
        for (PendingModifier modifier : pending) {
            addAttributeModifier(modifier.attribute().get(), modifierId(modifier.id()).toString(), modifier.amount(), modifier.operation());
        }
        pending.clear();
    }

    public boolean canApply(LivingEntity entity) {
        return !(entity instanceof Player player && (player.isCreative() || player.getAbilities().invulnerable));
    }

    public void onApplied(LivingEntity entity, MobEffectInstance instance, @Nullable MobEffectInstance previous) {}

    public void onEnded(LivingEntity entity, MobEffectInstance instance) {}

    public static boolean isProtectedFromHeat(LivingEntity entity) {
        return entity.hasEffect(MobEffects.FIRE_RESISTANCE) || entity.hasEffect(FeathersMobEffects.COOLING.get());
    }
}
