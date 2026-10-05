# High Energy — Paper Plugin

This is a real, independent Paper implementation of the High Energy system. It does not contain Fabric classes.

## Version matrix

- Minecraft/Paper **26.2**
- Compile API **paper-api 26.2.build.129-stable**
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

The plugin is `build/libs/HighEnergy-Plugin.jar`.

## Runtime

Place the JAR in a Paper 26.2 server's `plugins/` folder and start the server with Java 25. Configuration is generated at `plugins/HighEnergy/config.yml`; UUID-keyed player state is stored at `plugins/HighEnergy/players.yml`.

See the final package README for the full command, power, permission, configuration, and gameplay reference.
