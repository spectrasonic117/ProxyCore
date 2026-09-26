# 📦 ProxyCore — Memory Bank

> **Última actualización:** 2026-09-26
> **Proyecto:** ProxyCore v1.0.2
> **Plataforma:** Velocity Proxy 4.2.1-SNAPSHOT (MC 1.21.x+)
> **API:** velocity-api (PaperMC)
> **Java:** 25 (toolchain en build.gradle)
> **Build Tool:** Gradle (sistema: 9.5.1 vía SDKMAN, no hay wrapper `gradlew`)

## 🏗️ Arquitectura General

- **Tipo de plugin:** Velocity proxy plugin (Dopamine Network)
- **Clase principal:** `com.spectrasonic.ProxyCore.Main` (`@Plugin(id = "proxycore")`, Guice `@Inject`)
- **Entry point:** `ProxyInitializeEvent` → ConfigManager.load → AnnouncementManager → CommandsManager.registerAll → ListenerManager.registerAll → AnnouncementManager.start
- **Persistencia:** YAML (SnakeYAML, bundled con Velocity) en `plugins/ProxyCore/config.yml`
- **Mensajería plugin channels:** `dopamine:<channel>` con `DataOutputStream`/`writeUTF` hacia servidores backend

## 📁 Estructura del Proyecto

```
src/main/java/com/spectrasonic/ProxyCore/
├── Main.java                     # Entry point, logo ANSI, lifecycle
├── command/
│   ├── LobbyCommand.java         # /lobby /hub /spawn /leave
│   ├── FindCommand.java          # /find <player>
│   ├── GotoCommand.java          # /goto <player>
│   ├── BroadcastCommand.java     # /broadcast /br
│   ├── AnnounceCommand.java      # /announce true|false|reload
│   ├── MaintenanceCommand.java   # /maintenance on|off
│   ├── MotdReloadCommand.java    # /motd reload
│   └── ProxyCoreCommand.java     # /proxycore /pcore /proxyreload
├── chat/
│   └── StaffChatCommand.java     # /staffchat /sc (toggle estático)
├── managers/
│   ├── CommandsManager.java      # Registro central de comandos
│   └── ListenerManager.java      # Registro de listeners
├── config/
│   └── ConfigManager.java        # YAML load/save + defaults programáticos
├── announce/
│   └── AnnouncementManager.java  # Anuncios programados
├── util/
│   └── CommandNormalizer.java    # Línea de comando cruda → nombre base
└── listener/
    ├── MOTDListener.java         # ProxyPingEvent → MOTD/ mantenimiento
    └── CommandBlockerListener.java # Whitelist de comandos (CommandExecuteEvent)
src/main/resources/
├── config.yml                     # Copiado a dataDirectory en primer arranque
└── version.properties
```

## 📚 Dependencias y Frameworks

| Dependencia                | Versión        | Scope     | Propósito                     |
| -------------------------- | -------------- | --------- | ----------------------------- |
| com.velocitypowered:velocity-api | 4.2.1-SNAPSHOT | compileOnly | API del proxy               |
| adventure-text-minimessage | 4.17.0         | compileOnly | Formato de texto             |

- **Repositorio:** papermc-repo (repo.papermc.io) + mavenCentral
- **Salida JAR:** `out/ProxyCore-<version>.jar` (`jar` task con `destinationDirectory` custom)
- **Sin Lombok, sin tests.**

### Notas de compatibilidad
- ⚠️ Velocity 4.2.1-SNAPSHOT trae Adventure moderno transitivo: **`Component.join(Component, ...)` ya no existe** — usar `Component.join(JoinConfiguration.newlines(), ...)` (JoinConfiguration).
- El AGENTS.md dice Velocity 3.4.0/Java 21, pero lo real es **4.2.1-SNAPSHOT / Java 25**.
- Velocity 4.x: ningún método de `ServerPing`/`ServerPing.Builder` está deprecado (verificado en javadoc 4.1.0); `@SuppressWarnings("deprecation")` en MOTD era residual y se eliminó.

## 🎯 APIs en Uso

### Velocity Proxy API (4.2.1-SNAPSHOT)
- **MOTD:** `ProxyPingEvent` → `ping.asBuilder()` → `description/onlinePlayers/maximumPlayers/samplePlayers/clearSamplePlayers` → `event.setPing(builder.build())`
- **Comandos:** `SimpleCommand` (execute + hasPermission manual), registro vía `CommandManager.metaBuilder(...).plugin(plugin).build()`
- **Bloqueo de comandos:** `CommandExecuteEvent` → `event.setResult(CommandExecuteEvent.CommandResult.denied())`; `CommandManager.hasCommand(alias, source)` para detectar comandos del proxy
- **Eventos:** `ProxyInitializeEvent`, `ProxyShutdownEvent`, `ProxyPingEvent`, `CommandExecuteEvent`, `TabCompleteEvent` (`@Subscribe`)

