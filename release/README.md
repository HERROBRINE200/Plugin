# High Energy 1.0.0 — Fabric Mod and Paper Plugin

This package contains two independently implemented editions of the same High Energy gameplay system. `HighEnergy-Mod.jar` is a Fabric mod; `HighEnergy-Plugin.jar` is a native Paper plugin with Bukkit/Paper commands, listeners, scheduling, configuration, and persistence. The plugin is **not** a renamed Fabric JAR.

## Supported versions and requirements

### Fabric edition

The versions below are preserved from the supplied project's `gradle.properties`, `build.gradle`, and `fabric.mod.json`:

| Requirement | Version |
|---|---:|
| Minecraft Java Edition | **26.2** |
| Fabric Loader | **0.19.3 or newer** |
| Fabric API | **0.158.0+26.2** |
| Fabric Loom (build only) | **1.18-SNAPSHOT** |
| Java | **25** |
| Gradle wrapper (build only) | **9.7.0** |

### Paper edition

| Requirement | Version |
|---|---:|
| Minecraft Java Edition | **26.2** |
| Paper server | **26.2** |
| Paper API used to compile | **26.2.build.129-stable** |
| `api-version` in `plugin.yml` | **26.2** |
| Java | **25** |
| Gradle wrapper (build only) | **9.7.0** |

Paper forks that preserve the Paper 26.2 API may work, but Paper 26.2 is the tested/compiled target.

## Package layout

```text
HighEnergy-Final/
├── 1-Mod/
│   └── HighEnergy-Mod.jar
├── 2-Plugin/
│   └── HighEnergy-Plugin.jar
├── 3-Source-Code/
│   ├── HighEnergy-Fabric-Source.zip
│   └── HighEnergy-Paper-Source.zip
└── README.md
```

## Installation

### Fabric mod

1. Install Java 25.
2. Create or select a **Minecraft 26.2** Fabric profile/server.
3. Install **Fabric Loader 0.19.3 or newer**.
4. Put **Fabric API 0.158.0+26.2** in the profile/server's `mods` folder.
5. Put `1-Mod/HighEnergy-Mod.jar` in the same `mods` folder.
6. Start Minecraft or the dedicated server.

The Fabric mod is environment-neutral and can load on an integrated client or a dedicated server. All gameplay decisions are server-side. Player data is saved by UUID at `config/highenergy/players.properties` and is flushed periodically and during a clean server stop.

### Paper plugin

1. Install Java 25.
2. Install a **Paper 26.2** server.
3. Copy `2-Plugin/HighEnergy-Plugin.jar` to the server's `plugins` folder.
4. Start or restart the server. Do not use `/reload` for plugin upgrades.
5. Edit `plugins/HighEnergy/config.yml` if desired, then restart.

Paper player data is saved by UUID at `plugins/HighEnergy/players.yml`. It is flushed on player quit, periodically, after persistent admin/selection changes, and on plugin shutdown.

**Do not install both editions on the same server.** Use the Fabric mod on Fabric or the Paper plugin on Paper.

## Gameplay

- Default/max starting Energy: **200/200**.
- Intended regeneration: **3 Energy every 20 ticks (3 per second)**.
- Energy never regenerates above the player's saved maximum.
- Current Energy, maximum Energy, and selected power persist across restarts.
- Cooldowns are runtime combat state and intentionally reset when the server process restarts.
- A 20-segment Energy action bar is displayed once per second (configurable in Paper).
- The default selected power for a new player is **Dash**.

### Activation controls

Sneak and left-click to activate the selected power.

- **Paper:** left-click air, a block, or a living entity.
- **Fabric:** left-click a block or entity. Fabric's ordinary server event API has no empty-air left-swing callback, so sneak + right-click is retained as the reliable empty-air fallback.
- Powers that can use a clicked living target receive it. **Pull requires a living target** and does not consume Energy or begin cooldown when no target is supplied.
- Paper's optional right-click fallback is enabled by default and can be disabled in `config.yml`.

## Powers

Costs, cooldowns, effects, ranges, durations, and amplifiers are preserved from the supplied source. Minecraft effect amplifier 0 is level I, so amplifier 1 is level II, and so on.

| Selection name | Display name | Cost | Cooldown | Mechanics |
|---|---|---:|---:|---|
| `dash` | Energy Dash | 15 | 1.5s (30 ticks) | Launches the player in their look direction at 1.8 horizontal velocity and 0.45 upward velocity. |
| `strike` | Energy Strike | 20 | 2s (40 ticks) | Strength II for 4 seconds. |
| `shockwave` | Shockwave | 30 | 3s (60 ticks) | Hits living entities in a 5-block radius for 5 damage (2.5 hearts) and applies strong outward/upward knockback. |
| `shield` | Energy Shield | 25 | 5s (100 ticks) | Resistance III for 5 seconds. |
| `lightning` | Lightning Strike | 40 | 5s (100 ticks) | Calls real lightning at a clicked living target, or 8 blocks along the player's look direction. |
| `pull` | Energy Pull | 25 | 3s (60 ticks) | Pulls the clicked living target toward the player at 2.2 velocity with 0.4 upward velocity. |
| `blast` | Energy Blast | 35 | 2.5s (50 ticks) | Hits living entities in a 7-block radius for 7 damage (3.5 hearts) and applies outward/upward knockback. |
| `speed` | Speed Surge | 20 | 4s (80 ticks) | Speed IV for 6 seconds. |
| `regen` | Regeneration Burst | 30 | 5s (100 ticks) | Regeneration III for 5 seconds. |
| `overdrive` | Energy Overdrive | 80 | 15s (300 ticks) | Speed IV, Strength III, and Resistance II for 8 seconds. |

