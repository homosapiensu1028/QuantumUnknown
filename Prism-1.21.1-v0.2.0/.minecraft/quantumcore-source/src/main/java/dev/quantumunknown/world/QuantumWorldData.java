package dev.quantumunknown.world;

import dev.quantumunknown.QuantumCore;
import dev.quantumunknown.progression.ProgressionStage;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class QuantumWorldData extends SavedData {
    public static final String DATA_ID = "quantum_unknown_world";
    public static final Factory<QuantumWorldData> FACTORY = new Factory<>(
        QuantumWorldData::new,
        QuantumWorldData::load,
        null
    );

    private String recipeSeed = UUID.randomUUID().toString().replace("-", "");
    private String generatorVersion = QuantumCore.GENERATOR_VERSION;
    private int stageOrdinal = ProgressionStage.AWAKENING.ordinal();
    private boolean mappingInitialized;
    private boolean mappingSafe = true;
    private String mappingHash = "not-generated";
    private final Map<String, String> outputMapping = new LinkedHashMap<>();
    private final Set<String> observedBlocks = new LinkedHashSet<>();
    private final Set<String> observedItems = new LinkedHashSet<>();
    private final List<String> discoveredRecipes = new ArrayList<>();

    public QuantumWorldData() {
        setDirty();
    }

    public static QuantumWorldData get(ServerLevel anyLevel) {
        ServerLevel overworld = anyLevel.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(FACTORY, DATA_ID);
    }

    private static QuantumWorldData load(CompoundTag tag, HolderLookup.Provider registries) {
        QuantumWorldData data = new QuantumWorldData();
        data.recipeSeed = tag.getString("RecipeSeed");
        if (data.recipeSeed.isBlank()) data.recipeSeed = UUID.randomUUID().toString().replace("-", "");
        data.generatorVersion = tag.getString("GeneratorVersion");
        if (data.generatorVersion.isBlank()) data.generatorVersion = QuantumCore.GENERATOR_VERSION;
        data.stageOrdinal = tag.getInt("StageOrdinal");
        data.mappingInitialized = tag.getBoolean("MappingInitialized");
        data.mappingSafe = !tag.contains("MappingSafe") || tag.getBoolean("MappingSafe");
        data.mappingHash = tag.getString("MappingHash");
        readSet(tag.getList("ObservedBlocks", 8), data.observedBlocks);
        readSet(tag.getList("ObservedItems", 8), data.observedItems);
        readSet(tag.getList("DiscoveredRecipes", 8), data.discoveredRecipes);
        CompoundTag mappingTag = tag.getCompound("OutputMapping");
        for (String key : mappingTag.getAllKeys()) data.outputMapping.put(key, mappingTag.getString(key));
        data.setDirty();
        return data;
    }

    private static void readSet(ListTag list, Set<String> target) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) instanceof StringTag value) target.add(value.getAsString());
        }
    }

    private static void readSet(ListTag list, List<String> target) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) instanceof StringTag value && !target.contains(value.getAsString())) target.add(value.getAsString());
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putString("RecipeSeed", recipeSeed);
        tag.putString("GeneratorVersion", generatorVersion);
        tag.putInt("StageOrdinal", stageOrdinal);
        tag.putBoolean("MappingInitialized", mappingInitialized);
        tag.putBoolean("MappingSafe", mappingSafe);
        tag.putString("MappingHash", mappingHash == null ? "" : mappingHash);
        tag.put("ObservedBlocks", toListTag(observedBlocks));
        tag.put("ObservedItems", toListTag(observedItems));
        tag.put("DiscoveredRecipes", toListTag(discoveredRecipes));
        CompoundTag mappingTag = new CompoundTag();
        outputMapping.forEach(mappingTag::putString);
        tag.put("OutputMapping", mappingTag);
        return tag;
    }

    private static ListTag toListTag(Iterable<String> values) {
        ListTag list = new ListTag();
        for (String value : values) list.add(StringTag.valueOf(value));
        return list;
    }

    public String recipeSeed() { return recipeSeed; }
    public String generatorVersion() { return generatorVersion; }
    public ProgressionStage stage() { return ProgressionStage.fromOrdinal(stageOrdinal); }
    public boolean mappingInitialized() { return mappingInitialized; }
    public boolean mappingSafe() { return mappingSafe; }
    public String mappingHash() { return mappingHash; }
    public Map<String, String> outputMappingView() { return Collections.unmodifiableMap(outputMapping); }
    public String mappedOutput(String recipeId) { return outputMapping.get(recipeId); }
    public Set<String> observedBlocksView() { return Collections.unmodifiableSet(observedBlocks); }
    public Set<String> observedItemsView() { return Collections.unmodifiableSet(observedItems); }
    public List<String> discoveredRecipesView() { return Collections.unmodifiableList(discoveredRecipes); }
    public int observedCount() { return observedBlocks.size() + observedItems.size(); }

    public boolean observeBlock(String id) {
        if (id == null || id.isBlank() || !observedBlocks.add(id)) return false;
        advanceByDiscoveryCount();
        setDirty();
        return true;
    }

    public boolean observeItem(String id) {
        if (id == null || id.isBlank() || !observedItems.add(id)) return false;
        advanceByDiscoveryCount();
        setDirty();
        return true;
    }

    public boolean observeRecipe(String id) {
        if (id == null || id.isBlank() || discoveredRecipes.contains(id)) return false;
        discoveredRecipes.add(0, id);
        while (discoveredRecipes.size() > 40) discoveredRecipes.remove(discoveredRecipes.size() - 1);
        setDirty();
        return true;
    }

    private void advanceByDiscoveryCount() {
        int count = observedCount();
        if (stageOrdinal < ProgressionStage.OVERWORLD.ordinal() && count >= 5) stageOrdinal = ProgressionStage.OVERWORLD.ordinal();
        if (stageOrdinal < ProgressionStage.DEEP_WORLD.ordinal() && count >= 20) stageOrdinal = ProgressionStage.DEEP_WORLD.ordinal();
    }

    public boolean advanceTo(ProgressionStage stage) {
        if (stage.ordinal() <= stageOrdinal) return false;
        stageOrdinal = stage.ordinal();
        setDirty();
        return true;
    }

    public boolean setRecipeSeed(String value) {
        if (mappingInitialized || value == null || value.isBlank()) return false;
        recipeSeed = value.trim();
        generatorVersion = QuantumCore.GENERATOR_VERSION;
        outputMapping.clear();
        mappingHash = "not-generated";
        setDirty();
        return true;
    }

    public void setGeneratedMapping(Map<String, String> map, boolean safe, String hash) {
        outputMapping.clear();
        outputMapping.putAll(map);
        generatorVersion = QuantumCore.GENERATOR_VERSION;
        mappingInitialized = true;
        mappingSafe = safe;
        mappingHash = hash == null ? "" : hash;
        setDirty();
    }

    public void forceIdentityFallback(String hash) {
        outputMapping.clear();
        generatorVersion = QuantumCore.GENERATOR_VERSION;
        mappingInitialized = true;
        mappingSafe = false;
        mappingHash = hash == null ? "fallback-identity" : hash;
        setDirty();
    }
}
