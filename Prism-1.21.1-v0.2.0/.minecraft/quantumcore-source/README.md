日本語の仕様・導入手順は [README_ja.md](README_ja.md) を参照してください。

# QUANTUM UNKNOWN — QuantumCore 0.2.0

Minecraft Java Edition **1.21.1**, NeoForge **21.1.262**, Java **21**.

This is an implementation source project. It is not presented as a verified playable build: this environment did not have network access or Gradle, so the mod JAR and in-game test could not be produced here. Mixin and optional JEI integration must be validated by running the Gradle build and a client.

## Implemented in source

- Seeded, persisted recipe-output mapping. It shuffles outputs among carefully bounded vanilla tool/armor families instead of shuffling every recipe into an arbitrary item, which would trivially softlock progression.
- A CraftingMenu mixin applies a mapped result to the real crafting output slot; input patterns remain unchanged so players still have to experiment.
- Every new world receives an independent recipe seed. `/quantum seed` shows it. Before the first crafting-grid map is generated, operators can use `/quantum seed set <seed>` to reproduce a recipe universe in another world.
- Saved world data stores known block/item IDs, discovered recipe patterns, stage, output map, and map hash.
- Mining, pickup-attempt, item crafting, and dimension changes trigger observations and server-to-client synchronization. The notebook records the observed crafting-grid pattern and its actual output.
- Unknown items have generic tooltips and use a shared mystery model. Unknown baked block models use a shared block model until that block ID is observed. The uniform block model intentionally hides silhouettes as well as textures, so stairs/fences/complex models look like cubes while unknown; collisions and actual block behavior are unchanged. Fluid rendering, block-entity renderers, and special render pipelines may still reveal visual clues.
- A vanilla-path safety validator checks recipe anchors, required pickaxe families, unchanged core outputs, and family-preserving output counts; if it fails, recipe output mapping falls back to identity. Randomization only permutes outputs inside a same-tier vanilla tool/armor family, and non-vanilla recipe IDs are excluded. Essential recipes (sticks, crafting table, furnace, ender eye, portal materials) remain unchanged. This is a conservative guard, not a formal theorem about every third-party mod, custom datapack, or all unusual world states.
- The vanilla recipe-book toggle is disabled because it would reveal the original recipe mappings.
- Optional JEI integration hides common vanilla recipe categories and removes unknown items from the JEI ingredient list, re-adding items as they are observed.
- `/quantum status`, `/quantum notebook`, and `/quantum seed` provide an in-game debug/research summary.

## Important scope and limitations

The first implementation randomizes outputs within vanilla **tool families** (wood, stone, iron, gold, diamond) and **armor families** (leather, chainmail, iron, gold, diamond). This changes which tool/armor each vanilla pattern produces, with separate tiers unlocking as discoveries increase. It does not indiscriminately randomize every recipe, because that is incompatible with guaranteed progression. Non-whitelisted and modded recipes are unchanged. Special recipe types such as brewing, smithing, anvil, and machine recipes are not rewritten.

JEI is optional. If JEI is not installed, that integration does not load. Other recipe viewers (EMI/REI) are not integrated yet. If a JEI/API method has changed in a locally selected JEI build, adjust the JEI bridge to that version.

A complete compile and in-game test still has to be done with internet access and Gradle. In particular, validate the Mixin refmap/injection at `CraftingMenu.slotChangedCraftingGrid`, model-baking behavior, and JEI API compatibility against the exact installed versions before publishing a release.

## Build on Windows (PowerShell)

Requirements: JDK 21 and internet access for dependency downloads. From the extracted project directory run:

```powershell
Set-ExecutionPolicy -Scope Process Bypass
.\build.ps1
```

The script uses a system Gradle if available; otherwise it downloads Gradle 8.14.3 into the project-local `.tools` folder. It runs `clean test build`. If the build succeeds, the JAR is under `build\libs\`. Put that JAR in the `mods` folder of a NeoForge 1.21.1 installation. For multiplayer, install the mod on both the server and every client. JEI itself is optional; for its restrictions, install a compatible JEI 1.21.1 NeoForge build on the client.

## Useful commands

- `/quantum seed` — show recipe seed and mapping status/hash.
- `/quantum seed set <seed>` — operator-only; use before any recipe map has been generated.
- `/quantum status` — show progression stage and known-object counts.
- `/quantum notebook` — show recent discovered recipe patterns and observations.

## Gameplay improvements included

1. **Mystery pressure without arbitrary softlocks:** only compatible output families are permuted, with vanilla progression anchors checked.
2. **Co-op science:** observations are saved to world data and synchronized to all players.
3. **Research journal:** newly discovered recipe patterns are persisted and can be reviewed with a command.
4. **Reproducible worlds:** the recipe seed is independent of the terrain seed and is stored with the generator version.
5. **Progressive uncertainty:** wooden/stone equipment output patterns start changing in the Overworld stage; advanced equipment families activate after enough discovery progress. Entering the Nether/End advances later stages.
