# 📦 ProxyCore — Memory Bank

> **Última actualización:** 2026-09-25 14:29
> **Proyecto:** ProxyCore v1.0.1
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
│   └── MotdReloadCommand.java    # /motd reload
├── chat/
│   └── StaffChatCommand.java     # /staffchat /sc (toggle estático)
├── managers/
│   ├── CommandsManager.java      # Registro central de comandos
│   └── ListenerManager.java      # Registro de listeners
├── config/
│   └── ConfigManager.java        # YAML load/save + defaults programáticos
├── announce/
│   └── AnnouncementManager.java  # Anuncios programados
└── listener/
    └── MOTDListener.java         # ProxyPingEvent → MOTD/ mantenimiento
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
- **Eventos:** `ProxyInitializeEvent`, `ProxyShutdownEvent`, `ProxyPingEvent` (`@Subscribe`)

### Adventure MiniMessage
- **Patrón:** `MiniMessage.miniMessage().deserialize("<green>...</green>")`
- **Tags usados:** `<color>`, `<gradient>`, `<bold>`, `<reset>`, `<white>`
- **MOTD multi-línea:** `Component.join(JoinConfiguration.newlines(), componentes)`

## 📝 Decisiones de Diseño

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

### Pendiente
- [ ] `resource-pack` en config existe pero no está cableado a ningún listener (sin uso)
- [ ] `/broadcast` no tiene check de permiso
- [ ] Staff chat: estado estático `STAFF_CHAT_TOGGLED` se pierde al reiniciar el proxy
- [ ] SnakeYAML `save()` descarta comentarios del config.yml (afecta `/maintenance`, `/announce`)
- [ ] AGENTS.md desactualizado: dice Velocity 3.4.0/Java 21/Gradle 8.8; real es 4.2.1-SNAPSHOT/Java 25

## 🐛 Problemas Conocidos y Soluciones

### `Component.join(Component.newline(), ...)` no compila contra Velocity 4.2.1 — RESUELTO
- **Síntoma:** `no suitable method found for join(TextComponent, List<Component>)`
- **Causa:** Adventure moderno (transitivo de velocity-api 4.2.1-SNAPSHOT) eliminó el overload `join(ComponentLike, ...)`.
- **Solución:** `Component.join(JoinConfiguration.newlines(), components)` con import `net.kyori.adventure.text.JoinConfiguration`.
- **Archivos afectados:** `listener/MOTDListener.java`

## 📜 Bitácora de Cambios

### 2026-09-25 — Habilitar y cablear MOTD del proxy
- **Archivos modificados:** `config/ConfigManager.java`, `listener/MOTDListener.java`, `src/main/resources/config.yml` (por el usuario)
- **Descripción:** `motd.enabled: true` agregado al config por el usuario; se cableó el getter `isMotdEnabled()` (default true), early-return en listener, defaults programáticos actualizados, migración a `JoinConfiguration`, sample players con UUID real, eliminado `@SuppressWarnings("deprecation")` residual. Verificado contra javadocs de Velocity 4.1.0 (línea 4.x) por pedido del usuario.

## 🔗 Referencias y Recursos

- [Velocity Docs](https://docs.papermc.io/velocity/)
- [Velocity API Javadoc 4.1.0](https://jd.papermc.io/velocity/4.1.0/)
- [ServerPing.Builder javadoc](https://jd.papermc.io/velocity/4.1.0/com/velocitypowered/api/proxy/server/ServerPing.Builder.html)
- [Adventure MiniMessage Format](https://docs.papermc.io/adventure/minimessage/format/)
- [MiniMessage Web Viewer](https://webui.advntr.dev/)
