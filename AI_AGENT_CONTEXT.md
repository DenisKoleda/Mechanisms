# Mechanisms AI Agent Context

Use this file as portable context for AI agents working on this project.

## Project

- Project path: `D:\Documents\Minecraft\Mechanisms`
- Plugin name: `Mechanisms`
- Target: Paper/Purpur Minecraft `26.2`
- Java: `25`, not Java 8
- Known local Java runtime:
  `C:\Users\denis\AppData\Roaming\.minecraft\runtime\java-runtime-epsilon\windows\java-runtime-epsilon\bin\java.exe`
- Local dev server:
  `D:\Documents\Minecraft\server-dev`
- Local dev port:
  `25566`
- Production `/srv/minecraft` is out of scope. Do not SSH or deploy to production unless explicitly requested.
- Production was previously vanilla `26.2`, so this Paper/Purpur plugin is local-only until a separate migration happens.

## Build And Install

Build from the project root:

```powershell
cd D:\Documents\Minecraft\Mechanisms
.\scripts\gradle-local.ps1 build
```

Built jar:

```text
D:\Documents\Minecraft\Mechanisms\build\libs\Mechanisms-0.1.0.jar
```

Install only to local dev:

```powershell
.\scripts\install-server-dev.ps1
```

Installed jar:

```text
D:\Documents\Minecraft\server-dev\plugins\Mechanisms.jar
```

Preferred non-interactive selftest:

```powershell
cd D:\Documents\Minecraft\server-dev
java -Dmechanisms.selftestOnStartup=true -Dmechanisms.stopAfterSelftest=true -Xms1G -Xmx2G -jar paper-26.2-23.jar nogui
```

After any server run, verify `D:\Documents\Minecraft\server-dev\logs\latest.log`.

## Architecture

Preserve the existing structure:

- `MechanismsPlugin` - plugin entrypoint
- `module/MechanismModule` - module lifecycle
- `block/*` - special block identity, registry, drops, channels, upgrades, sides
- `logistics/*` - networks, route search, filters, transfer transactions, pending recovery
- `listener/*` - Bukkit events and GUI listeners
- `ui/*` - menu, wrench/settings GUI, filter GUI, network inspector, status rendering
- `crafting/*` - recipes and recipe profiles
- `selftest/*` - live plugin self-test
- `visual/*` - particles and ItemDisplay visual echoes
- `config/*` - config and crafting profile parsing
- `command/*` - `/mech` command tree

Read `README.md`, `TEST_REPORT.md`, `build.gradle.kts`, `src/main/resources/plugin.yml`, and the relevant package before editing.

## Commands

Current command surface:

```text
/mech menu
/mech give
/mech recipes
/mech network
/mech doctor
/mech log last [limit]
/mech log block [limit]
/mech perf
/mech wand
/mech wrench
/mech reload
/mech list
/mech stats
/mech debug on|off
/mech selftest [keep]
/mech help
```

`/mech give` is a compatibility alias for `/mech menu`.

Receiving items is limited to creative players or players with `mechanisms.give` / `mechanisms.admin`. Normal players should see recipes and available mechanisms, not receive free items.

Permissions:

```text
mechanisms.use
mechanisms.admin
mechanisms.give
mechanisms.reload
mechanisms.debug
```

## Config And Data

Main runtime files:

```text
plugins/Mechanisms/config.yml
plugins/Mechanisms/messages.yml
plugins/Mechanisms/data.yml
plugins/Mechanisms/pending.yml
```

Defaults live under `src/main/resources`.

Player-facing text belongs in `messages.yml` with UTF-8 Russian defaults. Mechanics and tuning belong in `config.yml`.

Important config defaults:

