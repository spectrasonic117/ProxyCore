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
│   ├── BroadcastCommand.java # /broadcast, /br
│   ├── AnnounceCommand.java  # /announce true|false|reload
│   ├── MaintenanceCommand.java # /maintenance on|off
│   ├── MotdReloadCommand.java  # /motd reload
│   └── ProxyCoreCommand.java   # /proxycore, /pcore, /proxyreload (global reload)
├── chat/
│   └── StaffChatCommand.java # /staffchat, /sc (with toggle)
├── managers/
│   ├── CommandsManager.java  # Central command registration
│   └── ListenerManager.java  # Central event listener registration
├── config/
│   └── ConfigManager.java    # YAML config load/save with defaults
├── announce/
│   └── AnnouncementManager.java # Scheduled announcement broadcasting
├── util/
│   └── CommandNormalizer.java # Raw command line -> bare command name
└── listener/
    ├── MOTDListener.java     # Server list MOTD handling
    └── CommandBlockerListener.java # Whitelist command blocker
```

## Architecture

**Initialization flow** (Main.java):
1. ConfigManager loads `config.yml` from plugin data directory (copies defaults if missing)
2. AnnouncementManager created
3. CommandsManager registers all commands via Velocity's CommandManager
4. ListenerManager registers all event listeners
5. AnnouncementManager starts scheduled task if enabled

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
- `command-blocker`: Enabled flag, whitelist, deny message, logging, tab-complete, proxy-commands escape hatch

## Gotchas

- **Permission gates are proxy-side**: every admin command checks its permission at the start of `execute()` and rejects with a MiniMessage error. The `hasPermission()` overrides were removed on purpose — when they return `false`, Velocity forwards the command to the player's backend server (confusing "unknown command" instead of a clean denial). Only `/lobby` has no permission check (by design).
- **`CommandExecuteEvent` result API**: in Velocity 4.x the result type is the nested `CommandExecuteEvent.CommandResult` with `allowed()` / `denied()` / `forwardToServer()`, **not** `ResultedEvent.GenericResult`. Use `denied()` so the command is neither executed nor forwarded to the backend.
- **`PlayerAvailableCommandsEvent` is dead**: it still exists in the 4.2.1 API but Velocity never fires it, and the root node it would receive is the live shared Brigadier dispatcher root — mutating it would break command execution for everyone. Do not use it. `TabCompleteEvent` is the only tab hook, and Velocity only fires it for 1.12.2-and-below clients, so tab filtering is best-effort while execution blocking is absolute.
- **Command matching is by base name**: `util/CommandNormalizer` trims, drops the leading slash, cuts arguments, lowercases and strips any `namespace:` prefix. The whitelist is normalized the same way, so `gamemode` and `minecraft:gamemode` are interchangeable entries.
- **Missing top-level config sections are auto-merged**: `ConfigManager.applyDefaults()` runs on every `load()` and `putIfAbsent`s any section the user's file lacks, so features added in a new version appear without wiping existing values. A missing `whitelist` key falls back to `DEFAULT_COMMAND_WHITELIST`; an explicitly empty list is honoured as block-all.
- **Use `configManager.reload()`, not `load()`, for hot reloads**: `load()` lets SnakeYAML `YAMLException` escape on a corrupt file. `reload()` catches it, keeps the previous in-memory config and returns `false` so the caller can warn.
- **Static staff chat state**: `StaffChatCommand.STAFF_CHAT_TOGGLED` is a static synchronized set - survives command re-registration but lost on proxy restart.
- **Config save on toggle**: `/maintenance` and `/announce` call `configManager.setMaintenance()`/`setAnnouncementsEnabled()` which persist to disk immediately. SnakeYAML `save()` drops comments.
- **Resource pack config exists** but is not wired to any listener - `isResourcePackEnabled()`, `getResourcePackUrl()`, `getResourcePackSha1()` are unused.
- **LobbyCommand bypasses permission check** - any player can use /hub. Other commands require explicit permissions.
- **No tests**: Project has no test suite.

## Permissions Reference

| Command | Permission | Default |
|---------|-----------|---------|
| /lobby, /hub, /spawn, /leave | None (all players) | - |
| /find | ProxyCore.find | op |
| /goto | ProxyCore.goto | op |
| /staffchat, /sc | ProxyCore.staffchat | op |
| /broadcast, /br | ProxyCore.broadcast | op |
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