All area powers exclude the activating player. Visual particles are reproduced in both editions.

## Commands

`<language>` accepts `english`/`en` or `hindi`/`hi`. Power selection uses the names in the first column of the powers table.

| Command | Description |
|---|---|
| `/energy` | Shows Energy, maximum, selected power, activation help, and common commands. |
| `/energy powers` | Lists every power, cost, cooldown, and mechanic. |
| `/energy select <power>` | Selects and immediately saves a power. |
| `/energy guide book english` | Gives a useful eight-page English written guide. |
| `/energy guide book hindi` | Gives a useful eight-page Hindi (Romanized) written guide. |
| `/energy guide information english` | Shows English information in chat. |
| `/energy guide information hindi` | Shows Hindi information in chat. |
| `/energy admin give <player> <amount>` | Adds Energy, clamped to the player's maximum. |
| `/energy admin take <player> <amount>` | Removes Energy, clamped at zero. |
| `/energy admin set <player> <amount>` | Sets Energy, clamped between zero and maximum. |
| `/energy admin refill <player>` | Sets Energy to the player's saved maximum. |
| `/energy admin setmax <player> <amount>` | Changes the saved maximum (minimum 1); current Energy is clamped if needed. |

Paper also provides `/highenergy` as an alias for `/energy`. Admin target players must be online in both editions, matching the supplied Fabric command's player argument.

## Permissions

### Fabric

- Player commands/power use: available to players.
- `/energy admin ...`: requires vanilla command permission level **2** (normally server operators).

### Paper

| Permission | Default | Purpose |
|---|---|---|
| `highenergy.use` | `true` | Use player commands and activate powers. |
| `highenergy.admin` | `op` | Use every `/energy admin` command. |

Paper admin commands work from the server console. Player-only commands report a clear message when run from console.

## Paper configuration

Generated file: `plugins/HighEnergy/config.yml`

| Path | Default | Description |
|---|---:|---|
| `energy.default-maximum` | `200` | Starting/current maximum for new UUIDs. Existing saved maxima are not overwritten. |
| `energy.regeneration-amount` | `3` | Energy restored each regeneration interval. Set to 0 to disable regeneration. |
| `energy.regeneration-interval-ticks` | `20` | Regeneration interval; 20 ticks is approximately one second. |
| `energy.show-action-bar` | `true` | Show the Energy bar once per second. |
| `activation.require-sneaking` | `true` | Require sneak while clicking to activate. |
| `activation.allow-right-click-fallback` | `true` | Also allow sneak + right-click activation. |
| `data.autosave-interval-ticks` | `100` | Dirty player-data flush interval (minimum enforced value: 20). |

Power values are intentionally not configurable so both editions retain the supplied mechanics exactly.

## Important fixes and implementation notes

- Added UUID-based persistence for Energy, maximum Energy, and selected power.
- Corrected regeneration from the old code's accidental 3 Energy per 10 ticks (6/second) to the guide's documented **3/second**.
- Replaced the old empty/custom-named book with actual written-book content and useful pages.
- Added real left-click attack/block activation callbacks in Fabric while preserving its original sneak-use fallback.
- Added Paper left-click air/block/entity listeners and guarded plugin-caused damage from recursively activating/cancelling powers.
- Pull validates its required target **before** spending Energy or starting cooldown.
- Added same-tick activation de-duplication to prevent off-hand/double-event Energy charges.
- Added clear cooldown and insufficient-Energy feedback.
- Added clamping and overflow-safe Energy administration.
- Added tab completion, console-safe command handling, language validation, and admin confirmation messages.
- Added periodic/action shutdown saves and action-bar display.

## Building from source

Each source ZIP expands to one independent Gradle project and includes Gradle 9.7.0 wrapper scripts and wrapper JAR.

Prerequisites: an internet connection and Java 25.

### Fabric

```bash
unzip HighEnergy-Fabric-Source.zip
cd HighEnergy-Fabric
./gradlew clean build
```

Windows:

```bat
gradlew.bat clean build
```

Output: `build/libs/HighEnergy-Mod.jar` (a Loom-remapped production JAR).

### Paper

```bash
unzip HighEnergy-Paper-Source.zip
cd HighEnergy-Paper
./gradlew clean build
```

Windows:

```bat
gradlew.bat clean build
```

Output: `build/libs/HighEnergy-Plugin.jar`.

The Paper project uses only Paper API classes and has its own `plugin.yml`, Java plugin entrypoint, listeners, command executor/tab completer, scheduler, configuration, persistent data store, and power implementation.
