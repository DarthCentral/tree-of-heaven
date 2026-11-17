# Worldroot (Minecraft 1.20.1 Forge)

This is a Forge source project, not a precompiled installable mod JAR.

The creative tab registration is named **Worldroot** and should appear as its own tab in the Creative inventory after the project is successfully compiled and installed. It contains the Worldroot Heart, Fragment, Wood, and Root items.

## Build
Use Java 17 and the Gradle wrapper (or a compatible Gradle installation) with internet access to resolve ForgeGradle and Minecraft dependencies. From this folder run `gradlew build` (Windows: `gradlew.bat build`). The compiled mod is written to `build/libs/`.

## Current prototype limitations
The growth implementation is experimental and does not yet implement the complete requested infinite tree behaviour or a full Dynamic Trees species integration. World height, chunk loading, and server performance also constrain growth.
