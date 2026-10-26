package dev.quantumunknown.client;

import dev.quantumunknown.QuantumCore;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = QuantumCore.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class QuantumTooltipEvents {
    private QuantumTooltipEvents() {}

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        var stack = event.getItemStack();
        if (stack.isEmpty()) return;
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (ClientKnowledge.isItemObserved(id)) return;
        event.getToolTip().clear();
        event.getToolTip().add(Component.translatable("quantumcore.item.unknown"));
        event.getToolTip().add(Component.translatable("quantumcore.item.observe_hint"));
    }
}
