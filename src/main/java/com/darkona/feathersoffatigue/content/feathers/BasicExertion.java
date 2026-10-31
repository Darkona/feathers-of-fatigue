package com.darkona.feathersoffatigue.content.feathers;

import com.darkona.feathersoffatigue.api.FeathersAPI;
import com.darkona.feathersoffatigue.api.SpendResult;
import com.darkona.feathersoffatigue.api.Stamina;
import com.darkona.feathersoffatigue.api.registry.FeathersIds;
import com.darkona.feathersoffatigue.config.ServerConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;

@EventBusSubscriber(modid = FeathersIds.MOD_ID)
public final class BasicExertion {

    public static final ResourceLocation SPRINT = id("sprint");
    public static final ResourceLocation JUMP = id("jump");

    private BasicExertion() {}

    public static boolean isActive() {
        return !FeathersAPI.arePlayerActionsTakenOver() && ServerConfig.ENABLE_BASIC_EXERTION.get();
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!isActive() || !player.isSprinting() || player.isPassenger()) return;

        double perSecond = ServerConfig.SPRINT_FEATHERS_PER_SECOND.get();
        if (perSecond <= 0) return;

        SpendResult result = FeathersAPI.startDrain(player, SPRINT, Stamina.perTick(perSecond));
        if (!result.allowed()) player.setSprinting(false);
    }

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof Player player) || !isActive()) return;
        double feathers = ServerConfig.JUMP_FEATHERS.get();
        if (feathers > 0) FeathersAPI.spend(player, JUMP, Stamina.ofFeathers(feathers));
    }
}
