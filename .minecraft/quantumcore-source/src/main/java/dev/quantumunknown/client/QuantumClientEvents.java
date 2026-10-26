package dev.quantumunknown.client;

import dev.quantumunknown.QuantumCore;
import dev.quantumunknown.network.ObservationSyncPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.network.RegisterClientPayloadHandlersEvent;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = QuantumCore.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class QuantumClientEvents {
    private QuantumClientEvents() {}

    private static ResourceLocation unknownModelResource() {
        // ResourceLocation paths are relative to assets/<namespace>/models/.
        return ResourceLocation.fromNamespaceAndPath(QuantumCore.MOD_ID, "block/unknown");
    }

    private static ModelResourceLocation unknownModelLocation() {
        return new ModelResourceLocation(unknownModelResource(), "normal");
    }

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(unknownModelResource());
    }

    @SubscribeEvent
    public static void modifyBakedModels(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        BakedModel unknown = models.get(unknownModelLocation());
        if (unknown == null) {
            unknown = models.entrySet().stream()
                .filter(e -> e.getKey().getNamespace().equals(QuantumCore.MOD_ID) && e.getKey().getPath().endsWith("unknown"))
                .map(Map.Entry::getValue).findFirst().orElse(null);
        }
        if (unknown == null) {
            QuantumCore.LOGGER.error("Could not find the shared unknown block model; observation texture masking will be skipped.");
            return;
        }

        Map<ModelResourceLocation, ResourceLocation> blockModels = new HashMap<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (block == Blocks.AIR) continue;
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                if (state.isAir() || !state.getFluidState().isEmpty()) continue;
                ModelResourceLocation location = BlockModelShaper.stateToModelLocation(state);
                if (models.containsKey(location)) blockModels.putIfAbsent(location, blockId);
            }
        }
        for (Map.Entry<ModelResourceLocation, ResourceLocation> entry : blockModels.entrySet()) {
            BakedModel original = models.get(entry.getKey());
            if (original == null || original == unknown || entry.getKey().equals(unknownModelLocation())) continue;
            models.put(entry.getKey(), new ObservationBakedModel(original, unknown, entry.getValue(), true));
        }

        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
            ModelResourceLocation location = new ModelResourceLocation(itemId, "inventory");
            BakedModel original = models.get(location);
            if (original != null && original != unknown && !location.equals(unknownModelLocation())) {
                models.put(location, new ObservationBakedModel(original, unknown, itemId, false));
            }
        }
        QuantumCore.LOGGER.info("Applied observation model wrappers to {} block models and item models.", blockModels.size());
    }

    @SubscribeEvent
    public static void registerPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(ObservationSyncPayload.TYPE, (payload, context) -> ClientKnowledge.apply(payload));
    }
}
