package com.darkona.feathersoffatigue.mixin;

import com.darkona.feathersoffatigue.content.mount.MountExertion;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractHorse.class)
public abstract class AbstractHorseMixin {

    @ModifyReturnValue(method = "canJump", at = @At("RETURN"))
    private boolean feathers_of_fatigue$canJump(boolean original) {
        return original && MountExertion.canJump((AbstractHorse) (Object) this);
    }
}
