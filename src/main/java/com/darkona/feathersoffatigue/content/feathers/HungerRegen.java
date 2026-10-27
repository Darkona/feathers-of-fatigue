package com.darkona.feathersoffatigue.content.feathers;

import com.darkona.feathersoffatigue.api.FeathersView;
import com.darkona.feathersoffatigue.api.registry.FeathersAttributes;
import com.darkona.feathersoffatigue.config.ServerConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodConstants;
import net.minecraft.world.food.FoodData;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;

public final class HungerRegen {

    static final int HUNGRY_FOOD_LEVEL = 6;

    private HungerRegen() {}

    public static void registerBuiltIn() {
        Extensions.addRegenFactor(id("hunger"), HungerRegen::regenFactor);
    }

    private static double regenFactor(LivingEntity entity, FeathersView feathers) {
        if (!(entity instanceof Player player)) return 0.0;

        FoodData food = player.getFoodData();
        int level = food.getFoodLevel();
        double multiplier;
        if (level >= FoodConstants.MAX_FOOD && food.getSaturationLevel() > 0) {
            multiplier = ServerConfig.SATURATION_REGEN_BONUS.get();
        } else if (level <= HUNGRY_FOOD_LEVEL) {
            multiplier = ServerConfig.HUNGER_REGEN_PENALTY.get();
        } else {
            return 0.0;
        }
        if (multiplier == 1.0) return 0.0;

        AttributeInstance regen = player.getAttribute(FeathersAttributes.FEATHERS_PER_SECOND);
        double base = regen != null ? regen.getBaseValue() : 0.0;
        return base > 0 ? base * (multiplier - 1.0) : 0.0;
    }
}
