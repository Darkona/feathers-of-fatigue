package com.darkona.feathersoffatigue.content.mount;

import com.darkona.feathersoffatigue.api.registry.FeathersAttributes;
import com.darkona.feathersoffatigue.api.registry.FeathersIds;
import com.darkona.feathersoffatigue.content.feathers.FeathersData;
import com.darkona.feathersoffatigue.content.feathers.FeathersServiceImpl;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FeathersIds.MOD_ID)
public final class MountTraits {
    private static final String ROLLED = "mount.stamina_rolled";

    private MountTraits() {}

    public static void ensureRolled(LivingEntity mount) {
        FeathersData data = FeathersServiceImpl.data(mount);
        if (data.getCounter(ROLLED) != 0) return;
        RandomSource random = mount.getRandom();
        double roll = (random.nextDouble() + random.nextDouble() + random.nextDouble()) / 3.0;
        setTrait(mount, data, Mth.lerp(roll, min(mount), max(mount)));
    }

    @SubscribeEvent
    public static void onBreed(BabyEntitySpawnEvent event) {
        LivingEntity foal = event.getChild();
        LivingEntity a = event.getParentA(), b = event.getParentB();
        if (foal == null || a == null || b == null || foal.level().isClientSide() || !FeathersServiceImpl.isMount(foal)) return;

        double average = (trait(a, foal) + trait(b, foal)) / 2.0;
        double spread = (max(foal) - min(foal)) / 6.0;
        double feathers = average + (foal.getRandom().nextDouble() * 2.0 - 1.0) * spread;
        setTrait(foal, FeathersServiceImpl.data(foal), Mth.clamp(feathers, min(foal), max(foal)));
    }

    private static double trait(LivingEntity parent, LivingEntity foal) {
        AttributeInstance attr = parent.getAttribute(FeathersAttributes.MAX_FEATHERS.get());
        return attr != null ? attr.getBaseValue() : (min(foal) + max(foal)) / 2.0;
    }

    private static void setTrait(LivingEntity mount, FeathersData data, double feathers) {
        AttributeInstance attr = mount.getAttribute(FeathersAttributes.MAX_FEATHERS.get());
        if (attr == null) return;
        attr.setBaseValue(Math.round(feathers));
        data.setCounter(ROLLED, 1);
    }

    private static double min(LivingEntity mount) {
        return FeathersServiceImpl.mountTuning(mount).minFeathers();
    }

    private static double max(LivingEntity mount) {
        return FeathersServiceImpl.mountTuning(mount).maxFeathers();
    }
}
