# High Energy

Energy / powers combat system for Minecraft, available as **two independent, fully
buildable projects**:

| | Project | Output |
|---|---|---|
| 1 | `source/HighEnergy-Fabric/` — Fabric mod for **Minecraft 26.2** | `HighEnergy-Mod.jar` |
| 2 | `source/HighEnergy-Paper/` — Paper server plugin | `HighEnergy-Plugin.jar` |

Pre-built, CI-produced jars live in [`dist/`](dist). The packaged deliverable
(`HighEnergy-Final.zip`) contains both jars, both source projects and the full
documentation.

---

## 1. Versions and requirements

### Fabric mod

| Item | Value |
|---|---|
| Minecraft | **26.2** (unchanged from the original project) |
| Fabric Loader | **0.19.3** or newer |
| Fabric API | **0.158.0+26.2** |
| Fabric Loom | **1.18-SNAPSHOT** |
| Gradle | **9.7+** (Loom 1.18 requires it; CI uses the current release) |
| Java | **25** |
| Side | server-side logic; `environment: "*"` so it can be installed on client or server |

> Minecraft 26.1+ ships **unobfuscated**, so the project no longer declares
> `loom.officialMojangMappings()` — that call made the original build fail with
> *“Failed to find official mojang mappings for 26.2”*.

### Paper plugin

| Item | Value |
|---|---|
| Server | **Paper 1.21+** (`api-version: 1.21`); also runs on Purpur/other Paper forks |
| API | `io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT` |
| Java | **21+** |
| Gradle | 8.10+ |
| Dependencies | none (no external libraries needed) |

---

## 2. Installation

**Fabric mod**

1. Install Fabric Loader 0.19.3+ for Minecraft 26.2.
2. Drop **Fabric API 0.158.0+26.2** into `mods/`.
3. Drop `HighEnergy-Mod.jar` into `mods/`.
4. Start the game/server. Player data is written to `<world>/highenergy-players.json`.

**Paper plugin**

1. Use a Paper 1.21+ server running Java 21+.
2. Drop `HighEnergy-Plugin.jar` into `plugins/`.
3. Start the server once — it generates `plugins/HighEnergy/config.yml` and
   `plugins/HighEnergy/playerdata.yml`.
4. Edit `config.yml` and run `/energy reload` to apply changes.

---

## 3. Commands

| Command | Description | Permission (Paper) | Level (Fabric) |
|---|---|---|---|
| `/energy` | Your Energy bar, selected power and the guide summary | `highenergy.use` | all |
| `/energy powers` | List all powers with cost and cooldown | `highenergy.use` | all |
| `/energy select <power>` | Select the power you activate | `highenergy.use` | all |
| `/energy guide book <english\|hindi>` | Receive the written guide book | `highenergy.use` | all |
| `/energy guide information <english\|hindi>` | Print the guide in chat | `highenergy.use` | all |
| `/energy help` *(Paper)* | Command overview | `highenergy.use` | – |
| `/energy admin give <player> <amount>` | Add Energy | `highenergy.admin` | 2 |
| `/energy admin take <player> <amount>` | Remove Energy | `highenergy.admin` | 2 |
| `/energy admin set <player> <amount>` | Set Energy | `highenergy.admin` | 2 |
| `/energy admin refill <player>` | Refill to maximum | `highenergy.admin` | 2 |
| `/energy admin setmax <player> <amount>` | Set maximum Energy | `highenergy.admin` | 2 |
| `/energy reload` *(Paper)* | Reload `config.yml` | `highenergy.admin` | – |

Paper aliases: `/he`, `/highenergy`. Both versions have tab-completion for
sub-commands, power names, languages and online players.

### Permissions (Paper)

| Node | Default | Grants |
|---|---|---|
| `highenergy.use` | everyone | `/energy`, power selection, power activation |
| `highenergy.admin` | op | `/energy admin ...`, `/energy reload` |
| `highenergy.*` | op | both of the above |

On Fabric the admin branch requires vanilla permission level **2**
(`Commands.LEVEL_GAMEMASTERS`), exactly as in the original mod.

---

## 4. Powers

Activate the selected power with **Sneak (Shift) + click**.
On Paper both sneak + right-click and sneak + left-click work (configurable);
on Fabric it is the sneak + *use* interaction. Clicking an entity while sneaking
targets that entity (used by Energy Pull and Lightning Strike).

