package dev.quantumunknown.network;

import dev.quantumunknown.client.ClientKnowledge;
import dev.quantumunknown.world.QuantumWorldData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.ArrayList;
import java.util.List;

public final class QuantumNetwork {
    private QuantumNetwork() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(ObservationSyncPayload.TYPE, ObservationSyncPayload.STREAM_CODEC);
    }

    public static ObservationSyncPayload snapshot(QuantumWorldData data) {
        List<String> blocks = new ArrayList<>(data.observedBlocksView());
        List<String> items = new ArrayList<>(data.observedItemsView());
        blocks.sort(String::compareTo);
        items.sort(String::compareTo);
        return new ObservationSyncPayload(blocks, items, data.stage().ordinal(), data.recipeSeed(), data.mappingHash());
    }

    public static void sync(ServerPlayer player, QuantumWorldData data) {
        PacketDistributor.sendToPlayer(player, snapshot(data));
    }

    public static void syncAll(ServerLevel level, QuantumWorldData data) {
        PacketDistributor.sendToAllPlayers(snapshot(data));
    }
}
