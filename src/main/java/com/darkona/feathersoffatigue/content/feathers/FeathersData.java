package com.darkona.feathersoffatigue.content.feathers;

import com.darkona.feathersoffatigue.api.Climate;
import com.darkona.feathersoffatigue.api.FeathersView;
import com.darkona.feathersoffatigue.api.RestState;
import com.darkona.feathersoffatigue.api.SpendResult;
import com.darkona.feathersoffatigue.api.Stamina;
import com.darkona.feathersoffatigue.api.registry.FeathersAttributes;
import com.darkona.feathersoffatigue.config.ServerConfig;
import com.darkona.feathersoffatigue.content.weight.WeightSplit;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraftforge.common.util.INBTSerializable;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class FeathersData implements FeathersView, INBTSerializable<CompoundTag> {
    public static final long FOREVER = Long.MAX_VALUE;

    abstract static class Sourced {
        final ResourceLocation source;

        Sourced(ResourceLocation source) {
            this.source = source;
        }
    }

    static final class Bonus extends Sourced {
        int amount;
        long expiresAt;

        Bonus(ResourceLocation source, int amount, long expiresAt) {
            super(source);
            this.amount = amount;
            this.expiresAt = expiresAt;
        }
    }

    static final class Drain extends Sourced {
        double perTick;
        boolean allowStrain;
        boolean blocksRegen;
        int timeoutTicks;
        long lastRefresh;
        double carry;

        Drain(ResourceLocation source) {
            super(source);
        }
    }

    static final class Timed extends Sourced {
        double value;
        long until;

        Timed(ResourceLocation source) {
            super(source);
        }
    }

    int stamina;
    int strain;
    int regenDelay;
    boolean exhausted;
    final ArrayList<Bonus> bonuses = new ArrayList<>(2);
    final Object2DoubleOpenHashMap<String> counters = new Object2DoubleOpenHashMap<>();

    int maxStamina;
    int maxStrain;
    int reserved;

    final WeightSplit weightSplit = new WeightSplit();
    double lastWeightMultiplier = Double.NaN;
    boolean initialized;

    boolean fresh = true;

    final ArrayList<Drain> drains = new ArrayList<>(2);
    final ArrayList<Timed> regenBlocks = new ArrayList<>(2);
    final ArrayList<Timed> restBonuses = new ArrayList<>(1);
    double regenCarry;
    double rate;

    boolean regenPaused;
    int lastDelta;
    long totalRegenerated;
    Climate climate = Climate.NEUTRAL;

    SpendLog spendLog;

    @Nullable AttributeInstance regenAttribute;
    @Nullable AttributeInstance usageAttribute;
    @Nullable AttributeInstance maxFeathersAttribute;
    @Nullable AttributeInstance maxStrainAttribute;
    @Nullable AttributeInstance weightMultiplierAttribute;
    @Nullable AttributeInstance armorWeightAttribute;

    RestState restState = RestState.NONE;
    double restMultiplier = 1.0;
    double lastX;
    double lastY;
    double lastZ;
    int stillTicks;

    int syncedStamina = -1;
    int syncedMax = -1;
    int syncedStrain = -1;
    int syncedMaxStrain = -1;
    int syncedBonus = -1;
    int syncedReserved = -1;
    double syncedRate = Double.NaN;
    boolean syncedExhausted;
    boolean syncedDelayed;
    final WeightSplit syncedWeightSplit = new WeightSplit();
    RestState syncedRest = RestState.NONE;
    boolean forceSync = true;

    public boolean mountSlowed;

    @Override
    public boolean hasFeathers() {
        return true;
    }

    @Override
    public int stamina() {
        return stamina;
    }

    @Override
    public int maxStamina() {
        return maxStamina;
    }

    @Override
    public int availableStamina() {
        return regularAvailable() + bonusStamina();
    }

    @Override
    public int weight() {
        return Stamina.toFeathersCeil(reserved);
    }

    public int reserved() {
        return reserved;
    }

    public double rate() {
        return rate;
    }

    public int weightPart(int part) {
        return weightSplit.part(part);
    }

    public WeightSplit weightSplit() {
        return weightSplit;
    }

    @Override
    public int strain() {
        return strain;
    }

    @Override
    public int maxStrain() {
        return maxStrain;
    }

    @Override
    public int bonusStamina() {
        int total = 0;
        int size = bonuses.size();
        for (int i = 0; i < size; i++) total += bonuses.get(i).amount;
        return total;
    }

    @Override
    public int regenDelay() {
        return regenDelay;
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
    public boolean exhausted() {
        return exhausted;
    }

    @Override
    public RestState restState() {
        return restState;
    }

    @Override
    public double restMultiplier() {
        return restMultiplier;
    }

    int regularAvailable() {
        return Math.max(0, stamina - reserved);
    }

    int strainRoom(boolean allowStrain, boolean strainEnabled) {
        return allowStrain && strainEnabled ? Math.max(0, maxStrain - strain) : 0;
    }

    boolean canPay(int cost, boolean allowStrain, boolean strainEnabled) {
        return (long) cost <= (long) bonusStamina() + regularAvailable() + strainRoom(allowStrain, strainEnabled);
    }

    public SpendResult check(int cost, boolean allowStrain, boolean ignoreExhaustion) {
        if (exhausted && !ignoreExhaustion) return SpendResult.EXHAUSTED;
        return canPay(cost, allowStrain, ServerConfig.ENABLE_STRAIN.get()) ? SpendResult.OK : SpendResult.INSUFFICIENT;
    }

    boolean predicted() {
        return false;
    }

    public boolean canJump(int cost) {
        return check(cost, true, false) == SpendResult.OK;
    }

    int pay(int cost) {
        int left = cost;
        int size = bonuses.size();
        for (int i = 0; i < size && left > 0; i++) {
            Bonus bonus = bonuses.get(i);
            int take = Math.min(bonus.amount, left);
            bonus.amount -= take;
            left -= take;
        }
        removeEmptyBonuses();

        int take = Math.min(regularAvailable(), left);
        stamina -= take;
        left -= take;

        strain += left;
        return left;
    }

    private void removeEmptyBonuses() {
        for (int i = bonuses.size() - 1; i >= 0; i--) {
            if (bonuses.get(i).amount <= 0) bonuses.remove(i);
        }
    }

    boolean isSpent(boolean strainEnabled) {
        return availableStamina() <= 0 && strainRoom(true, strainEnabled) <= 0;
    }

    public boolean isAtRest() {
        return initialized && stamina >= maxStamina && strain == 0 && regenDelay == 0 && !exhausted
                && bonuses.isEmpty() && drains.isEmpty() && regenBlocks.isEmpty();
    }

    static int indexOf(ArrayList<? extends Sourced> list, ResourceLocation source) {
        int size = list.size();
        for (int i = 0; i < size; i++) {
            if (list.get(i).source.equals(source)) return i;
        }
        return -1;
    }

    @Nullable
    Bonus bonus(ResourceLocation source) {
        int i = indexOf(bonuses, source);
        return i < 0 ? null : bonuses.get(i);
    }

    void setBonus(ResourceLocation source, int amount, long expiresAt) {
        Bonus bonus = bonus(source);
        if (bonus == null) bonuses.add(new Bonus(source, amount, expiresAt));
        else {
            bonus.amount = amount;
            bonus.expiresAt = expiresAt;
        }
    }

    public int bonusStamina(ResourceLocation source) {
        Bonus bonus = bonus(source);
        return bonus != null ? bonus.amount : 0;
    }

    boolean removeBonus(ResourceLocation source) {
        int i = indexOf(bonuses, source);
        if (i >= 0) bonuses.remove(i);
        return i >= 0;
    }

    @Nullable
    Drain drain(ResourceLocation source) {
        int i = indexOf(drains, source);
        return i < 0 ? null : drains.get(i);
    }

    @Nullable
    static Timed timed(ArrayList<Timed> list, ResourceLocation source) {
        int i = indexOf(list, source);
        return i < 0 ? null : list.get(i);
    }

    static void setTimed(ArrayList<Timed> list, ResourceLocation source, double value, long until) {
        Timed entry = timed(list, source);
        if (entry == null) {
            entry = new Timed(source);
            list.add(entry);
        }
        entry.value = value;
        entry.until = until;
    }

    static void removeTimed(ArrayList<Timed> list, ResourceLocation source) {
        int i = indexOf(list, source);
        if (i >= 0) list.remove(i);
    }

    void expire(long now) {
        for (int i = bonuses.size() - 1; i >= 0; i--) {
            if (bonuses.get(i).expiresAt <= now) bonuses.remove(i);
        }
        for (int i = regenBlocks.size() - 1; i >= 0; i--) {
            if (regenBlocks.get(i).until <= now) regenBlocks.remove(i);
        }
    }

    void expireRestBonuses(long now) {
        for (int i = restBonuses.size() - 1; i >= 0; i--) {
            if (restBonuses.get(i).until <= now) restBonuses.remove(i);
        }
    }

    boolean isRegenHeld() {
        if (!regenBlocks.isEmpty()) return true;
        int size = drains.size();
        for (int i = 0; i < size; i++) {
            Drain drain = drains.get(i);
            if (drain.blocksRegen) return true;
        }
        return false;
    }

    void logSpend(ResourceLocation source, int amount, long gameTime) {
        if (spendLog == null) spendLog = new SpendLog();
        spendLog.record(source, amount, gameTime);
    }

    public @Nullable SpendLog spendLog() {
        return spendLog;
    }

    @Nullable AttributeInstance regenAttribute(LivingEntity entity) {
        if (regenAttribute == null) regenAttribute = entity.getAttribute(FeathersAttributes.FEATHERS_PER_SECOND.get());
        return regenAttribute;
    }

    @Nullable AttributeInstance usageAttribute(LivingEntity entity) {
        if (usageAttribute == null) usageAttribute = entity.getAttribute(FeathersAttributes.USAGE_MULTIPLIER.get());
        return usageAttribute;
    }

    @Nullable AttributeInstance maxFeathersAttribute(LivingEntity entity) {
        if (maxFeathersAttribute == null) maxFeathersAttribute = entity.getAttribute(FeathersAttributes.MAX_FEATHERS.get());
        return maxFeathersAttribute;
    }

    @Nullable AttributeInstance maxStrainAttribute(LivingEntity entity) {
        if (maxStrainAttribute == null) maxStrainAttribute = entity.getAttribute(FeathersAttributes.MAX_STRAIN.get());
        return maxStrainAttribute;
    }

    @Nullable AttributeInstance armorWeightAttribute(LivingEntity entity) {
        if (armorWeightAttribute == null) armorWeightAttribute = entity.getAttribute(FeathersAttributes.ARMOR_WEIGHT.get());
        return armorWeightAttribute;
    }

    void forgetAttributes() {
        regenAttribute = null;
        usageAttribute = null;
        maxFeathersAttribute = null;
        maxStrainAttribute = null;
        weightMultiplierAttribute = null;
        armorWeightAttribute = null;
    }

    @Nullable AttributeInstance weightMultiplierAttribute(LivingEntity entity) {
        if (weightMultiplierAttribute == null) weightMultiplierAttribute = entity.getAttribute(FeathersAttributes.ARMOR_WEIGHT_MULTIPLIER.get());
        return weightMultiplierAttribute;
    }

    public double getCounter(String name) {
        return counters.getDouble(name);
    }

    public void setCounter(String name, double value) {
        counters.put(name, value);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("stamina", stamina);
        tag.putInt("strain", strain);
        tag.putInt("regen_delay", regenDelay);
        tag.putBoolean("exhausted", exhausted);

        ListTag bonusList = new ListTag();
        for (Bonus bonus : bonuses) {
            CompoundTag entry = new CompoundTag();
            entry.putString("source", bonus.source.toString());
            entry.putInt("amount", bonus.amount);
            entry.putLong("expires_at", bonus.expiresAt);
            bonusList.add(entry);
        }
        tag.put("bonuses", bonusList);

        CompoundTag counterTag = new CompoundTag();
        for (Object2DoubleMap.Entry<String> e : counters.object2DoubleEntrySet()) {
            counterTag.putDouble(e.getKey(), e.getDoubleValue());
        }
        tag.put("counters", counterTag);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        stamina = tag.getInt("stamina");
        strain = tag.getInt("strain");
        regenDelay = tag.getInt("regen_delay");
        exhausted = tag.getBoolean("exhausted");

        bonuses.clear();
        for (Tag t : tag.getList("bonuses", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) t;
            ResourceLocation source = ResourceLocation.tryParse(entry.getString("source"));
            if (source != null) bonuses.add(new Bonus(source, entry.getInt("amount"), entry.getLong("expires_at")));
        }

        counters.clear();
        CompoundTag counterTag = tag.getCompound("counters");
        for (String key : counterTag.getAllKeys()) {
            counters.put(key, counterTag.getDouble(key));
        }

        initialized = false;
        fresh = false;
        forceSync = true;
    }
}