```yaml
logistics:
  tickInterval: 10
  itemsPerTransfer: 8
  fastPipeItemsPerTransfer: 16
  expressPipeItemsPerTransfer: 32
  maxTransfersPerTick: 32
  maxNetworkNodes: 256
  maxRouteLength: 128
  allowCrossChunk: true
  allowUnloadedChunks: false
  defaultFilterMode: WHITELIST
  exactMetaDefault: false
visuals:
  enabled: true
  particles: true
  itemDisplayEcho: true
  itemDisplayScale: 0.35
  sounds: false
recipes:
  enabled: true
  profile: normal
upgrades:
  speedBonusPerLevel: 4
  stackBonusPerLevel: 8
  priorityBonusPerLevel: 10
  rangeBonusPerLevel: 32
  maxFilterPages: 4
materials:
  extractor: HOPPER
  pipe: COPPER_GRATE
  pipeFast: EXPOSED_COPPER_GRATE
  pipeExpress: OXIDIZED_COPPER_GRATE
  router: TARGET
  inserter: DROPPER
  overflow: BARREL
  trash: CAULDRON
```

## Logistics Invariants

- Registered mechanism blocks are the graph source of truth.
- Do not scan the whole world every tick.
- Rebuild graph/index on place, break, reload, and explicit admin operations.
- Index by world/chunk/location.
- Route search is BFS/graph-based and must respect `maxNetworkNodes` and `maxRouteLength`.
- Route caches must be invalidated when registry/network data changes.
- Extractors must be processed round-robin and capped by `maxTransfersPerTick`.
- Pipe channels isolate adjacent pipe networks.
- Route amount is limited by the slowest pipe tier on that route.
- Empty filters allow all items.
- Overflow and trash are fallback destinations: normal inserters first, overflow second, trash last.
- Trash must stay safety-gated; disabled trash must never delete items.

## Transaction Invariants

Inventory transfer must be transactional:

1. choose source stack and amount;
2. find route and valid destination;
3. verify destination capacity;
4. remove from source;
5. add to destination;
6. return unexpected leftovers to source;
7. persist unrecoverable leftovers in pending/recovery storage.

Never use dropped item entities as logical transfer state.

Particles and ItemDisplay are visual echoes only after commit. Visuals must never drive item logic.

## GUI And Interaction Rules

- Wand is read-only diagnostics.
- Wrench handles status, settings, channels, side selection, route mode, filters, trash safety, redstone mode, and upgrades.
- Block-in-hand interactions should pass through only when sneaking, following the local `InteractionPolicy` pattern.
- Filter slots are ghost slots. Copy one item as ghost data without consuming the player item.
- Cancel top GUI mutations and drags touching top GUI slots.
- Cancel shift-clicks that could move real items into ghost/config GUIs.
- Allow bottom inventory clicks only where explicitly safe.

## Crafting And Progression

Recipes are enabled by default.

`recipes.profile` supports:

```text
easy
normal
expensive
```

Unlock recipes on join when recipes are enabled.

`/mech menu` should show recipes to everyone. Admins and creative players can click to receive mechanisms.

Pipe tiers:

```text
pipe         - 8 items per transfer
pipe_fast    - 16 items per transfer
pipe_express - 32 items per transfer
```

Upgrade modules:

```text
Sugar          - speed
Chest          - stack
Paper          - filter slots
Gold ingot     - priority
Ender pearl    - range
Amethyst shard - silent visual echo
```

## Diagnostics

Keep these useful for in-game debugging:

```text
/mech network
/mech doctor
/mech log last [n]
/mech log block [n]
/mech perf
/mech stats
/mech debug on|off
```

## Selftest Coverage

The live selftest should cover:

- basic chest -> extractor -> pipe -> inserter -> chest;
- full destination leaves source unchanged;
- filtered iron/gold split;
- extractor internal hopper inventory drain;
- no source / no destination / filtered errors;
- save/load persistence;
- stale physical block cleanup;
- exact meta filters;
- two extractors same network without loops;
- pending recovery;
- broken pipe route invalidation;
- pipe tiers;
- configured side selection;
- overflow and trash safety;
- round-robin routing;
- redstone gating;
- recipe registration.

## Final Response Expectations

When an agent finishes work on this project, it should report:

- changed behavior;
- build/test command and result;
- built jar path;
- installed jar path if installed;
- local dev-server state if touched;
- manual in-game cases not verified.
