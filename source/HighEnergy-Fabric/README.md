# High Energy — Fabric Mod

This is the independently buildable Fabric edition of High Energy.

## Fixed version matrix

The supplied source project specified these exact versions, and this project preserves them:

- Minecraft **26.2**
- Fabric Loader **0.19.3** or newer
- Fabric API **0.158.0+26.2**
- Fabric Loom **1.18-SNAPSHOT**
- Java **25**
- Gradle wrapper **9.7.0**

## Build

On Linux/macOS:

```bash
./gradlew clean build
```

On Windows:

```bat
gradlew.bat clean build
```

The production mod is `build/libs/HighEnergy-Mod.jar`. Minecraft 26.2 is deobfuscated, so this project correctly uses Loom's no-remap plugin.

## Runtime

Install Fabric Loader for Minecraft 26.2, put the matching Fabric API JAR and `HighEnergy-Mod.jar` in `mods/`, and start the game/server with Java 25. Persistent player data is stored in `config/highenergy/players.properties`.

See the final package README for the full command, power, installation, and gameplay reference.