### Adventure MiniMessage
- **Patrón:** `MiniMessage.miniMessage().deserialize("<green>...</green>")`
- **Tags usados:** `<color>`, `<gradient>`, `<bold>`, `<reset>`, `<white>`
- **MOTD multi-línea:** `Component.join(JoinConfiguration.newlines(), componentes)`

## 📝 Decisiones de Diseño

### 2026-09-26 — Bloqueador de comandos con whitelist por nombre base
- **Contexto:** Los jugadores podían ejecutar comandos de backend (`/gamemode`, `/miplugin:cmd`) que el proxy no filtra.
- **Decisión:** Whitelist **estricta** en `CommandExecuteEvent`; coincidencia por **nombre base** normalizado (trim, sin `/`, sin argumentos, minúsculas, sin `namespace:`), de modo que `gamemode` y `minecraft:gamemode` son la misma entrada. La whitelist se normaliza igual, luego el match es bidireccional.
- **Exenciones:** consola (nunca bloqueada), permiso `ProxyCore.commandblocker.bypass`, y con `always-allow-proxy-commands: true` cualquier comando registrado en el proxy (`CommandManager.hasCommand`) — red de seguridad anti-lockout para admins.
- **Tab-complete:** `block-tab-complete` es best-effort. `PlayerAvailableCommandsEvent` **existe en la API 4.2.1 pero Velocity nunca lo dispara** (verificado leyendo `ClientPlaySessionHandler`, `ConnectedPlayer`, `ClientConfigSessionHandler` y los 4 `CommandHandler` de `dev/4.0.0`), y el root node que recibiría es el root compartido del dispatcher Brigadier — mutarlo rompería la ejecución de comandos para todos. Solo se usa `TabCompleteEvent`, que Velocity dispara únicamente para clientes ≤1.12.2.
- **Config:** nueva sección `command-blocker`; `ConfigManager.applyDefaults()` hace `putIfAbsent` de secciones de primer nivel ausentes para que una config 1.0.1 migre sin perder valores. `whitelist` ausente → `DEFAULT_COMMAND_WHITELIST`; `whitelist: []` explícito → se respeta (bloquea todo).
- **Reload:** `/proxycore reload` (alias `pcore`, `proxyreload`, permiso `ProxyCore.reload`) usa `ConfigManager.reload()` — que captura `YAMLException` de SnakeYAML, conserva la config en memoria y devuelve `false` — y luego `AnnouncementManager.restart()`. `/motd reload` se mantiene intacto.

### 2026-09-25 — `motd.enabled` como switch maestro del MOTD
- **Contexto:** Se agregó `motd.enabled: true` al config.yml pero nada lo leía (flag inerte).
- **Decisión:** `ConfigManager.isMotdEnabled()` (default `true` para compat con configs viejas) + early-return en `MOTDListener.onProxyPing`. Si es `false`, el plugin no toca el ping (MOTD vanilla de Velocity) — incluye el mensaje de mantenimiento.
- **Impacto:** `/motd reload` ya recoge el flag (listener lee config en cada ping). `createDefaults()` ahora incluye `enabled: true`.

### 2026-09-25 — Sample players con UUID real
- **Decisión:** `ServerPing.SamplePlayer(player.getUsername(), player.getUniqueId())` en vez de `UUID.randomUUID()` (semánticamente correcto para el sample de la server list).

## ✅ Progreso del Proyecto

### Completado
- [x] MOTD custom con MiniMessage + modo mantenimiento — 2026-09-25
- [x] Flag `motd.enabled` cableado (ConfigManager + MOTDListener) — 2026-09-25
- [x] Migración `Component.join` → `JoinConfiguration.newlines()` (compilable contra Velocity 4.2.1) — 2026-09-25
- [x] Build verificado: `gradle clean build` OK → `out/ProxyCore-1.0.1.jar` — 2026-09-25
- [x] Logo con colores reales en consola: `ANSIComponentSerializer.ansi()` reemplazó al serializer ANSI manual con mapeo RGB erróneo — 2026-09-25
- [x] Bloqueador de comandos por whitelist (`CommandBlockerListener` + `CommandNormalizer` + sección `command-blocker`) — 2026-09-26
- [x] Comando `/proxycore reload` (alias `pcore`, `proxyreload`) con recarga global y `ConfigManager.reload()` tolerante a YAML corrupto — 2026-09-26
- [x] Migración automática de config: `applyDefaults()` + fallback de whitelist — 2026-09-26
- [x] Versión 1.0.2 — 2026-09-26

