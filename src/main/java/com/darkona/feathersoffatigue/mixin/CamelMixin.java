package com.darkona.feathersoffatigue.mixin;

import com.darkona.feathersoffatigue.content.mount.MountExertion;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.animal.camel.Camel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Camel.class)
public abstract class CamelMixin {

    @ModifyReturnValue(method = "canJump", at = @At("RETURN"))
    private boolean feathers_of_fatigue$canDash(boolean original) {
        return original && MountExertion.canJump((Camel) (Object) this);
    }
}
