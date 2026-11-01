package com.darkona.feathersoffatigue.content.mount;

import com.darkona.feathersoffatigue.api.FeathersAPI;
import com.darkona.feathersoffatigue.api.Stamina;
import com.darkona.feathersoffatigue.api.registry.FeathersIds;
import com.darkona.feathersoffatigue.config.ServerConfig;
import com.darkona.feathersoffatigue.content.feathers.FeathersData;
import com.darkona.feathersoffatigue.content.feathers.FeathersServiceImpl;
import com.darkona.feathersoffatigue.content.feathers.FeathersTicker;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;

@Mod.EventBusSubscriber(modid = FeathersIds.MOD_ID)
public final class MountExertion {
    public static final ResourceLocation GALLOP = id("mount_gallop");
    public static final ResourceLocation JUMP = id("mount_jump");
    private static final ResourceLocation EXHAUSTED_SLOWDOWN = id("mount_exhausted");
    private static final UUID EXHAUSTED_SLOWDOWN_ID = FeathersTicker.modifierId(EXHAUSTED_SLOWDOWN);

    private MountExertion() {}

    @SubscribeEvent(priority = EventPriority.LOW, receiveCanceled = false)
    public static void onEntityTick(LivingEvent.LivingTickEvent event) {
        LivingEntity mount = event.getEntity();
        if (mount instanceof Player || mount.level().isClientSide() || !mount.isAlive()) return;
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
        return speed != null && speed.getModifier(EXHAUSTED_SLOWDOWN_ID) != null;
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
        double feathers = fullJumpFeathers(mount) * Mth.clamp(power, 0, 100) / 100.0;
        return feathers <= 0 || FeathersAPI.spend(mount, JUMP, Stamina.ofFeathers(feathers)).allowed();
    }

    public static boolean canJump(LivingEntity mount) {
        if (!FeathersServiceImpl.isMount(mount)) return true;
        FeathersData data = FeathersServiceImpl.stamina(mount);
        return data == null || data.canJump(Stamina.ofFeathers(fullJumpFeathers(mount)));
    }

    private static double fullJumpFeathers(LivingEntity mount) {
        return FeathersServiceImpl.mountTuning(mount).jumpFeathers();
    }

    private static void updateSlowdown(LivingEntity mount, FeathersData data) {
        boolean exhausted = data.exhausted();
        if (!exhausted && !data.mountSlowed) return;
        data.mountSlowed = exhausted;
        FeathersTicker.toggleModifier(mount, Attributes.MOVEMENT_SPEED, EXHAUSTED_SLOWDOWN, EXHAUSTED_SLOWDOWN_ID, exhausted,
                -ServerConfig.MOUNT_EXHAUSTED_SLOWDOWN.get(), AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    public static void release(LivingEntity entity) {
        if (FeathersServiceImpl.dataOrNull(entity) == null) return;
        FeathersTicker.toggleModifier(entity, Attributes.MOVEMENT_SPEED, EXHAUSTED_SLOWDOWN, EXHAUSTED_SLOWDOWN_ID, false, 0,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
        FeathersTicker.releaseExMount(entity);
    }
}
