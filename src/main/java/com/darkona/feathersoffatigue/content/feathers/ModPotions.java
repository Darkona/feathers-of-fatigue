package com.darkona.feathersoffatigue.content.feathers;

import com.darkona.feathersoffatigue.api.registry.FeathersIds;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.darkona.feathersoffatigue.api.registry.FeathersMobEffects.*;

public final class ModPotions {

    private static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(Registries.POTION, FeathersIds.MOD_ID);

    public static final DeferredHolder<Potion, Potion> ENDURANCE_POTION = potion("endurance_potion", ENDURANCE, 2600, 0);
    public static final DeferredHolder<Potion, Potion> STRONG_ENDURANCE_POTION = potion("strong_endurance_potion", ENDURANCE, 2000, 1);
    public static final DeferredHolder<Potion, Potion> LONG_ENDURANCE_POTION = potion("long_endurance_potion", ENDURANCE, 4200, 0);

    public static final DeferredHolder<Potion, Potion> MOMENTUM_POTION = potion("momentum_potion", MOMENTUM, 2600, 0);
    public static final DeferredHolder<Potion, Potion> STRONG_MOMENTUM_POTION = potion("strong_momentum_potion", MOMENTUM, 2000, 1);
    public static final DeferredHolder<Potion, Potion> LONG_MOMENTUM_POTION = potion("long_momentum_potion", MOMENTUM, 4200, 0);

    public static final DeferredHolder<Potion, Potion> COLD_POTION = potion("cold_potion", COLD, 6000, 0);
    public static final DeferredHolder<Potion, Potion> STRONG_COLD_POTION = potion("strong_cold_potion", COLD, 3000, 1);

    public static final DeferredHolder<Potion, Potion> HOT_POTION = potion("hot_potion", HOT, 6000, 0);
    public static final DeferredHolder<Potion, Potion> STRONG_HOT_POTION = potion("strong_hot_potion", HOT, 3000, 1);

    public static final DeferredHolder<Potion, Potion> COOLING_POTION = potion("cooling_potion", COOLING, 3600, 0);
    public static final DeferredHolder<Potion, Potion> LONG_COOLING_POTION = potion("long_cooling_potion", COOLING, 9600, 0);

    public static final DeferredHolder<Potion, Potion> ENERGIZED_POTION = potion("energized_potion", ENERGIZED, 1600, 0);
    public static final DeferredHolder<Potion, Potion> STRONG_ENERGIZED_POTION = potion("strong_energized_potion", ENERGIZED, 1000, 1);
    public static final DeferredHolder<Potion, Potion> LONG_ENERGIZED_POTION = potion("long_energized_potion", ENERGIZED, 2600, 0);

    private static DeferredHolder<Potion, Potion> potion(String name, Holder<MobEffect> effect, int duration, int amplifier) {
        return POTIONS.register(name, () -> new Potion(new MobEffectInstance(effect, duration, amplifier)));
    }

    public static void register(IEventBus modEventBus) {
        POTIONS.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(ModPotions::registerBrewingRecipes);
    }

    private static void registerBrewingRecipes(RegisterBrewingRecipesEvent event) {
        PotionBrewing.Builder builder = event.getBuilder();

        builder.addMix(Potions.AWKWARD, Items.SNOWBALL, COLD_POTION);
        builder.addMix(COLD_POTION, Items.GLOWSTONE_DUST, STRONG_COLD_POTION);

        builder.addMix(Potions.AWKWARD, Items.MAGMA_BLOCK, HOT_POTION);
        builder.addMix(HOT_POTION, Items.GLOWSTONE_DUST, STRONG_HOT_POTION);

        builder.addMix(Potions.AWKWARD, Items.PACKED_ICE, COOLING_POTION);
        builder.addMix(COOLING_POTION, Items.REDSTONE, LONG_COOLING_POTION);

        builder.addMix(Potions.AWKWARD, Items.FEATHER, ENDURANCE_POTION);
        builder.addMix(ENDURANCE_POTION, Items.REDSTONE, LONG_ENDURANCE_POTION);
        builder.addMix(ENDURANCE_POTION, Items.GLOWSTONE_DUST, STRONG_ENDURANCE_POTION);

        builder.addMix(Potions.AWKWARD, Items.BASALT, MOMENTUM_POTION);
        builder.addMix(MOMENTUM_POTION, Items.REDSTONE, LONG_MOMENTUM_POTION);
        builder.addMix(MOMENTUM_POTION, Items.GLOWSTONE_DUST, STRONG_MOMENTUM_POTION);

        builder.addMix(Potions.AWKWARD, Items.RAW_COPPER, ENERGIZED_POTION);
        builder.addMix(ENERGIZED_POTION, Items.REDSTONE, LONG_ENERGIZED_POTION);
        builder.addMix(ENERGIZED_POTION, Items.GLOWSTONE_DUST, STRONG_ENERGIZED_POTION);
    }

    private ModPotions() {}
}
