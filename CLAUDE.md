# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

WoolfPostalService (WPS) is a Spigot/Bukkit Minecraft server plugin written in Java 17. It lets
players designate shulker boxes as "mailboxes" — when a player places or a dispenser ejects a
shulker box into a monitored location, the plugin posts a notification to a Discord channel via
the JDA (Java Discord API) library, so WPS "staff" know a package is ready for pickup.

## Build

Build is Ant-based, not Maven/Gradle:

```
ant jar          # compiles src/ into build/ and produces WoolfPostalService-<version>.jar
ant clean        # removes build/, doc/, and the jar
```

There is no automated test suite in this repo.

### Before building, dependencies must be present in `lib/`

The build does not fetch dependencies. `lib/README.md` documents what's needed:

- `spigot-api-<version>.jar` — obtain by building CraftBukkit/Spigot with BuildTools
  (https://www.spigotmc.org/wiki/buildtools/): `java -jar BuildTools.jar --rev <minecraft version>`.
- `JDA-5.1.0-withDependencies-min.jar` — the Discord bot library; `ant jar` zip-groups this whole
  jar into the plugin's output jar (see `build.xml`'s `zipgroupfileset`), so the shipped plugin jar
  is self-contained for Discord connectivity.

The plugin `version` (currently 1.21) is hardcoded in both `build.xml` and `plugin.yml` — keep
them in sync when bumping versions, and match it to the target `api-version` (Minecraft/Spigot
version) since Bukkit enforces that at load time.

## Architecture

Single package: `com.asharpminer.wps`. Four classes, wired together from the plugin entrypoint:

- **`WoolfPostalService`** (`JavaPlugin` subclass, the plugin main class per `plugin.yml`) —
  owns all shared state and is passed into the other three classes by constructor injection.
  Responsibilities:
  - Tracks monitored mailbox locations in `Map<Block, String>` (block → nickname), persisted to
    `mailboxes.yml` in the plugin's data folder (not `config.yml`) as `world:x:y:z:nickname`
    strings — see `readMailboxes()`/`saveMailboxes()`. Mailboxes are saved on every mutation and
    again in `onDisable()`.
  - Owns the JDA Discord bot connection, created in `onEnable()` from `config.yml`'s
    `discord.token`. It looks up the first guild the bot belongs to, then finds the channel
    named `discord.server.channel` within it, and stores that as `wpsChannel`. If no guild or
    channel is found it logs a warning and returns early (note: the bot connection stays open in
    that case; `disable()` calls are currently commented out).
  - `notifyMailChannel(msg)` is the single choke point for outbound Discord messages. When
    `config.yml`'s `testing: true`, messages are logged instead of sent — check/toggle this when
    working on Discord-facing behavior locally.
  - Also exposes small Bukkit helpers used by listeners/commands: `getPlayerByName`,
    `runCommand` (dispatches a console command after a scheduler delay), `asyncBroadcast`.

- **`PlacementListener`** — Bukkit `Listener` registered in `onEnable()`. Two event handlers:
  - `onBlockPlace`: fires when any block is placed; filters to shulker boxes placed at a known
    mailbox location. Players with `wps.mailman` placing there get a joke message instead of
    triggering a notification (mail staff placing their own boxes isn't a customer event).
    Otherwise builds a message (using the mailbox nickname if set) and calls
    `notifyMailChannel`; empty shulkers get an "It's empty, no rush" suffix.
  - `onBlockDispense`: fires when a dispenser ejects an item; filters to shulker boxes ejected
    directly into a monitored location (computed via the dispenser's facing `BlockFace`), and
    notifies Discord that "the magic of WPS" moved a package there.

- **`MailboxCommandExecutor`** — implements `/wpsbox <nickname>` (permission `wps.mailman`).
  Registers itself against the command Bukkit already knows about from `plugin.yml`
  (`plugin.getCommand("wpsbox")`), not by registering a new command. Adds the block the
  commanding player is standing on to the monitored set.

- **`DeleteMailboxCommandExecutor`** — implements `/deletebox` (permission `wps.mailman`) the
  same way; removes the block the player is standing on from the monitored set.

### Adding a new command or listener

Follow the existing pattern: a new class that takes `WoolfPostalService plugin` in its
constructor, registers itself (`getCommand(...).setExecutor(this)` for commands, or
`getPluginManager().registerEvents(this, plugin)` for listeners), and is instantiated once from
`WoolfPostalService.onEnable()`. Any new command also needs an entry under `commands:` in
`plugin.yml` (and a `permissions:` entry if it's gated), matching the pattern of `wpsbox` and
`deletebox`.

## Configuration

- `config.yml` (shipped as a template, real one lives in the server's plugin data folder):
  `discord.token`, `discord.server.name` (currently unused for guild lookup — the code just
  takes the bot's first guild), `discord.server.channel`, and `testing` (routes Discord messages
  to the log instead of the server when `true`).
- `mailboxes.yml`: runtime-generated, not shipped — the persisted list of monitored locations.
