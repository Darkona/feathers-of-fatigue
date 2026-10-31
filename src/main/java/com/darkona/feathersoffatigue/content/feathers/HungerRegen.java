package com.darkona.feathersoffatigue.content.feathers;

import com.darkona.feathersoffatigue.api.FeathersView;
import com.darkona.feathersoffatigue.api.registry.FeathersAttributes;
import com.darkona.feathersoffatigue.config.ServerConfig;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodConstants;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;

public final class HungerRegen {

    static final int HUNGRY_FOOD_LEVEL = 6;

    private HungerRegen() {}

    public static void registerBuiltIn() {
        Extensions.addRegenFactor(id("hunger"), HungerRegen::regenFactor);
    }

    public static double multiplier(int foodLevel, float saturation, double hungry, double saturated) {
        double fullness = (foodLevel - HUNGRY_FOOD_LEVEL) / (double) (FoodConstants.MAX_FOOD - HUNGRY_FOOD_LEVEL);
        double food = Mth.lerp(Mth.clamp(fullness, 0.0, 1.0), hungry, 1.0);
        return food * Mth.lerp(Mth.clamp(saturation / FoodConstants.MAX_SATURATION, 0.0, 1.0), 1.0, saturated);
    }

    private static double regenFactor(LivingEntity entity, FeathersView feathers) {
        if (!(entity instanceof Player player)) return 0.0;

        double multiplier = multiplier(player.getFoodData().getFoodLevel(), player.getFoodData().getSaturationLevel(),
                ServerConfig.HUNGER_REGEN_PENALTY.get(), ServerConfig.SATURATION_REGEN_BONUS.get());
        if (multiplier == 1.0) return 0.0;

        AttributeInstance regen = player.getAttribute(FeathersAttributes.FEATHERS_PER_SECOND);
        double base = regen != null ? regen.getBaseValue() : 0.0;
        return base > 0 ? base * (multiplier - 1.0) : 0.0;
    }
}
