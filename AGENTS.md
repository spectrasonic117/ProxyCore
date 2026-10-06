# ProxyCore

Velocity proxy plugin for Minecraft servers (Dopamine Network).

## Build Commands

```bash
gradle build          # Build the plugin JAR (Gradle 9.x local, no wrapper included)
gradle clean build    # Clean and rebuild
```

Output JAR: `out/ProxyCore-<version>.jar`

## Tech Stack

- **Platform**: Velocity 4.2.1-SNAPSHOT proxy
- **Java**: 25 (enforced via toolchain)
- **Build**: Gradle 9.5.1
- **Text formatting**: Adventure MiniMessage 4.17.0
- **Config**: SnakeYAML (bundled with Velocity)

## Project Structure

```
src/main/java/com/spectrasonic/ProxyCore/
├── Main.java                 # Plugin entry point (@Plugin annotation, Guice injection)
├── command/                  # SimpleCommand implementations
│   ├── LobbyCommand.java     # /lobby, /hub, /spawn, /leave
│   ├── FindCommand.java      # /find <player>
│   ├── GotoCommand.java      # /goto <player>
│   ├── WhoisCommand.java     # /whois <player>
│   ├── SeenCommand.java      # /seen <player>
│   ├── BroadcastCommand.java # /gbroadcast
│   ├── AnnounceCommand.java  # /announce true|false|reload
│   ├── MaintenanceCommand.java # /maintenance on|off
│   ├── MotdReloadCommand.java  # /motd reload
│   └── ProxyCoreCommand.java   # /proxycore, /pcore, /proxyreload (global reload)
├── chat/
│   └── StaffChatCommand.java # /staffchat, /sc (with toggle)
├── managers/
│   ├── CommandsManager.java  # Central command registration
│   ├── ListenerManager.java  # Central event listener registration
│   └── SeenManager.java      # Player connection history (seen-data.json via Gson)
├── config/
│   └── ConfigManager.java    # YAML config load/save with defaults
├── announce/
│   └── AnnouncementManager.java # Scheduled announcement broadcasting
├── util/
│   └── CommandNormalizer.java # Raw command line -> bare command name
└── listener/
    ├── MOTDListener.java     # Server list MOTD handling
    ├── CommandBlockerListener.java # Whitelist command blocker
    └── SeenListener.java     # PostLogin/Disconnect -> SeenManager
```

## Architecture

**Initialization flow** (Main.java):
1. ConfigManager loads `config.yml` from plugin data directory (copies defaults if missing)
2. AnnouncementManager created
3. SeenManager created (loads `seen-data.json` connection history)
4. CommandsManager registers all commands via Velocity's CommandManager
5. ListenerManager registers all event listeners
6. AnnouncementManager starts scheduled task if enabled
7. On shutdown: announcement task stopped, SeenManager saved to disk

**Command registration** (CommandsManager.java):
- All commands registered in `registerAll()` method
- Lobby command has 4 aliases: `lobby`, `hub`, `spawn`, `leave`
- ProxyCore root command has 3 aliases: `proxycore`, `pcore`, `proxyreload`
- Commands accept `ProxyServer` and `ConfigManager` via constructor injection

**Plugin messaging**:
- Channels use `dopamine:<channel>` format (e.g., `dopamine:announcement`, `dopamine:staffchat`, `dopamine:broadcast`)
- Messages serialized with `DataOutputStream` (writeUTF)
- Forwarded to all backend servers for BungeeCord/Spigot plugin handling

## Code Conventions

- **Text formatting**: Always use `MiniMessage.miniMessage().deserialize()` for player-facing messages
- **Commands**: Implement `SimpleCommand` interface, override `execute(Invocation)`
- **Permissions**: Follow `ProxyCore.<feature>` pattern (e.g., `ProxyCore.find`, `ProxyCore.staffchat`)
- **Config access**: Use `ConfigManager` getters, never access YAML directly
- **Player checks**: Use `instanceof Player player` pattern guard, send error to console if not player

## Config System

`ConfigManager` handles:
- Auto-copies bundled `config.yml` on first run
- Falls back to programmatic defaults if file missing or corrupt
- `save()` writes back to disk (used by maintenance toggle)
- Nested sections accessed via `getSection()` helpers

Key config sections:
- `target-server`: Lobby server name for /hub
- `motd`: Maintenance mode, MOTD lines (MiniMessage)
- `announcements`: Enabled flag, interval, message list
- `chat`: Broadcast and staff chat formats with `{message}`, `{player}`, `{server}` placeholders
- `resource-pack`: URL and SHA1 (not yet wired to listener)
- `command-blocker`: Enabled flag, whitelist, deny message, logging, tab-complete, hide-from-client, proxy-commands escape hatch
- `seen`: Enabled flag and show-ip flag for the /seen tracking system (data lives in `seen-data.json`, not in config.yml)

## Gotchas

