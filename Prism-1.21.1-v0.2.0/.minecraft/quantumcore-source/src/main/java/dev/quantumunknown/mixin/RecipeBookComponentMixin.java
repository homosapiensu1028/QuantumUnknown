package dev.quantumunknown.mixin;

import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The vanilla recipe book would disclose the original recipes, so its toggle is disabled. */
@Mixin(RecipeBookComponent.class)
public abstract class RecipeBookComponentMixin {
    @Inject(method = "toggleVisibility", at = @At("HEAD"), cancellable = true)
    private void quantum$disableVanillaRecipeBook(CallbackInfo ci) {
        ci.cancel();
    }
}
