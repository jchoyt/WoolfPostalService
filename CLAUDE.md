# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

WoolfPostalService (WPS) is a Spigot/Bukkit Minecraft server plugin written in Java 17. It lets
players designate shulker boxes as "mailboxes" — when a player places or a dispenser ejects a
shulker box into a monitored location, the plugin posts a notification to a Discord channel via
the JDA (Java Discord API) library, so WPS "staff" know a package is ready for pickup.

## Build

Build is Gradle-based (via the wrapper — no local Gradle install required):

```
./gradlew build         # compiles, runs checks, and produces build/libs/WoolfPostalService-<version>.jar
./gradlew shadowJar     # just the fat jar (plugin classes + JDA bundled, spigot-api excluded)
./gradlew clean
```

Standard Gradle source layout: Java sources under `src/main/java`, and `plugin.yml`/`config.yml`
under `src/main/resources` (both get copied verbatim into the jar root, as Spigot requires).

Dependencies are declared in `build.gradle`, not vendored in `lib/` anymore:

- `spigot-api` is `compileOnly` (provided by the server at runtime, not bundled), resolved from
  Spigot's own Maven snapshot repo (`hub.spigotmc.org`) using coordinates `org.spigotmc:spigot-api:<version>-R0.1-SNAPSHOT`.
- `JDA` is `implementation`, resolved from Maven Central and bundled into the output jar by the
  Shadow plugin (`com.github.johnrengelman.shadow`) — this replaces the old `build.xml`
  `<zipgroupfileset>` step. The `opus-java` (voice/audio) transitive dependency is excluded since
  WPS only ever sends text notifications.

The project `version` in `build.gradle` must stay in sync with `api-version` in `plugin.yml` and
with the Spigot dependency coordinate above — Bukkit enforces the `api-version` match at load time.

Gradle itself needs to run on a JDK the installed Gradle version supports (8.10 does not run on
JDK 25 class-file format); `sourceCompatibility`/`targetCompatibility` in `build.gradle` still
target Java 17 bytecode regardless of which JDK runs the build, so a locally-installed JDK 17
toolchain is not required.

## Tests

```
./gradlew test                                        # whole suite
./gradlew test --tests "*.PlacementListenerTest"       # one class
```

Tests use [MockBukkit](https://github.com/MockBukkit/MockBukkit) (`org.mockbukkit.mockbukkit:mockbukkit-v1.21`)
against `paper-api` (Paper is a superset of Spigot's API, so it mocks Bukkit types this plugin
uses fine) plus JUnit 5, all `testImplementation`-only — none of it ships in the plugin jar.
`compileTestJava` is pinned to `--release 21` separately from the main source set's Java 17
target, because the current `paper-api`/MockBukkit snapshot's classes require JVM 21+; bump that
alongside `paperApiVersion`/`mockbukkitVersion` in `build.gradle` if a future snapshot moves the
floor again.

WoolfPostalService's own `onEnable()` makes a real, blocking Discord connection via JDA
(`connectDiscord()`), which tests can't do — there's no bot token in a test environment. All
tests instead load `TestableWoolfPostalService` (in `src/test/java`, same package so it can
override package-visible internals), a subclass that no-ops `connectDiscord()` and records
`notifyMailChannel()` calls into a list instead of logging/sending them, both cheap overrides
since the two methods are non-`private` seams left for exactly this. This is also why
`WoolfPostalService` isn't `final` and `onDisable()` null-checks the JDA `bot` field before
calling `shutdown()` on it.

One MockBukkit gotcha worth knowing: `MockBukkit.load(SomeClass.class)` matches `SomeClass`
against `plugin.yml`'s `main:` field and silently falls back to a bare, commandless/permissionless
description if it doesn't match — since `TestableWoolfPostalService` isn't the class named there,
tests use `MockBukkit.loadWith(TestableWoolfPostalService.class, "plugin.yml")` instead, which
takes the description file as given. Likewise, `PluginCommand.execute(...)` (used to drive
`/wpsbox` and `/deletebox` directly in tests) enforces the command's declared `permission:` from
`plugin.yml` itself, same as real dispatch would — tests grant `wps.mailman` to whichever
`PlayerMock` should be allowed to run the command via `player.addAttachment(plugin, "wps.mailman", true)`.

## Architecture

Single package: `com.asharpminer.wps`. Four classes, wired together from the plugin entrypoint:

- **`WoolfPostalService`** (`JavaPlugin` subclass, the plugin main class per `plugin.yml`) —
  owns all shared state and is passed into the other three classes by constructor injection.
  Responsibilities:
  - Tracks monitored mailbox locations in `Map<Block, String>` (block → nickname), persisted to
    `mailboxes.yml` in the plugin's data folder (not `config.yml`) as `world:x:y:z:nickname`
    strings — see `readMailboxes()`/`saveMailboxes()`. Mailboxes are saved on every mutation and
    again in `onDisable()`.
  - Owns the JDA Discord bot connection, created in `connectDiscord()` (called once from
    `onEnable()`) from `config.yml`'s `discord.token`. It looks up the first guild the bot
    belongs to, then finds the channel named `discord.server.channel` within it, and stores that
    as `wpsChannel`. If no guild or channel is found it logs a warning and returns early (note:
    the bot connection stays open in that case; `disable()` calls are currently commented out).
    `connectDiscord()` is a seam for tests (see Tests below) — it's the only reason the class
    isn't `final` and the only non-`private` method that isn't part of the plugin's real API.
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