- **Permission gates are proxy-side**: every admin command checks its permission at the start of `execute()` and rejects with a MiniMessage error. The `hasPermission()` overrides were removed on purpose — when they return `false`, Velocity forwards the command to the player's backend server (confusing "unknown command" instead of a clean denial). Only `/lobby` has no permission check (by design).
- **`CommandExecuteEvent` result API**: in Velocity 4.x the result type is the nested `CommandExecuteEvent.CommandResult` with `allowed()` / `denied()` / `forwardToServer()`, **not** `ResultedEvent.GenericResult`. Use `denied()` so the command is neither executed nor forwarded to the backend.
- **`PlayerAvailableCommandsEvent` IS fired in Velocity 4** (an earlier note claiming otherwise was wrong). It is fired from `BackendPlaySessionHandler.handle(AvailableCommandsPacket)` once per backend command-tree packet, *after* `CommandGraphInjector.inject()` has added the proxy commands. The `rootNode` it exposes is the freshly deserialized node of that packet — a per-player copy — **not** the shared proxy dispatcher root: the injector rebuilds every node via `node.createBuilder()`, so pruning `getRootNode().getChildren()` is safe and does not affect command execution. `@Subscribe(order = ...)` is deprecated in 4.x; use `@Subscribe(priority = Short.MIN_VALUE)` to run last. Hiding a command here also removes it from 1.13+ client-side autocomplete. `TabCompleteEvent` is still the only tab hook for pre-1.13 clients, so keep both.
- **Command matching is by base name**: `util/CommandNormalizer` trims, drops the leading slash, cuts arguments, lowercases and strips any `namespace:` prefix. The whitelist is normalized the same way, so `gamemode` and `minecraft:gamemode` are interchangeable entries.
- **Missing top-level config sections are auto-merged**: `ConfigManager.applyDefaults()` runs on every `load()` and `putIfAbsent`s any section the user's file lacks, so features added in a new version appear without wiping existing values. A missing `whitelist` key falls back to `DEFAULT_COMMAND_WHITELIST`; an explicitly empty list is honoured as block-all.
- **Use `configManager.reload()`, not `load()`, for hot reloads**: `load()` lets SnakeYAML `YAMLException` escape on a corrupt file. `reload()` catches it, keeps the previous in-memory config and returns `false` so the caller can warn.
- **Static staff chat state**: `StaffChatCommand.STAFF_CHAT_TOGGLED` is a static synchronized set - survives command re-registration but lost on proxy restart.
- **Config save on toggle**: `/maintenance` and `/announce` call `configManager.setMaintenance()`/`setAnnouncementsEnabled()` which persist to disk immediately. SnakeYAML `save()` drops comments.
- **Resource pack config exists** but is not wired to any listener - `isResourcePackEnabled()`, `getResourcePackUrl()`, `getResourcePackSha1()` are unused.
- **LobbyCommand bypasses permission check** - any player can use /hub. Other commands require explicit permissions.
- **Seen data is JSON, not YAML**: `SeenManager` persists per-player connection history (`uuid, name, firstJoin, lastJoin, lastQuit, lastIp, lastServer`) in `seen-data.json` via Gson (transitive from velocity-api). Saves happen on every disconnect plus once on proxy shutdown; Velocity fires connection events async, so the blocking file write there is safe. A corrupt file logs a warning and starts with empty history rather than refusing to boot. When adding fields to `SeenRecord`, keep them Gson-friendly (plain fields, no final).
- **Tab-complete via `suggest()`**: `/whois` and `/seen` are the only commands overriding `SimpleCommand.suggest(Invocation)` — they suggest online player names (plus seen-history names for `/seen`), prefix-filtered case-insensitively. Follow the same pattern for future commands.
- **No tests**: Project has no test suite.

## Permissions Reference

| Command | Permission | Default |
|---------|-----------|---------|
| /lobby, /hub, /spawn, /leave | None (all players) | - |
| /find | ProxyCore.find | op |
| /goto | ProxyCore.goto | op |
| /whois | ProxyCore.whois | op |
| /seen | ProxyCore.seen | op |
| /staffchat, /sc | ProxyCore.staffchat | op |
| /gbroadcast | ProxyCore.broadcast | op |
| /maintenance | ProxyCore.maintenance | op |
| /motd reload | ProxyCore.motd | op |
| /announce | ProxyCore.announce | op |
| /proxycore, /pcore, /proxyreload | ProxyCore.reload | op |
| *(not a command)* bypass the command whitelist | ProxyCore.commandblocker.bypass | op |

## Plugin Channels

| Channel | Purpose | Payload |
|---------|---------|---------|
| dopamine:announcement | Broadcast announcements to backend | UTF string (raw message) |
| dopamine:staffchat | Forward staff messages to backend | UTF player, UTF server, UTF message |
| dopamine:broadcast | Forward broadcasts to backend | UTF string (raw message) |
