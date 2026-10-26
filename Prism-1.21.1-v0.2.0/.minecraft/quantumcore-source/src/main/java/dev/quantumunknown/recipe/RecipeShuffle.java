package dev.quantumunknown.recipe;

import dev.quantumunknown.QuantumCore;
import dev.quantumunknown.progression.ProgressionStage;
import dev.quantumunknown.world.QuantumWorldData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Server-side deterministic output permutation for progression-safe vanilla equipment families. */
public final class RecipeShuffle {
    private static final Pattern TOOL = Pattern.compile("^(wooden|stone|iron|golden|diamond)_(pickaxe|axe|shovel|hoe|sword)$");
    private static final Pattern ARMOR = Pattern.compile("^(leather|chainmail|iron|golden|diamond)_(helmet|chestplate|leggings|boots)$");
    private static final Map<String, ProgressionStage> FAMILY_UNLOCKS = Map.ofEntries(
        Map.entry("tool:wooden", ProgressionStage.OVERWORLD),
        Map.entry("tool:stone", ProgressionStage.OVERWORLD),
        Map.entry("tool:iron", ProgressionStage.DEEP_WORLD),
        Map.entry("tool:golden", ProgressionStage.DEEP_WORLD),
        Map.entry("tool:diamond", ProgressionStage.DEEP_WORLD),
        Map.entry("armor:leather", ProgressionStage.OVERWORLD),
        Map.entry("armor:chainmail", ProgressionStage.DEEP_WORLD),
        Map.entry("armor:iron", ProgressionStage.DEEP_WORLD),
        Map.entry("armor:golden", ProgressionStage.DEEP_WORLD),
        Map.entry("armor:diamond", ProgressionStage.DEEP_WORLD)
    );

    private record PendingRecipe(String recipeId, String expectedOutputId, String pattern) {}
    private static final ConcurrentMap<UUID, PendingRecipe> PENDING_RECIPE_BY_PLAYER = new ConcurrentHashMap<>();

    private RecipeShuffle() {}

    public static String familyForItem(ResourceLocation itemId) {
        if (itemId == null || !"minecraft".equals(itemId.getNamespace())) return null;
        Matcher tool = TOOL.matcher(itemId.getPath());
        if (tool.matches()) return "tool:" + tool.group(1);
        Matcher armor = ARMOR.matcher(itemId.getPath());
        if (armor.matches()) return "armor:" + armor.group(1);
        return null;
    }

    public static ProgressionStage unlockStageForItem(ResourceLocation itemId) {
        String family = familyForItem(itemId);
        return family == null ? ProgressionStage.ENDGAME : FAMILY_UNLOCKS.getOrDefault(family, ProgressionStage.ENDGAME);
    }

    public static boolean isRandomizedAtCurrentStage(ResourceLocation outputItemId, QuantumWorldData data) {
        String family = familyForItem(outputItemId);
        if (family == null) return false;
        return data.stage().ordinal() >= FAMILY_UNLOCKS.getOrDefault(family, ProgressionStage.ENDGAME).ordinal();
    }

