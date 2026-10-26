package dev.quantumunknown.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Optional JEI-only bridge. Unknown item IDs are removed from JEI and their variants restored when observed. */
public final class QuantumJeiBridge {
    private static IJeiRuntime runtime;
    private static Set<String> knownItems = Set.of();
    private static final Map<String, List<ItemStack>> hiddenStacksById = new HashMap<>();

    private QuantumJeiBridge() {}

    public static synchronized void attach(IJeiRuntime jeiRuntime, Set<String> known) {
        runtime = jeiRuntime;
        knownItems = Set.copyOf(known);
        hiddenStacksById.clear();
        Minecraft.getInstance().execute(() -> {
            synchronized (QuantumJeiBridge.class) {
                if (runtime != null) applyKnowledge();
            }
        });
    }

    public static synchronized void detach() {
        runtime = null;
        knownItems = Set.of();
        hiddenStacksById.clear();
    }

    public static void updateKnownItems(Set<String> known) {
        synchronized (QuantumJeiBridge.class) {
            knownItems = Set.copyOf(known);
        }
        Minecraft.getInstance().execute(() -> {
            synchronized (QuantumJeiBridge.class) {
                if (runtime != null) applyKnowledge();
            }
        });
    }

    private static void applyKnowledge() {
        if (runtime == null) return;
        IIngredientManager manager = runtime.getIngredientManager();
        Collection<ItemStack> visible = new ArrayList<>(manager.getAllIngredients(VanillaTypes.ITEM_STACK));
        List<ItemStack> toHide = new ArrayList<>();

        for (ItemStack stack : visible) {
            if (stack.isEmpty()) continue;
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            if (knownItems.contains(id)) continue;
            ItemStack copy = stack.copy();
            hiddenStacksById.computeIfAbsent(id, ignored -> new ArrayList<>()).add(copy);
            toHide.add(stack);
        }
        if (!toHide.isEmpty()) manager.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, toHide);

        // The ingredient manager no longer returns hidden entries, so restore the saved stack variants.
        for (String id : knownItems) {
            List<ItemStack> restore = hiddenStacksById.remove(id);
            if (restore != null && !restore.isEmpty()) {
                manager.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, restore);
            }
        }
    }
}