### Pendiente
- [ ] `resource-pack` en config existe pero no está cableado a ningún listener (sin uso)
- [ ] `/broadcast` no tiene check de permiso
- [ ] Staff chat: estado estático `STAFF_CHAT_TOGGLED` se pierde al reiniciar el proxy
- [ ] SnakeYAML `save()` descarta comentarios del config.yml (afecta `/maintenance`, `/announce`)
- [ ] AGENTS.md desactualizado: dice Velocity 3.4.0/Java 21/Gradle 8.8; real es 4.2.1-SNAPSHOT/Java 25
- [ ] `block-tab-complete` es inefectivo en clientes 1.13+ (Velocity no expone evento; ver decisiones de diseño)

## 🐛 Problemas Conocidos y Soluciones

### `Component.join(Component.newline(), ...)` no compila contra Velocity 4.2.1 — RESUELTO
- **Síntoma:** `no suitable method found for join(TextComponent, List<Component>)`
- **Causa:** Adventure moderno (transitivo de velocity-api 4.2.1-SNAPSHOT) eliminó el overload `join(ComponentLike, ...)`.
- **Solución:** `Component.join(JoinConfiguration.newlines(), components)` con import `net.kyori.adventure.text.JoinConfiguration`.
- **Archivos afectados:** `listener/MOTDListener.java`

### Colores del logo ANSI no se mostraban en la consola — RESUELTO
- **Síntoma:** El logo de inicio de ProxyCore salía todo en blanco plano, sin colores, aunque emitía códigos ANSI.
- **Causa (bug principal):** El serializer ANSI hecho a mano (`getAnsiColorCode`) comparaba contra RGB "puros" del ANSI clásico (`0,255,255` aqua, `255,255,0` amarillo, etc.), pero Adventure usa la paleta Minecraft: aqua `#55FFFF`=(85,255,255), yellow `#FFFF55`, red `#FF5555`, green `#55FF55`. Ningún branch matcheaba → todos los colores caían al default `"37"` (blanco) → logo monocromo.
- **Causa (secundaria, por diseño de Velocity):** el appender de archivo en `log4j2.xml` de Velocity usa `%stripAnsi{%msg}` → `logs/latest.log` NUNCA tendrá colores. Los colores solo son visibles en la consola live. Velocity detecta ANSI con `TerminalConsoleAppender.isAnsiSupported()` (JLine; `-Dterminal.ansi` lo sobreescribe).
- **Solución:** Eliminado el serializer manual (~60 líneas); reemplazado por `ANSIComponentSerializer.ansi()` (adventure-text-serializer-ansi 5.2.0, transitivo de velocity-api, sin cambios en build.gradle). Auto-detecta `ColorLevel.compute()` (respeta `-Dnet.kyori.ansi.colorLevel` y `-Dterminal.ansi=false`); con nivel NONE emite texto plano limpio (sin basura en paneles/headless). En paneles tipo Pterodactyl que renderizan ANSI, forzar colores con `-Dnet.kyori.ansi.colorLevel=truecolor` en los flags JVM del proxy.
- **Verificación:** Test local con los jars reales del classpath: OLD emitía `ESC[37m` para todo; NEW emite `ESC[96m` (aqua), `ESC[97m` (white), `ESC[93m` (yellow), `ESC[91m` (red), `ESC[92m` (green) en modo 16 colores (truecolor → `38;2;R;G;B`).
- **Archivos afectados:** `Main.java`

## 📜 Bitácora de Cambios

