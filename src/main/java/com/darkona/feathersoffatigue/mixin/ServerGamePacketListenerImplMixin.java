package com.darkona.feathersoffatigue.mixin;

import com.darkona.feathersoffatigue.mount.MountExertion;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PlayerRideableJumping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {

    @Shadow
    public ServerPlayer player;

    @WrapOperation(method = "handlePlayerCommand", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/PlayerRideableJumping;canJump()Z"))
    private boolean feathers_of_fatigue$canJump(PlayerRideableJumping mount, Operation<Boolean> original) {
        return original.call(mount) && (!(mount instanceof LivingEntity living) || MountExertion.canJump(living));
    }

    @WrapOperation(method = "handlePlayerCommand", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/PlayerRideableJumping;handleStartJump(I)V"))
    private void feathers_of_fatigue$chargeJump(PlayerRideableJumping mount, int power, Operation<Void> original) {
        if (!(mount instanceof LivingEntity living) || MountExertion.chargeJump(living, player, power)) original.call(mount, power);
    }
}
