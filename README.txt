WRA CLIENT - Minecraft 1.20.1 Forge 47.4.x - SOURCE PROJECT

IMPORTANT: This is SOURCE, not a verified playable JAR. Minecraft must be compiled
with ForgeGradle. Internet and enough disk space are required for initial dependencies.

SETUP
1. Install Java JDK 17 (Java 25 or 21 may not work reliably with this ForgeGradle setup).
2. Install IntelliJ IDEA and open this project folder (where build.gradle is).
3. Set Settings > Build Tools > Gradle > Gradle JVM to Java 17.
4. Sync Gradle, then run Gradle Tasks > fg_runs > runClient or the generated runClient configuration.
5. Build Gradle task: build. Result expected in build/libs after successful compilation.
6. The mod .jar, not the entire ZIP, goes in Minecraft Forge 1.20.1 mods folder.

MENU
Right Shift opens/closes the menu. ESC closes. Mouse left-click toggles each feature.
Five tabs: Combat, Movement, Render, Player, World. 20 implemented module behaviors.

IMPORTANT LIMITATIONS
- This has not been compiled or tested in Minecraft here because this runtime has no
  ForgeGradle dependencies and cannot resolve Forge Maven hosts.
- Menu uses readable in-game font; no external font/assets required.
- Server permissions, anti-cheat rules and synchronization can limit effects.
- Auto Eat uses held-key state and may interact with manual food usage.
- Some auto input options are intentionally client-focused.
- Forge 1.20.1 is based on Java 17, not Java 25.

When reporting errors, include full Gradle stacktrace or latest.log.

GRADLE WRAPPER
The ZIP does not bundle third-party Gradle binaries. On Windows, run
GRADLE_WRAPPER_KUR.bat to fetch official Forge MDK wrapper files online.
This is optional if you already have an installed Gradle build environment.
For transparency, inspect the .bat script before running it.

AUTOMATED GITHUB BUILD (NO LOCAL GRADLE REQUIRED)
See BURADAN_JAR_OLUSTUR.txt and .github/workflows/build-forge-jar.yml.
The workflow provisions Java 17 and Gradle 8.8, then compiles via official ForgeGradle.
It downloads Forge and Minecraft dependency files on GitHub's runner, not on your C: drive.
The artifact is available only after a successful workflow.
