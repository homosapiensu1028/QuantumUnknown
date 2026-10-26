package dev.quantumunknown.recipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Fail-closed checks for the randomizer's deliberately limited safe families.
 * The generator preserves the multiset of outputs in every group; core survival
 * recipes are not members of any randomizable family and therefore remain intact.
 */
public final class ProgressionValidator {
    private static final Set<String> REQUIRED_OUTPUTS = Set.of(
        "minecraft:stick", "minecraft:crafting_table", "minecraft:furnace", "minecraft:ender_eye",
        "minecraft:wooden_pickaxe", "minecraft:stone_pickaxe", "minecraft:iron_pickaxe", "minecraft:diamond_pickaxe"
    );

    private ProgressionValidator() {}

    public static boolean validate(
        ServerLevel level,
        List<RecipeHolder<CraftingRecipe>> recipes,
        Map<String, String> identity,
        Map<String, String> generated
    ) {
        Set<String> availableOutputs = new HashSet<>(identity.values());
        if (!availableOutputs.containsAll(REQUIRED_OUTPUTS)) return false;
        if (!identity.keySet().equals(generated.keySet())) return false;

        Map<String, Integer> originalCounts = frequencies(identity);
        Map<String, Integer> generatedCounts = frequencies(generated);
        // Outputs are only permuted inside each safe family, so the overall output
        // multiset is conserved. This check detects accidental item loss/duplication.
        if (!originalCounts.equals(generatedCounts)) return false;

        for (Map.Entry<String, String> entry : generated.entrySet()) {
            ResourceLocation outputId = ResourceLocation.tryParse(entry.getValue());
            if (outputId == null) return false;
            // Mappings outside approved families must remain identical.
            ResourceLocation original = ResourceLocation.tryParse(identity.get(entry.getKey()));
            if (original == null) return false;
            String originalFamily = RecipeShuffle.familyForItem(original);
            String mappedFamily = RecipeShuffle.familyForItem(outputId);
            if (originalFamily == null && !original.equals(outputId)) return false;
            if (originalFamily != null && !originalFamily.equals(mappedFamily)) return false;
        }
        return true;
    }

    private static Map<String, Integer> frequencies(Map<String, String> mapping) {
        Map<String, Integer> counts = new HashMap<>();
        for (String value : mapping.values()) counts.merge(value, 1, Integer::sum);
        return counts;
    }
}
