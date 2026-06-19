# Mechanisms

Server-side Paper/Purpur 26.2 plugin for mechanism blocks. First module: `logistics` with extractor, three pipe tiers, router, inserter, overflow, and trash blocks for transactional item movement between containers.

Production note: the current production server is vanilla 26.2, so this plugin is not deployable there until the server is migrated to Paper/Purpur. Work only against the local dev server:

```text
D:\Documents\Minecraft\server-dev
```

## Build

Requires Java 25. The local wrapper script pins the known working runtime:

```powershell
.\scripts\gradle-local.ps1 build
```

Built jar:

```text
D:\Documents\Minecraft\Mechanisms\build\libs\Mechanisms-0.1.0.jar
```

Local dev install:

```powershell
.\scripts\install-server-dev.ps1
```

This copies the jar to:

```text
D:\Documents\Minecraft\server-dev\plugins\Mechanisms.jar
```

## Commands

```text
/mech menu
/mech give
/mech recipes
/mech unlockrecipes [player]
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

`/mech give` is a compatibility alias for `/mech menu`. Receiving items is limited to creative players or players with `mechanisms.give`/`mechanisms.admin`. `/mech unlockrecipes` opens all registered Mechanisms recipes in the recipe book for yourself; targeting another online player requires `mechanisms.admin`.

## Permissions

```text
mechanisms.use
mechanisms.admin
mechanisms.give
mechanisms.reload
mechanisms.debug
```

`mechanisms.admin` includes give, reload, and debug.

## Config

Main config: `plugins/Mechanisms/config.yml`

```yaml
logistics:
  enabled: true
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

Text messages live in `plugins/Mechanisms/messages.yml` and are UTF-8 Russian by default.

## Pipe Tiers

```text
pipe         - Труба I,  8 items per transfer
pipe_fast    - Труба II, 16 items per transfer
pipe_express - Труба III, 32 items per transfer
```

Route speed is the slowest pipe tier in that route. Mixed `pipe` + `pipe_express` routes still move 8 items per transfer; fully express routes move 32.

Pipe upgrades with wrench:

```text
wrench main hand + redstone block offhand: Pipe I -> Pipe II
wrench main hand + diamond offhand: Pipe II -> Pipe III
wrench main hand + shears offhand: Pipe III -> Pipe II or Pipe II -> Pipe I
```

## Pipe Channels

Pipes have independent channels:

```text
default
red
blue
green
yellow
```

Change a pipe channel in the wrench settings GUI. Adjacent pipes with different channels do not connect into the same network, which makes compact side-by-side routing possible.

## Upgrade Modules

Hold the wrench in the main hand and the module item in the offhand, then right-click the mechanism:

```text
Sugar          - Speed, faster transfer amount on pipes/extractors
Chest          - Stack, larger transfer amount on pipes/extractors
Paper          - Filter slots, extra filter pages on router/destination blocks
Gold ingot     - Priority, extra destination priority
Ender pearl    - Range, longer route search from extractors
Amethyst shard - Silent, disables visual transfer echo from that source
```

Each module has levels `0..3`. Creative players do not consume module items; survival players do.

## Crafting

Recipes are enabled by default. `recipes.profile` controls survival balance:

```text
easy      - cheaper recipes and more output
normal    - default balance
expensive - lower output and block-level ingredients
```

`/mech menu` shows all mechanisms and recipes. Admins and creative players can click to receive items; other players can only view recipes. `/mech recipes` prints recipes in chat. On player join, the plugin unlocks Mechanisms recipes in the vanilla recipe book when `recipes.enabled` is true.

## Logistics Model

Mechanism blocks are crafted or issued through `/mech menu` and marked with persistent item data. When placed, they are stored in `plugins/Mechanisms/data.yml`; when broken, they are removed.

Networks are built only from registered mechanism blocks. The plugin does not scan the world every tick. It rebuilds the graph on place, break, and reload. Routes are found with BFS, cached, and invalidated when registry data changes.

Transfers are synchronous and transactional on the server thread:

1. Select a source stack and amount.
2. Find a route to a filtered destination.
3. Verify destination capacity.
4. Remove from source.
5. Add to destination.
6. Return unexpected leftovers to source.
7. Persist unrecoverable leftovers in `plugins/Mechanisms/pending.yml`.

No dropped item entity is used as transfer state. ItemDisplay and particles are visual echoes after commit.

## Filters

Sneak-right-click a router, inserter, overflow, or trash block with the wrench to open settings, then open the filter GUI. Filter slots are ghost slots: clicking with a real item copies a single-item ghost and does not consume the player item.

The filter GUI supports:

```text
pages
search by cursor item
clear
copy
paste
whitelist/blacklist
material-only/exact-meta
priority -1000..1000
route mode
```

Empty filters allow all items. Filter-slot modules add more pages.

## Routing

Route modes:

```text
priority_first - highest priority, then shortest path
nearest        - shortest path first
round_robin    - rotate between matching destinations
split_evenly   - MVP alias of round_robin for even distribution over cycles
```

Overflow and trash are explicit destination blocks. The engine attempts normal inserters first, overflow blocks second, and trash blocks last. Trash is disabled by default and must be armed in settings.

Extractor redstone modes:

```text
ignore
requires_power
requires_no_power
```

Extractor, inserter, and overflow can be set to `auto`, `north`, `south`, `east`, `west`, `up`, or `down`.

## Diagnostics

```text
/mech network       - GUI inspector for the mechanism block you are looking at
/mech doctor        - network limits, unloaded records, pending items, recipes, recent errors
/mech log last [n]  - recent transfer/error events
/mech log block [n] - recent transfer/error events for the targeted block
/mech perf          - server-side performance counters
```

Use `/mech wand` for read-only diagnostics. Use `/mech wrench` for status, settings, channels, side selection, route mode, filters, trash safety, redstone mode, and upgrades.

## Visuals

Visuals never drive item logic. They only echo committed transfers.

```text
Труба I   - copper/orange particles
Труба II  - green particles
Труба III - purple particles
channels  - red/blue/green/yellow tint
```

`visuals.itemDisplayScale` controls the size of the ItemDisplay echo. Default is `0.35`.

## Live Self-Test

Run on the local dev Paper server:

```text
/mech selftest
```

Non-interactive dev automation:

```powershell
java -Dmechanisms.selftestOnStartup=true -Dmechanisms.stopAfterSelftest=true -Xms1G -Xmx2G -jar paper-26.2-23.jar nogui
```

The selftest covers basic transfer, full destinations, filters, exact meta, broken routes, reload persistence, pending recovery, pipe tiers, side selection, overflow, trash safety, round-robin routing, redstone gating, and recipe registration.
