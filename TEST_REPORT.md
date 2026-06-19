# Mechanisms Test Report

Date: 2026-06-19

## Automated Build

Command:

```powershell
.\scripts\gradle-local.ps1 build
```

Result: passed.

Coverage:

```text
FilterServiceTest
- empty filters allow all
- whitelist material filter
- blacklist material filter
- exact/meta similarity path with custom test tags

RouteSearchTest
- router filter blocks non-matching items
- destination priority wins before path length
- maxRouteLength stops route discovery
- reachable inserter diagnostic ignores filters

NetworkIndexerTest
- networks over maxNetworkNodes are marked tooLarge
- unloaded nodes are excluded from graph/neighbors when allowUnloadedChunks=false
- adjacent pipes with different channels do not connect

TransferTransactionTest
- transfer removes from source and adds to destination
- full destination leaves source unchanged
- source-changed guard prevents destination mutation
- addStack merges before using empty slots

InteractionPolicyTest
- wand right-click shows status
- wand sneak-right-click opens supported filter GUI
- block in hand only passes through when sneaking
- empty hand sneak-click opens filter/status quick interaction

FilterGuiPolicyTest
- top filter GUI slots cancel real item movement
- normal bottom inventory clicks are allowed
- bottom inventory shift-clicks are cancelled
- drags touching the top GUI are cancelled

MechanismBlockTypeTest
- pipe, fast pipe, and express pipe tokens/aliases resolve correctly
- only pipe mechanism types report a pipe tier
```

Compile coverage includes `/mech menu`, `/mech doctor`, `/mech log`, `/mech network`, `/mech perf`, settings GUI, filter GUI pages/copy/paste/search, pipe channels, upgrade modules, tier/channel particles, recipe profiles, overflow/trash, redstone mode, and route modes.

## Live Plugin Self-Test

Command run from `D:\Documents\Minecraft\server-dev`:

```text
java -Dmechanisms.selftestOnStartup=true -Dmechanisms.stopAfterSelftest=true -Xms1G -Xmx2G -jar paper-26.2-23.jar nogui
```

Result on local Paper 26.2 dev server: passed.

Evidence:

```text
Mechanisms selftest: PASS
PASS basic chest->extractor->pipe->inserter->chest - source=0, dest=24
PASS destination full keeps item in source - source=8, dest=0
PASS filtered iron/gold split - sourceIron=0, sourceGold=0, ironDest=16, goldDest=16
PASS extractor internal hopper inventory drains - extractor=0, dest=12
PASS no source reports no_source - status=no_source
PASS no destination leaves item in source - source=8, status=no_destination
PASS filtered destination leaves item in source - source=8, dest=0, status=filtered
PASS registry save/load preserves mechanism and filter - present=true, filter=true
PASS stale physical block cleanup removes registry entry - present=false
PASS exact meta filter moves only matching custom item - sourceAccepted=0, sourceRejected=8, destAccepted=8, destRejected=0
PASS two extractors same network drain without loop - sourceIron=0, sourceGold=0, destIron=8, destGold=8
PASS pending recovery restores into extractor inventory - extractor=7, pendingBefore=0, pendingAfter=0
PASS partial pending recovery keeps only leftover - extractor=64, remaining=4, pendingBefore=0, pendingAfterCleanup=0
PASS broken pipe blocks route - source=8, dest=0
PASS pipe tiers change transfer amount - base=8, express=32
PASS configured extractor side chooses one container - destIron=8, destGold=0, southGold=8
PASS overflow receives when primary destination is full - source=0, primary=0, overflow=8
PASS trash deletes only after primary destination is full - source=0, primary=0
PASS disabled trash does not delete items - source=8
PASS round-robin route mode alternates destinations - a=8, b=8
PASS redstone mode can block extractor - source=8, dest=0, status=redstone_blocked
PASS crafting recipes registered - recipes=pipe,pipe_fast,pipe_express,extractor,router,inserter,overflow,trash,wrench
```

The automated run stopped the server cleanly with exit code 0.

## Local Dev Install

Installed jar:

```text
D:\Documents\Minecraft\server-dev\plugins\Mechanisms.jar
```

Dev config updated:

```text
D:\Documents\Minecraft\server-dev\plugins\Mechanisms\config.yml
D:\Documents\Minecraft\server-dev\plugins\Mechanisms\messages.yml
```

## Manual In-World Cases

These require a real player session:

```text
1. /mech menu as survival/non-admin shows recipes only
2. /mech menu as creative/admin can click to receive mechanisms
3. wrench Shift+right-click opens settings GUI
4. pipe channel cycles and prevents adjacent cross-channel connection
5. filter GUI pages, search, clear, copy, paste
6. wrench + module offhand applies speed/stack/filter/priority/range/silent
7. /mech doctor reports networks, pending, recipes, recent errors
8. /mech log last and /mech log block show recent events
9. tier/channel particles show correct colors
10. recipes change after recipes.profile switch + /mech reload
```
