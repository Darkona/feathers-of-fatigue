package com.darkona.feathersoffatigue.index;

import com.darkona.feathersoffatigue.api.registry.FeathersIds;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.common.brewing.IBrewingRecipe;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

import static com.darkona.feathersoffatigue.api.registry.FeathersMobEffects.*;

public final class FeathersPotionRegistry {
    private static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(ForgeRegistries.POTIONS, FeathersIds.MOD_ID);

    public static final RegistryObject<Potion> ENDURANCE_POTION = potion("endurance_potion", ENDURANCE, 2600, 0);
    public static final RegistryObject<Potion> STRONG_ENDURANCE_POTION = potion("strong_endurance_potion", ENDURANCE, 2000, 1);
    public static final RegistryObject<Potion> LONG_ENDURANCE_POTION = potion("long_endurance_potion", ENDURANCE, 4200, 0);

    public static final RegistryObject<Potion> MOMENTUM_POTION = potion("momentum_potion", MOMENTUM, 2600, 0);
    public static final RegistryObject<Potion> STRONG_MOMENTUM_POTION = potion("strong_momentum_potion", MOMENTUM, 2000, 1);
    public static final RegistryObject<Potion> LONG_MOMENTUM_POTION = potion("long_momentum_potion", MOMENTUM, 4200, 0);

    public static final RegistryObject<Potion> COLD_POTION = potion("cold_potion", COLD, 6000, 0);
    public static final RegistryObject<Potion> STRONG_COLD_POTION = potion("strong_cold_potion", COLD, 3000, 1);

    public static final RegistryObject<Potion> HOT_POTION = potion("hot_potion", HOT, 6000, 0);
    public static final RegistryObject<Potion> STRONG_HOT_POTION = potion("strong_hot_potion", HOT, 3000, 1);

    public static final RegistryObject<Potion> COOLING_POTION = potion("cooling_potion", COOLING, 3600, 0);
    public static final RegistryObject<Potion> LONG_COOLING_POTION = potion("long_cooling_potion", COOLING, 9600, 0);

    public static final RegistryObject<Potion> ENERGIZED_POTION = potion("energized_potion", ENERGIZED, 1600, 0);
    public static final RegistryObject<Potion> STRONG_ENERGIZED_POTION = potion("strong_energized_potion", ENERGIZED, 1000, 1);
    public static final RegistryObject<Potion> LONG_ENERGIZED_POTION = potion("long_energized_potion", ENERGIZED, 2600, 0);

    private static RegistryObject<Potion> potion(String name, Supplier<MobEffect> effect, int duration, int amplifier) {
        return POTIONS.register(name, () -> new Potion(new MobEffectInstance(effect.get(), duration, amplifier)));
    }

    public static void register(IEventBus modEventBus) {
        POTIONS.register(modEventBus);
    }

    public static void registerBrewingRecipes() {
        mix(() -> Potions.AWKWARD, Items.SNOWBALL, COLD_POTION);
        mix(COLD_POTION, Items.GLOWSTONE_DUST, STRONG_COLD_POTION);

        mix(() -> Potions.AWKWARD, Items.MAGMA_BLOCK, HOT_POTION);
        mix(HOT_POTION, Items.GLOWSTONE_DUST, STRONG_HOT_POTION);

        mix(() -> Potions.AWKWARD, Items.PACKED_ICE, COOLING_POTION);
        mix(COOLING_POTION, Items.REDSTONE, LONG_COOLING_POTION);

        mix(() -> Potions.AWKWARD, Items.FEATHER, ENDURANCE_POTION);
        mix(ENDURANCE_POTION, Items.REDSTONE, LONG_ENDURANCE_POTION);
        mix(ENDURANCE_POTION, Items.GLOWSTONE_DUST, STRONG_ENDURANCE_POTION);

        mix(() -> Potions.AWKWARD, Items.BASALT, MOMENTUM_POTION);
        mix(MOMENTUM_POTION, Items.REDSTONE, LONG_MOMENTUM_POTION);
        mix(MOMENTUM_POTION, Items.GLOWSTONE_DUST, STRONG_MOMENTUM_POTION);

        mix(() -> Potions.AWKWARD, Items.RAW_COPPER, ENERGIZED_POTION);
        mix(ENERGIZED_POTION, Items.REDSTONE, LONG_ENERGIZED_POTION);
        mix(ENERGIZED_POTION, Items.GLOWSTONE_DUST, STRONG_ENERGIZED_POTION);
    }

    private static void mix(Supplier<Potion> from, Item ingredient, Supplier<Potion> to) {
        BrewingRecipeRegistry.addRecipe(new PotionMix(from.get(), ingredient, to.get()));
    }

    private record PotionMix(Potion from, Item ingredient, Potion to) implements IBrewingRecipe {
        @Override
        public boolean isInput(ItemStack input) {
            Item item = input.getItem();
            return (item == Items.POTION || item == Items.SPLASH_POTION || item == Items.LINGERING_POTION)
                    && PotionUtils.getPotion(input) == from;
        }

        @Override
        public boolean isIngredient(ItemStack stack) {
            return stack.is(ingredient);
        }

        @Override
        public ItemStack getOutput(ItemStack input, ItemStack stack) {
            if (!isInput(input) || !isIngredient(stack)) return ItemStack.EMPTY;
            return PotionUtils.setPotion(new ItemStack(input.getItem()), to);
        }
    }

    private FeathersPotionRegistry() {}
}
