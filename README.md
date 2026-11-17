# Worldroot (Forge 1.20.1)

This is the Worldroot source project. Build with Java 17 and Gradle using `gradle build`.

## Growth update
- The heart schedules a growth pulse every 1–2 ticks (water nearby accelerates it).
- The trunk grows upward one block per pulse.
- The crown now adds multiple branches at different trunk heights, secondary forks, and dense persistent oak-leaf clusters.
- Roots extend down through common soft terrain and branch horizontally from deeper root sections. They do not replace stone, bedrock, fluids, or player-placed blocks.

## Important limitations
- This is a custom Forge tree implementation, not a registered Dynamic Trees species. Dynamic Trees is declared as a required runtime companion in `mods.toml`, but its growth API is not yet integrated.
- Growth is not truly infinite: Minecraft build height, loaded chunks, available space, and server performance still apply.
- This update is source-only and has not been compiled in this environment. Run the GitHub Actions build and check for errors before installing.
