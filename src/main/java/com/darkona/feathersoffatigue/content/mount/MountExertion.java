package com.darkona.feathersoffatigue.content.mount;

import com.darkona.feathersoffatigue.api.FeathersAPI;
import com.darkona.feathersoffatigue.api.FeathersView;
import com.darkona.feathersoffatigue.api.SpendResult;
import com.darkona.feathersoffatigue.api.Stamina;
import com.darkona.feathersoffatigue.api.registry.FeathersIds;
import com.darkona.feathersoffatigue.config.FeathersServerConfig;
import com.darkona.feathersoffatigue.content.feathers.FeathersAttachments;
import com.darkona.feathersoffatigue.content.feathers.FeathersData;
import com.darkona.feathersoffatigue.content.feathers.FeathersServiceImpl;
import com.darkona.feathersoffatigue.content.feathers.FeathersTicker;
import dev.darkona.jdlib.api.attribute.JDModifiers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;


@EventBusSubscriber(modid = FeathersIds.MOD_ID)
public final class MountExertion {

    public static final ResourceLocation GALLOP = id("mount_gallop");
    public static final ResourceLocation JUMP = id("mount_jump");
    private static final ResourceLocation EXHAUSTED_SLOWDOWN = id("mount_exhausted");

    private MountExertion() {}

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity mount) || mount instanceof Player || mount.level().isClientSide()
                || !mount.isAlive()) return;
        if (FeathersServiceImpl.isMount(mount)) tickMount(mount);
        else if (mount.tickCount % 100 == 0) release(mount);
    }

    public static void tickMount(LivingEntity mount) {
        Player rider = riderOf(mount);
        FeathersData data = FeathersServiceImpl.data(mount);
        if (rider != null || !data.isAtRest()) {
            if (rider != null && !FeathersServiceImpl.isExempt(rider)) gallop(mount);
            FeathersTicker.tick(mount);
        }
        updateSlowdown(mount, data);
    }

    public static boolean isSlowedDown(LivingEntity mount) {
        AttributeInstance speed = mount.getAttribute(Attributes.MOVEMENT_SPEED);
        return speed != null && speed.hasModifier(EXHAUSTED_SLOWDOWN);
    }

    @SubscribeEvent
    public static void onMount(EntityMountEvent event) {
        if (event.isMounting() && event.getEntityBeingMounted() instanceof LivingEntity mount && !mount.level().isClientSide()
                && FeathersServiceImpl.isMount(mount)) {
            FeathersAPI.sync(mount);
        }
    }

    public static Player riderOf(LivingEntity mount) {
        if (mount.getControllingPassenger() instanceof Player player) return player;
        return mount.getFirstPassenger() instanceof Player player ? player : null;
    }

    private static void gallop(LivingEntity mount) {
        MountTuning tuning = FeathersServiceImpl.mountTuning(mount);
        double perSecond = tuning.gallopFeathersPerSecond();
        if (perSecond <= 0) return;
        Vec3 motion = mount.getDeltaMovement();
        double speed = tuning.gallopSpeed();
        if (motion.x * motion.x + motion.z * motion.z < speed * speed) return;
        FeathersAPI.startDrain(mount, GALLOP, Stamina.perTick(perSecond));
    }

    public static boolean chargeJump(LivingEntity mount, int power) {
        Player rider = riderOf(mount);
        return rider == null || chargeJump(mount, rider, power);
    }

    public static boolean chargeJump(LivingEntity mount, Player rider, int power) {
        if (!FeathersServiceImpl.isMount(mount) || FeathersServiceImpl.isExempt(rider)) return true;
        double feathers = fullJumpFeathers(mount) * Math.clamp(power, 0, 100) / 100.0;
        return feathers <= 0 || FeathersAPI.spend(mount, JUMP, Stamina.ofFeathers(feathers)).allowed();
    }

    public static boolean canJump(LivingEntity mount) {
        if (!FeathersServiceImpl.isMount(mount)) return true;
        FeathersView view = FeathersAPI.get(mount);
        if (!view.hasFeathers()) return true;
        return FeathersServiceImpl.simulateAgainst(view, Stamina.ofFeathers(fullJumpFeathers(mount)), true, false) == SpendResult.OK;
    }

    private static double fullJumpFeathers(LivingEntity mount) {
        return FeathersServiceImpl.mountTuning(mount).jumpFeathers();
    }

    private static void updateSlowdown(LivingEntity mount, FeathersData data) {
        boolean exhausted = data.exhausted();
        if (!exhausted && !data.mountSlowed) return;
        data.mountSlowed = exhausted;
        JDModifiers.toggle(mount, Attributes.MOVEMENT_SPEED, EXHAUSTED_SLOWDOWN, exhausted, -FeathersServerConfig.MOUNT_EXHAUSTED_SLOWDOWN.get(),
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    public static void release(LivingEntity entity) {
        if (!entity.hasData(FeathersAttachments.FEATHERS)) return;
        JDModifiers.toggle(entity, Attributes.MOVEMENT_SPEED, EXHAUSTED_SLOWDOWN, false, 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        FeathersTicker.releaseExMount(entity);
    }
}
