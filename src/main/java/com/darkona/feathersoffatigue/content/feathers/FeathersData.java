package com.darkona.feathersoffatigue.content.feathers;

import com.darkona.feathersoffatigue.api.Climate;
import com.darkona.feathersoffatigue.api.RestState;
import com.darkona.feathersoffatigue.api.registry.FeathersAttributes;
import com.darkona.feathersoffatigue.content.weight.WeightSplit;
import dev.darkona.jdlib.core.debug.RecentSourceLog;
import dev.darkona.jdlib.core.stat.FractionalCarry;
import dev.darkona.jdlib.core.stat.Resource;
import dev.darkona.jdlib.core.stat.ShownFields;
import dev.darkona.jdlib.core.stat.TimedSources;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class FeathersData extends StaminaView implements INBTSerializable<CompoundTag> {

    private static final int SPEND_LOG_SIZE = 32;
    private static final int SPEND_FOLD_TICKS = 20;

    enum Shown { REST }

    @Nullable Resource<ResourceLocation> resource;
    final Object2DoubleOpenHashMap<String> counters = new Object2DoubleOpenHashMap<>();

    final WeightSplit weightSplit = new WeightSplit();
    double lastWeightMultiplier = Double.NaN;
    boolean initialized;
    boolean fresh = true;

    final TimedSources<ResourceLocation> restBonuses = new TimedSources<>();
    private final Object2ObjectOpenHashMap<String, FractionalCarry> carries = new Object2ObjectOpenHashMap<>();
    int lastDelta;
    long totalRegenerated;
    Climate climate = Climate.NEUTRAL;

    RecentSourceLog<ResourceLocation> spendLog;

    @Nullable AttributeInstance usageAttribute;
    @Nullable AttributeInstance weightMultiplierAttribute;

    RestState restState = RestState.NONE;
    double restMultiplier = 1.0;
    double lastX;
    double lastY;
    double lastZ;
    int stillTicks;

    final ShownFields<Shown> shown = new ShownFields<>(Shown.class);
    final WeightSplit syncedWeightSplit = new WeightSplit();

    public boolean mountSlowed;

    @Override
    public boolean hasFeathers() {
        return true;
    }

    @Override
    protected @Nullable Resource<ResourceLocation> resource() {
        return resource;
    }

    public int weightPart(int part) {
        return weightSplit.part(part);
    }

    public WeightSplit weightSplit() {
        return weightSplit;
    }

    @Override
    public int lastDelta() {
        return lastDelta;
    }

    @Override
    public long totalRegenerated() {
        return totalRegenerated;
    }

    @Override
    public RestState restState() {
        return restState;
    }

    @Override
    public double restMultiplier() {
        return restMultiplier;
    }

    public boolean isAtRest() {
        Resource<ResourceLocation> r = resource;
        return initialized && r != null && r.full() && r.overdraw() == 0 && r.regenDelay() == 0 && !r.exhausted()
                && r.bonuses().isEmpty() && r.drains().isEmpty() && r.regenBlocks().isEmpty();
    }

    public int bonusStamina(ResourceLocation source) {
        Resource<ResourceLocation> r = resource;
        return r != null ? r.bonuses().amount(source) : 0;
    }

    void logSpend(ResourceLocation source, int amount, long gameTime) {
        if (spendLog == null) spendLog = new RecentSourceLog<>(SPEND_LOG_SIZE, SPEND_FOLD_TICKS);
        spendLog.record(source, amount, gameTime);
    }

    public @Nullable RecentSourceLog<ResourceLocation> spendLog() {
        return spendLog;
    }

    @Nullable AttributeInstance usageAttribute(LivingEntity entity) {
        if (usageAttribute == null) usageAttribute = entity.getAttribute(FeathersAttributes.USAGE_MULTIPLIER);
        return usageAttribute;
    }

    @Nullable AttributeInstance weightMultiplierAttribute(LivingEntity entity) {
        if (weightMultiplierAttribute == null) weightMultiplierAttribute = entity.getAttribute(FeathersAttributes.ARMOR_WEIGHT_MULTIPLIER);
        return weightMultiplierAttribute;
    }

    public FractionalCarry carry(String name) {
        FractionalCarry carry = carries.get(name);
        if (carry == null) carries.put(name, carry = new FractionalCarry());
        return carry;
    }

    public double getCounter(String name) {
        return counters.getDouble(name);
    }

    public void setCounter(String name, double value) {
        counters.put(name, value);
    }

    @Override
    public @NotNull CompoundTag serializeNBT(HolderLookup.@NotNull Provider provider) {
        CompoundTag tag = new CompoundTag();
        CompoundTag counterTag = new CompoundTag();
        for (Object2DoubleMap.Entry<String> e : counters.object2DoubleEntrySet()) {
            counterTag.putDouble(e.getKey(), e.getDoubleValue());
        }
        tag.put("counters", counterTag);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.@NotNull Provider provider, @NotNull CompoundTag tag) {
        counters.clear();
        CompoundTag counterTag = tag.getCompound("counters");
        for (String key : counterTag.getAllKeys()) {
            counters.put(key, counterTag.getDouble(key));
        }
        initialized = false;
        fresh = false;
        shown.resendNext();
    }
}
