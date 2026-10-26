package dev.quantumunknown.observation;

import dev.quantumunknown.network.QuantumNetwork;
import dev.quantumunknown.progression.ProgressionStage;
import dev.quantumunknown.world.QuantumWorldData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

public final class ObservationEvents {
    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getPlayer() instanceof ServerPlayer player)) return;
        String blockId = BuiltInRegistries.BLOCK.getKey(event.getState().getBlock()).toString();
        QuantumWorldData data = QuantumWorldData.get(level);
        boolean changed = data.observeBlock(blockId);
        var blockItem = event.getState().getBlock().asItem();
        if (blockItem != net.minecraft.world.item.Items.AIR) {
            changed |= data.observeItem(BuiltInRegistries.ITEM.getKey(blockItem).toString());
        }
        if (changed) {
            player.displayClientMessage(Component.literal("OBSERVED BLOCK: ").append(Component.translatable(event.getState().getBlock().getDescriptionId())), true);
            QuantumNetwork.syncAll(level, data);
        }
    }

    @SubscribeEvent
    public void onPickup(ItemEntityPickupEvent.Pre event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        ItemStack stack = event.getItemEntity().getItem();
        if (stack.isEmpty()) return;
        QuantumWorldData data = QuantumWorldData.get(level);
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (data.observeItem(id)) {
            player.displayClientMessage(Component.literal("OBSERVED ITEM: ").append(stack.getHoverName()), true);
            QuantumNetwork.syncAll(level, data);
        }
    }

    @SubscribeEvent
    public void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) return;
        ItemStack stack = event.getCrafting();
        if (stack.isEmpty()) return;
        QuantumWorldData data = QuantumWorldData.get(level);
        boolean itemChanged = data.observeItem(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        boolean recipeChanged = dev.quantumunknown.recipe.RecipeShuffle.consumePendingRecipe(data, player.getUUID(), stack);
        if (recipeChanged) {
            player.displayClientMessage(Component.literal("OBSERVED RECIPE PATTERN — ")
                .append(Component.literal(data.discoveredRecipesView().get(0))), true);
        }
        if (itemChanged || recipeChanged) QuantumNetwork.syncAll(level, data);
    }

    @SubscribeEvent
    public void onItemUse(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) return;
        ItemStack stack = event.getItem();
        if (stack.isEmpty()) return;
        QuantumWorldData data = QuantumWorldData.get(level);
        if (data.observeItem(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())) {
            QuantumNetwork.syncAll(level, data);
        }
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) return;
        QuantumNetwork.sync(player, QuantumWorldData.get(level));
    }

    @SubscribeEvent
    public void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) return;
        QuantumWorldData data = QuantumWorldData.get(level);
        boolean changed = false;
        if (event.getTo().equals(Level.NETHER)) changed = data.advanceTo(ProgressionStage.NETHER);
        else if (event.getTo().equals(Level.END)) changed = data.advanceTo(ProgressionStage.ENDGAME);
        if (changed) QuantumNetwork.syncAll(level, data);
    }
}