    public static void ensureMapping(ServerLevel level, QuantumWorldData data) {
        if (data.mappingInitialized()) return;
        try {
            List<RecipeHolder<CraftingRecipe>> all = level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING);
            Map<String, List<RecipeHolder<CraftingRecipe>>> groups = new LinkedHashMap<>();
            Map<String, String> identity = new LinkedHashMap<>();

            for (RecipeHolder<CraftingRecipe> holder : all) {
                ItemStack result = holder.value().getResultItem(level.registryAccess());
                if (result.isEmpty()) continue;
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(result.getItem());
                String recipeId = holder.id().toString();
                String itemPath = itemId.toString();
                identity.put(recipeId, itemPath);
                String family = familyForItem(itemId);
                // Only randomize built-in recipes. Third-party recipes that happen to
                // output a vanilla tool/armor item remain untouched to preserve their own progression.
                if ("minecraft".equals(holder.id().getNamespace())
                    && "minecraft".equals(itemId.getNamespace())
                    && holder.id().getPath().equals(itemId.getPath())
                    && family != null && result.getCount() == 1) {
                    groups.computeIfAbsent(family, ignored -> new ArrayList<>()).add(holder);
                }
            }

            Map<String, String> generated = new LinkedHashMap<>(identity);
            for (Map.Entry<String, List<RecipeHolder<CraftingRecipe>>> entry : groups.entrySet()) {
                List<RecipeHolder<CraftingRecipe>> holders = entry.getValue();
                if (holders.size() < 2) continue;
                holders.sort(Comparator.comparing(h -> h.id().toString()));
                List<String> outputIds = new ArrayList<>();
                for (RecipeHolder<CraftingRecipe> holder : holders) {
                    ItemStack result = holder.value().getResultItem(level.registryAccess());
                    outputIds.add(BuiltInRegistries.ITEM.getKey(result.getItem()).toString());
                }
                RecipeSeedEngine.deterministicShuffle(outputIds, data.recipeSeed(), QuantumCore.GENERATOR_VERSION, entry.getKey());
                for (int i = 0; i < holders.size(); i++) generated.put(holders.get(i).id().toString(), outputIds.get(i));
            }

            boolean safe = ProgressionValidator.validate(level, all, identity, generated);
            if (!safe) {
                QuantumCore.LOGGER.error("Recipe mapping failed safety validation for seed {}; using identity outputs.", data.recipeSeed());
                data.setGeneratedMapping(identity, false, RecipeSeedEngine.mappingHash(identity));
            } else {
                data.setGeneratedMapping(generated, true, RecipeSeedEngine.mappingHash(generated));
                QuantumCore.LOGGER.info("Generated {} deterministic recipe output mappings for seed {} ({}).",
                    generated.size(), data.recipeSeed(), data.mappingHash());
            }
        } catch (Throwable throwable) {
            QuantumCore.LOGGER.error("Could not build recipe mapping; falling back to vanilla outputs.", throwable);
            data.forceIdentityFallback("fallback-identity");
        }
    }

    public static ItemStack randomizedResult(ServerLevel level, QuantumWorldData data, RecipeHolder<CraftingRecipe> recipe, ItemStack vanillaResult) {
        ensureMapping(level, data);
        if (!data.mappingSafe()) return vanillaResult.copy();
        ResourceLocation originalId = BuiltInRegistries.ITEM.getKey(vanillaResult.getItem());
        if (!isRandomizedAtCurrentStage(originalId, data)) return vanillaResult.copy();
        String mapped = data.mappedOutput(recipe.id().toString());
        if (mapped == null) return vanillaResult.copy();
        ResourceLocation targetId = ResourceLocation.tryParse(mapped);
        if (targetId == null) return vanillaResult.copy();
        var targetItem = BuiltInRegistries.ITEM.get(targetId);
        if (targetItem == null || targetItem == net.minecraft.world.item.Items.AIR) return vanillaResult.copy();
        // Preserve custom data/components for recipes whose mapping is identity.
        if (targetId.equals(originalId)) return vanillaResult.copy();
        return new ItemStack(targetItem, vanillaResult.getCount());
    }

    public static void setPendingRecipe(UUID playerId, String recipeId, ItemStack expectedOutput, CraftingContainer grid) {
        if (recipeId == null || expectedOutput == null || expectedOutput.isEmpty() || grid == null) {
            PENDING_RECIPE_BY_PLAYER.remove(playerId);
            return;
        }
        String expectedId = BuiltInRegistries.ITEM.getKey(expectedOutput.getItem()).toString();
        PENDING_RECIPE_BY_PLAYER.put(playerId, new PendingRecipe(recipeId, expectedId, describePattern(grid)));
    }

    private static String describePattern(CraftingContainer grid) {
        StringBuilder pattern = new StringBuilder();
        int width = grid.getWidth();
        int height = grid.getHeight();
        for (int row = 0; row < height; row++) {
            if (row > 0) pattern.append(" / ");
            for (int col = 0; col < width; col++) {
                if (col > 0) pattern.append(" | ");
                ItemStack stack = grid.getItem(row * width + col);
                pattern.append(stack.isEmpty() ? "-" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            }
        }
        return pattern.toString();
    }

    public static void clearPendingRecipe(UUID playerId) {
        PENDING_RECIPE_BY_PLAYER.remove(playerId);
    }

    public static boolean consumePendingRecipe(QuantumWorldData data, UUID playerId, ItemStack crafted) {
        PendingRecipe pending = PENDING_RECIPE_BY_PLAYER.remove(playerId);
        if (pending == null || crafted == null || crafted.isEmpty()) return false;
        String actualId = BuiltInRegistries.ITEM.getKey(crafted.getItem()).toString();
        if (!pending.expectedOutputId().equals(actualId)) return false;
        return data.observeRecipe(pending.pattern() + " => " + actualId);
    }

}
