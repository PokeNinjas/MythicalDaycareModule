package com.mythicalnetwork.mythicaldaycare.mixin;

import net.minecraft.commands.Commands;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Commands.class)
public class CommandsMixin {
    @Redirect(at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;isDebugEnabled()Z"), method = "performCommand")
    public boolean redirectIsDebugEnabled(Logger instance) {
        return true;
    }
}