| Power id | Display name | Energy cost | Cooldown | Effect |
|---|---|---|---|---|
| `dash` | Energy Dash | 15 | 30 ticks (1.5 s) | Launch forward (1.8 horizontal / 0.45 vertical) + spark burst |
| `strike` | Energy Strike | 20 | 40 ticks (2.0 s) | Strength II for 80 ticks (4 s) + crit particles |
| `shockwave` | Shockwave | 30 | 60 ticks (3.0 s) | 5-block radius: 5 damage + knockback (1.8 / 0.7) |
| `shield` | Energy Shield | 25 | 100 ticks (5.0 s) | Resistance III for 100 ticks (5 s) |
| `lightning` | Lightning Strike | 40 | 100 ticks (5.0 s) | Lightning bolt on the target, or 8 blocks ahead |
| `pull` | Energy Pull | 25 | 60 ticks (3.0 s) | Pulls the clicked entity towards you (2.2 / 0.4) |
| `blast` | Energy Blast | 35 | 50 ticks (2.5 s) | 7-block radius: 7 damage + knockback (1.5 / 0.5) + flames |
| `speed` | Speed Surge | 20 | 80 ticks (4.0 s) | Speed IV for 120 ticks (6 s) |
| `regen` | Regeneration Burst | 30 | 100 ticks (5.0 s) | Regeneration III for 100 ticks (5 s) |
| `overdrive` | Energy Overdrive | 80 | 300 ticks (15 s) | Speed IV + Strength III + Resistance II for 160 ticks (8 s) |

### Energy

* Maximum Energy: **200** per player (per-player maximum changeable with `/energy admin setmax`).
* Regeneration: **+3 Energy every 10 ticks** (every 0.5 s → 6 Energy/second).
* Energy, maximum Energy and the selected power **persist across restarts**.
* Activating a power that you cannot pay for is refused and costs no cooldown.

---

## 5. Configuration (Paper)

`plugins/HighEnergy/config.yml`:

```yaml
energy:
  default-max: 200            # starting / default maximum Energy
  regen-amount: 3             # Energy restored per interval
  regen-interval-ticks: 10    # interval length in ticks
  action-bar: true            # show the Energy bar while sneaking

activation:
  require-sneak: true
  sneak-right-click: true
  sneak-left-click: true

storage:
  auto-save-interval-ticks: 6000   # 5 minutes (also saves on quit/shutdown)

powers:                        # per-power overrides
  dash:
    cost: 15
    cooldown-ticks: 30
  # ... all ten powers
```

The Fabric mod has no config file; its values are the same defaults
(`EnergyManager.MAX_DEFAULT`, `REGEN_AMOUNT`, `REGEN_INTERVAL_TICKS`, `Power`).

---

## 6. Building

Both projects are completely independent.

```bash
# Fabric mod  (Java 25, Gradle 9.7+)
cd source/HighEnergy-Fabric
gradle build
# -> build/libs/HighEnergy-Mod-1.0.0.jar   (rename to HighEnergy-Mod.jar)

# Paper plugin  (Java 21, Gradle 8.10+)
cd source/HighEnergy-Paper
gradle build
# -> build/libs/HighEnergy-Plugin.jar
```

The GitHub Actions workflow [`.github/workflows/build.yml`](.github/workflows/build.yml)
builds both projects on every push and publishes the resulting jars to `dist/`.

---

## 7. What was fixed compared to the original project

* **Persistence** — Energy, maximum Energy and the selected power survive a restart
  (JSON file on Fabric, `playerdata.yml` on Paper). Previously everything lived in a
  `HashMap` and was lost on shutdown.
* **Build** — the original Fabric build could not resolve mappings for 26.2
  (`loom.officialMojangMappings()` no longer exists) and used a `settings.gradle`
  that blocked Loom's own repositories. Also updated to the 26.2 APIs
  (`EntityTypes.LIGHTNING_BOLT`, `Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)`,
  `ServerLevel` from `player.level()`).
* **Guide book** — used to be an empty written book; it now contains real pages
  (intro, how to play, all powers with cost/cooldown, command list) in English and Hindi.
* **Documentation mismatch** — the old guide claimed “Shift + Left Click” and
  “3/sec” regeneration while the code used the *use* interaction and 3 per 10 ticks.
  Texts now describe the real behaviour, and Paper additionally accepts sneak + left-click.
* **Power activation** — Energy Pull no longer consumes Energy and starts a cooldown
  when no target was clicked; the Dash/knockback velocities are now actually sent to
  clients (`hurtMarked`).
* **Cooldowns** — remaining time is reported to the player and cooldown maps are cleared
  on disconnect instead of leaking memory.
* **Admin commands** — all five sub-commands give feedback to the sender *and* the
  target, validate their input, and immediately persist the change.
* **Commands** — added tab-completion, `/energy help`, `/energy reload` (Paper) and
  console-safe handling.
