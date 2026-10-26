package dev.quantumunknown.network;

import dev.quantumunknown.QuantumCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record ObservationSyncPayload(
    List<String> observedBlocks,
    List<String> observedItems,
    int stageOrdinal,
    String recipeSeed,
    String mappingHash
) implements CustomPacketPayload {
    public static final Type<ObservationSyncPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(QuantumCore.MOD_ID, "observation_sync")
    );

    private static final StreamCodec<ByteBuf, List<String>> STRING_LIST_CODEC =
        ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8);

    public static final StreamCodec<ByteBuf, ObservationSyncPayload> STREAM_CODEC = StreamCodec.composite(
        STRING_LIST_CODEC, ObservationSyncPayload::observedBlocks,
        STRING_LIST_CODEC, ObservationSyncPayload::observedItems,
        ByteBufCodecs.VAR_INT, ObservationSyncPayload::stageOrdinal,
        ByteBufCodecs.STRING_UTF8, ObservationSyncPayload::recipeSeed,
        ByteBufCodecs.STRING_UTF8, ObservationSyncPayload::mappingHash,
        ObservationSyncPayload::new
    );

    public ObservationSyncPayload {
        observedBlocks = List.copyOf(observedBlocks);
        observedItems = List.copyOf(observedItems);
        recipeSeed = recipeSeed == null ? "" : recipeSeed;
        mappingHash = mappingHash == null ? "" : mappingHash;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
