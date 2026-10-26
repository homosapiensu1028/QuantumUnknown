package dev.quantumunknown.mixin;

import dev.quantumunknown.recipe.RecipeShuffle;
import dev.quantumunknown.world.QuantumWorldData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {
    @Inject(method = "slotChangedCraftingGrid", at = @At("TAIL"))
    private static void quantum$applyOutputPermutation(
        AbstractContainerMenu menu,
        Level level,
        Player player,
        CraftingContainer craftSlots,
        ResultContainer resultSlots,
        @Nullable RecipeHolder<CraftingRecipe> recipe,
        CallbackInfo ci
    ) {
        if (!(level instanceof ServerLevel serverLevel) || level.isClientSide) return;
        if (recipe == null || resultSlots.getItem(0).isEmpty()) {
            RecipeShuffle.clearPendingRecipe(player.getUUID());
            return;
        }
        QuantumWorldData data = QuantumWorldData.get(serverLevel);
        ItemStack vanillaResult = resultSlots.getItem(0).copy();
        ItemStack randomized = RecipeShuffle.randomizedResult(serverLevel, data, recipe, vanillaResult);
        resultSlots.setItem(0, randomized);
        RecipeShuffle.setPendingRecipe(player.getUUID(), recipe.id().toString(), randomized, craftSlots);
    }
}
