package dev.quantumunknown.jei;

import dev.quantumunknown.QuantumCore;
import dev.quantumunknown.client.ClientKnowledge;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaRecipeTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

@JeiPlugin
public final class QuantumJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(QuantumCore.MOD_ID, "jei_plugin");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        // Hide vanilla recipe categories rather than trying to display stale vanilla outputs.
        List<RecipeType<?>> hiddenCategories = List.of(
            VanillaRecipeTypes.CRAFTING,
            VanillaRecipeTypes.SMELTING,
            VanillaRecipeTypes.BLASTING,
            VanillaRecipeTypes.SMOKING,
            VanillaRecipeTypes.CAMPFIRE_COOKING,
            VanillaRecipeTypes.STONECUTTING,
            VanillaRecipeTypes.SMITHING
        );
        for (RecipeType<?> type : hiddenCategories) runtime.getRecipeManager().hideRecipeCategory(type);
        QuantumJeiBridge.attach(runtime, ClientKnowledge.observedItems());
    }

    @Override
    public void onRuntimeUnavailable() {
        QuantumJeiBridge.detach();
    }
}
