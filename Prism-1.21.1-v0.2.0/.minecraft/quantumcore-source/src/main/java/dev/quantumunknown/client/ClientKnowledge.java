package dev.quantumunknown.client;

import dev.quantumunknown.network.ObservationSyncPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientKnowledge {
    private static final Set<String> OBSERVED_BLOCKS = ConcurrentHashMap.newKeySet();
    private static final Set<String> OBSERVED_ITEMS = ConcurrentHashMap.newKeySet();
    private static volatile int stageOrdinal;
    private static volatile String recipeSeed = "";
    private static volatile String mappingHash = "";

    private ClientKnowledge() {}

    public static void apply(ObservationSyncPayload payload) {
        OBSERVED_BLOCKS.clear();
        OBSERVED_BLOCKS.addAll(payload.observedBlocks());
        OBSERVED_ITEMS.clear();
        OBSERVED_ITEMS.addAll(payload.observedItems());
        stageOrdinal = payload.stageOrdinal();
        recipeSeed = payload.recipeSeed();
        mappingHash = payload.mappingHash();
        try {
            if (net.neoforged.fml.ModList.get().isLoaded("jei")) {
                dev.quantumunknown.jei.QuantumJeiBridge.updateKnownItems(Set.copyOf(OBSERVED_ITEMS));
            }
        } catch (NoClassDefFoundError ignored) {
            // JEI API is optional; this path is only used when JEI is installed.
        }
    }

    public static boolean isBlockObserved(ResourceLocation id) {
        return id != null && OBSERVED_BLOCKS.contains(id.toString());
    }

    public static boolean isItemObserved(ResourceLocation id) {
        return id != null && OBSERVED_ITEMS.contains(id.toString());
    }

    public static Set<String> observedItems() { return Set.copyOf(OBSERVED_ITEMS); }
    public static Set<String> observedBlocks() { return Set.copyOf(OBSERVED_BLOCKS); }
    public static int stageOrdinal() { return stageOrdinal; }
    public static String recipeSeed() { return recipeSeed; }
    public static String mappingHash() { return mappingHash; }
}
