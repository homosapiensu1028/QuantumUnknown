package dev.quantumunknown;

import com.mojang.logging.LogUtils;
import dev.quantumunknown.command.QuantumCommands;
import dev.quantumunknown.network.QuantumNetwork;
import dev.quantumunknown.observation.ObservationEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(QuantumCore.MOD_ID)
public final class QuantumCore {
    public static final String MOD_ID = "quantumcore";
    public static final String GENERATOR_VERSION = "quantumcore-0.2.0";
    public static final Logger LOGGER = LogUtils.getLogger();

    public QuantumCore(IEventBus modBus) {
        modBus.addListener(QuantumNetwork::registerPayloads);
        NeoForge.EVENT_BUS.addListener(QuantumCommands::register);
        NeoForge.EVENT_BUS.register(new ObservationEvents());
        LOGGER.info("Quantum Unknown 0.2.0 initialized. Recipe output randomization is server-side.");
    }
}
