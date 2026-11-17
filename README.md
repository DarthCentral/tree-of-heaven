# Worldroot (Forge 1.20.1)

Build with Java 17 and Gradle using `gradle build`.

## Colossal continuous growth
- The trunk grows until it reaches the world's maximum build height; the old 2,048-block scan cap is removed.
- Crown branches scale with trunk height, and the trunk's periodic outer rings can become much wider.
- The taproot advances downward until it encounters the bedrock floor. Bedrock is never replaced.
- Radial roots extend one new block per growth pulse from every established taproot tier. Their frontier is based on world age, not tree height, so they keep expanding after the canopy reaches the sky limit.
- Roots tunnel through supported natural terrain and stone types, but not bedrock, fluids, ores, containers, or arbitrary player builds.
- Root placement is limited to already-loaded chunks to avoid forcing huge areas of the world to load. Unloaded areas can be reached as they become loaded and growth continues.

## Important notes
- This is a custom Forge tree implementation, not a registered Dynamic Trees species.
- Minecraft's build-height floor/ceiling, loaded chunks, replaceable terrain, and server performance remain physical limits. The tree cannot grow above the sky limit or through bedrock.
- Source update has not been compiled in this environment. Run `gradle build` (or the project's CI build) before installing.
