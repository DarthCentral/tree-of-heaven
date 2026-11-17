# Worldroot (Minecraft 1.20.1 Forge)

A Forge addon concept for Dynamic Trees 1.20.1-1.4.11. The supplied Dynamic Trees jar was inspected; its manifest reports `Implementation-Version: 1.20.1-1.4.11` and its mod id is `dynamictrees`.

## Build

Requires Java 17 and internet access for Gradle/ForgeGradle dependencies:

```bash
gradle build
```

The output will be `build/libs/worldroot-1.0.0.jar`.

## Current implementation status

This is an initial source project, not a compiled or game-tested jar. It registers a Creative-tab-only heart item, a fragment item, custom wood/root blocks, a timed trunk-growth pulse, water-accelerated pulses, downward roots, and fragment replanting restricted to Creative players.

Important limitations in this prototype:
- The tree growth is a custom block-based system alongside Dynamic Trees; it does not yet register a Dynamic Trees `Species` or use Dynamic Trees branch/leaf rendering.
- Crown growth is a simple branch prototype, not a mature procedural canopy.
- Regrowth of removed wood/root sections, persistent per-tree growth counters, and true unlimited horizontal root/crown propagation still need implementation.
- Minecraft world-height and chunk-loading limits still apply; no tree can literally grow beyond loaded/valid world coordinates.
- The target rates are prototype scheduling targets and need in-game profiling/tuning.

Dynamic Trees is declared as a required companion mod in `mods.toml`.
