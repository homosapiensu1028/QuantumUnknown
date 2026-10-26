package dev.quantumunknown.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.quantumunknown.network.QuantumNetwork;
import dev.quantumunknown.progression.ProgressionStage;
import dev.quantumunknown.world.QuantumWorldData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.List;

public final class QuantumCommands {
    private QuantumCommands() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("quantum")
            .executes(ctx -> showStatus(ctx.getSource()))
            .then(Commands.literal("status").executes(ctx -> showStatus(ctx.getSource())))
            .then(Commands.literal("seed")
                .executes(ctx -> showSeed(ctx.getSource()))
                .then(Commands.literal("set")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("seed", StringArgumentType.greedyString())
                        .executes(ctx -> setSeed(ctx.getSource(), StringArgumentType.getString(ctx, "seed"))))))
            .then(Commands.literal("notebook").executes(ctx -> showNotebook(ctx.getSource())))
        );
    }

    private static QuantumWorldData data(CommandSourceStack source) {
        return QuantumWorldData.get(source.getServer().overworld());
    }

    private static int showSeed(CommandSourceStack source) {
        QuantumWorldData data = data(source);
        source.sendSuccess(() -> Component.literal("Recipe Seed: " + data.recipeSeed()), false);
        source.sendSuccess(() -> Component.literal("Mapping: " + (data.mappingInitialized() ? "generated" : "not generated yet")), false);
        source.sendSuccess(() -> Component.literal("Mapping hash: " + data.mappingHash()), false);
        source.sendSuccess(() -> Component.literal("Generator version: " + data.generatorVersion()), false);
        return 1;
    }

    private static int setSeed(CommandSourceStack source, String seed) {
        String clean = seed == null ? "" : seed.trim();
        if (clean.isBlank() || clean.length() > 128) {
            source.sendFailure(Component.literal("Seed must contain 1–128 non-space characters."));
            return 0;
        }
        QuantumWorldData data = data(source);
        if (!data.setRecipeSeed(clean)) {
            source.sendFailure(Component.literal("Recipe mapping has already been generated. Set the seed before the first crafting-grid use in this world."));
            return 0;
        }
        ServerLevel level = source.getServer().overworld();
        QuantumNetwork.syncAll(level, data);
        source.sendSuccess(() -> Component.literal("Recipe Seed set to: " + clean), true);
        return 1;
    }

    private static int showStatus(CommandSourceStack source) {
        QuantumWorldData data = data(source);
        ProgressionStage stage = data.stage();
        source.sendSuccess(() -> Component.literal("§6QUANTUM UNKNOWN §7| Stage: §f" + stage.name()), false);
        source.sendSuccess(() -> Component.literal("Observed blocks: " + data.observedBlocksView().size()
            + " | observed items: " + data.observedItemsView().size()
            + " | discovered recipes: " + data.discoveredRecipesView().size()), false);
        source.sendSuccess(() -> Component.literal("Recipe Seed: " + data.recipeSeed()), false);
        source.sendSuccess(() -> Component.literal("Recipe mapping: " + (data.mappingInitialized()
            ? (data.mappingSafe() ? "validated" : "identity fallback (safety check failed)") : "not generated")), false);
        source.sendSuccess(() -> Component.literal("Mapping hash: " + data.mappingHash()), false);
        source.sendSuccess(() -> Component.literal("Generator version: " + data.generatorVersion()), false);
        return 1;
    }

    private static int showNotebook(CommandSourceStack source) {
        QuantumWorldData data = data(source);
        List<String> recipes = data.discoveredRecipesView();
        source.sendSuccess(() -> Component.literal("§6Research Notebook §7— " + recipes.size() + " discovered recipe patterns recorded"), false);
        if (recipes.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No recipe patterns recorded yet. Craft an item to observe its result."), false);
            return 1;
        }
        recipes.stream().limit(12).forEach(recipe -> source.sendSuccess(() -> Component.literal("• " + recipe), false));
        if (recipes.size() > 12) source.sendSuccess(() -> Component.literal("...and " + (recipes.size() - 12) + " more."), false);
        return 1;
    }
}