### 2026-09-26 — Bloqueador de comandos por whitelist + `/proxycore reload` (v1.0.2)
- **Archivos nuevos:** `util/CommandNormalizer.java`, `listener/CommandBlockerListener.java`, `command/ProxyCoreCommand.java`
- **Archivos modificados:** `config/ConfigManager.java` (sección `command-blocker`, `applyDefaults()`, `reload()`, helpers `getBoolean`/`getString`/`normalizeAll`/`castToMap`), `managers/ListenerManager.java` (recibe `Logger`, registra el blocker), `managers/CommandsManager.java` (registra `proxycore`/`pcore`/`proxyreload`), `Main.java` (pasa `logger`, log de módulos), `announce/AnnouncementManager.java` (nuevo `restart()`, `reload()` delega), `src/main/resources/config.yml`, `gradle.properties` (1.0.1 → 1.0.2), `README.md`, `AGENTS.md`, `MEMORY.md`
- **Descripción:** Whitelist estricta en `CommandExecuteEvent` con `CommandResult.denied()`. El matching es por nombre base normalizado, así que `gamemode` cubre `/gamemODE`, `/minecraft:gamemode creative` y cualquier variante con namespace; los argumentos nunca se inspeccionan. Exentos: consola, `ProxyCore.commandblocker.bypass` y (con `always-allow-proxy-commands`) todo comando registrado en el proxy. Mensaje MiniMessage configurable + log opcional con placeholders. `/proxycore reload` recarga todo y reinicia anuncios; usa `ConfigManager.reload()` para no tumbar el proxy con un YAML corrupto. `applyDefaults()` migra configs 1.0.1 sin perder valores y evita el lockout de todos los jugadores.
- **Descartado:** `PlayerAvailableCommandsEvent` — presente en la API 4.2.1 pero **nunca disparado** por Velocity (verificado en `ClientPlaySessionHandler`, `ConnectedPlayer`, `ClientConfigSessionHandler` y los `CommandHandler` de `dev/4.0.0`); además el root node que recibiría es el root compartido del dispatcher Brigadier y mutarlo rompería la ejecución de comandos para todos los jugadores.
- **Verificación:** `gradle clean build` OK → `out/ProxyCore-1.0.2.jar`. Harness temporal contra las clases compiladas: 19 casos de normalización (espacios, `/`, mayúsculas, `plugin:cmd`, `a:b:c`, vacío) y 5 escenarios de config (1.0.1 legacy, whitelist con namespaces, lista vacía explícita, YAML corrupto con `reload() == false` conservando valores previos, `config.yml` empaquetado == defaults). Todos correctos.

### 2026-09-25 — Colores del logo en consola (bug RGB + serializer oficial)
- **Archivos modificados:** `Main.java`
- **Descripción:** El logo de inicio salía monocromo porque el mapper ANSI manual comparaba RGB clásicos (`0,255,255` etc.) contra la paleta Minecraft de Adventure (`#55FFFF` aqua, `#FFFF55` yellow, `#FF5555` red, `#55FF55` green) — ningún color matcheaba y todo caía a `"37"` (blanco). Se eliminaron `serializeToAnsi`/`appendAnsi`/`getAnsiColorCode` (~60 líneas) y se usa `ANSIComponentSerializer.ansi()` (transitivo de velocity-api 4.2.1, Adventure 5.2.0), que detecta el nivel de color del terminal (`ColorLevel.compute()`, respeta `-Dnet.kyori.ansi.colorLevel`) y degrada a texto plano en entornos headless. Nota: `logs/latest.log` nunca tendrá colores — Velocity aplica `%stripAnsi` en el file appender por diseño; los colores solo viven en la consola live. Verificado empíricamente: aqua→`ESC[96m`, yellow→`ESC[93m`, red→`ESC[91m`, green→`ESC[92m`. Build OK → `out/ProxyCore-1.0.1.jar`.

### 2026-09-25 — Habilitar y cablear MOTD del proxy
- **Archivos modificados:** `config/ConfigManager.java`, `listener/MOTDListener.java`, `src/main/resources/config.yml` (por el usuario)
- **Descripción:** `motd.enabled: true` agregado al config por el usuario; se cableó el getter `isMotdEnabled()` (default true), early-return en listener, defaults programáticos actualizados, migración a `JoinConfiguration`, sample players con UUID real, eliminado `@SuppressWarnings("deprecation")` residual. Verificado contra javadocs de Velocity 4.1.0 (línea 4.x) por pedido del usuario.

## 🔗 Referencias y Recursos

- [Velocity Docs](https://docs.papermc.io/velocity/)
- [Velocity API Javadoc 4.1.0](https://jd.papermc.io/velocity/4.1.0/)
- [ServerPing.Builder javadoc](https://jd.papermc.io/velocity/4.1.0/com/velocitypowered/api/proxy/server/ServerPing.Builder.html)
- [Command API (docs.papermc.io)](https://docs.papermc.io/velocity/dev/command-api/)
- [Adventure MiniMessage Format](https://docs.papermc.io/adventure/minimessage/format/)
- [MiniMessage Web Viewer](https://webui.advntr.dev/)
