package com.darkona.feathersoffatigue.index;

import com.darkona.feathersoffatigue.api.registry.FeathersIds;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class FeathersEnchantmentRegistry {
    private static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, FeathersIds.MOD_ID);

    private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    static {
        ENCHANTMENTS.register("lightweight", Lightweight::new);
        ENCHANTMENTS.register("heavy", Heavy::new);
    }

    public static void register(IEventBus modEventBus) {
        ENCHANTMENTS.register(modEventBus);
    }

    static final class Lightweight extends Enchantment {
        Lightweight() {
            super(Rarity.UNCOMMON, EnchantmentCategory.ARMOR, ARMOR_SLOTS);
        }

        @Override
        public int getMaxLevel() {
            return 3;
        }

        @Override
        public int getMinCost(int level) {
            return 5 + 8 * (level - 1);
        }

        @Override
        public int getMaxCost(int level) {
            return 25 + 8 * (level - 1);
        }
    }

    static final class Heavy extends Enchantment {
        Heavy() {
            super(Rarity.VERY_RARE, EnchantmentCategory.ARMOR, ARMOR_SLOTS);
        }

        @Override
        public int getMinCost(int level) {
            return 25;
        }

        @Override
        public int getMaxCost(int level) {
            return 50;
        }

        @Override
        public boolean isTreasureOnly() {
            return true;
        }

        @Override
        public boolean isCurse() {
            return true;
        }
    }

    private FeathersEnchantmentRegistry() {}
}
