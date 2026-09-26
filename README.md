# ProxyCore

Plugin de proxy **Velocity** para la red **Dopamine Network**. Añade comandos de red, MOTD configurable con modo mantenimiento, anuncios programados, staff chat y formatos de chat con MiniMessage.

| | |
|---|---|
| **Plataforma** | Velocity 4.2.1-SNAPSHOT |
| **Java** | 25 |
| **Build** | Gradle 9.5.1 |
| **Texto** | Adventure MiniMessage 4.17.0 |
| **Versión actual** | 1.0.1 |

---

## Compilación

```bash
gradle build
```

> El JAR resultante se genera en `out/ProxyCore-<versión>.jar` (p. ej. `out/ProxyCore-1.0.1.jar`).
> Nota: el proyecto no incluye el wrapper de Gradle (`gradlew`); usa una instalación local de Gradle 9.x.

## Instalación

1. Copia `out/ProxyCore-1.0.1.jar` a la carpeta `plugins/` del proxy Velocity.
2. Arranca el proxy: se creará `plugins/ProxyCore/config.yml` con los valores por defecto.
3. Edita `config.yml` según tu red y reinicia (o usa los comandos de recarga).

---

## Comandos

| Comando | Alias | Descripción | Permiso |
|---------|-------|-------------|---------|
| `/lobby` | `/hub`, `/spawn`, `/leave` | Te conecta al servidor lobby (`target-server`) | — (todos los jugadores) |
| `/find <jugador>` | — | Muestra en qué servidor de la red está un jugador | `ProxyCore.find` |
| `/goto <jugador>` | — | Te conecta al servidor donde está el jugador indicado | `ProxyCore.goto` |
| `/broadcast <mensaje>` | `/br` | Anuncia un mensaje en **toda la red** (formato `chat.broadcast.format`) | `ProxyCore.broadcast` |
| `/staffchat [mensaje]` | `/sc` | Sin argumentos: alterna el modo staffchat. Con mensaje: lo envía a todo el staff conectado | `ProxyCore.staffchat` |
| `/maintenance [on\|off]` | — | Sin argumentos: muestra el estado. `on`/`enable` o `off`/`disable`: alterna el modo mantenimiento (MOTD) | `ProxyCore.maintenance` |
| `/motd reload` | — | Recarga `config.yml` (sección MOTD) | `ProxyCore.motd` |
| `/announce [true\|false\|reload]` | — | Sin argumentos: estado de anuncios. `true`/`false`: activa/desactiva. `reload`: recarga la config y reinicia el temporizador | `ProxyCore.announce` |

---

## Permisos

| Permiso | Comando | Por defecto |
|---------|---------|-------------|
| `ProxyCore.broadcast` | `/broadcast`, `/br` | Solo consola / staff con permiso |
| `ProxyCore.find` | `/find` | Solo consola / staff con permiso |
| `ProxyCore.goto` | `/goto` | Solo consola / staff con permiso |
| `ProxyCore.staffchat` | `/staffchat`, `/sc` | Solo consola / staff con permiso |
| `ProxyCore.maintenance` | `/maintenance` | Solo consola / staff con permiso |
| `ProxyCore.motd` | `/motd reload` | Solo consola / staff con permiso |
| `ProxyCore.announce` | `/announce` | Solo consola / staff con permiso |
| *(sin permiso)* | `/lobby`, `/hub`, `/spawn`, `/leave` | Todos los jugadores |

### Cómo se aplican los permisos

- **Los permisos se evalúan en el proxy.** Si no tienes el permiso, el proxy responde con el mensaje
  `You don't have permission to use this command.` y el comando **no llega nunca a los servidores backend**.
- **La consola del proxy tiene todos los permisos** por defecto.
- **Los jugadores necesitan un proveedor de permisos en el proxy** para recibir estos nodos. Instala, por ejemplo,
  **LuckPerms para Velocity** en el proxy y otorga los nodos `ProxyCore.*` a tus grupos de staff:

  ```
  /lp group staff permission set ProxyCore.broadcast true
  /lp group staff permission set ProxyCore.staffchat true
  // ... resto de nodos
  ```

- Sin proveedor de permisos, estos comandos solo son utilizables desde la consola del proxy.

---

## Configuración (`plugins/ProxyCore/config.yml`)

```yaml
# Servidor al que te lleva /lobby
target-server: "lobby"

# MOTD de la lista de servidores (soporta MiniMessage)
motd:
  enabled: true
  maintenance: false          # true = muestra maintenance-lines en vez de players
  maintenance-lines:
    - "<yellow>⚠ <red>El Servidor esta en mantenimiento!</red> ⚠</yellow>"
  players:
    - "<gradient:gold:yellow>✦ Dopamine Network ✦</gradient>"

# Anuncios programados en chat
announcements:
  enabled: false
  interval-seconds: 300       # tiempo entre anuncios
  messages:
    - "<gradient:aqua:light_purple>★ Welcome to Dopamine Network!</gradient>"

# Formatos de chat (MiniMessage)
chat:
  broadcast:
    format: "..."             # placeholder: {message}
  staff:
    format: "..."             # placeholders: {player}, {message}

# Resource pack (configurado, aún no aplicado)
resource-pack:
  enabled: false
  url: ""
  sha1: ""
```

**Notas:**

- `/maintenance` **solo cambia el MOTD** de la lista de servidores; no bloquea conexiones de jugadores.
- `/announce` y `/maintenance` **guardan el estado en `config.yml`** al alternarlos; el resto de cambios
  requieren editar el archivo y usar `/motd reload` o reiniciar.
- Las líneas de MOTD y anuncios usan **MiniMessage** (gradientes, colores, negritas).

---

## Canales de plugin messaging (para plugins backend)

ProxyCore se comunica con los plugins de los servidores backend (Spigot/Paper) por plugin messaging:

| Canal | Dirección | Payload |
|-------|-----------|---------|
| `dopamine:announcement` | proxy → backends | `UTF` mensaje crudo |
| `dopamine:staffchat` | proxy → backends | `UTF` jugador, `UTF` servidor, `UTF` mensaje |
| `dopamine:broadcast` | proxy → backends | `UTF` mensaje crudo (formato aplicado por el backend) |

Úsalos en tus plugins de backend para mostrar broadcasts/staff chat con el formato propio del backend
(chat de red con ActionBar, log a DB, etc.).

---

## Limitaciones conocidas

- **Toggle de `/staffchat`**: el estado vive solo en memoria, se pierde al reiniciar el proxy y todavía
  no redirige el flujo de chat normal (el envío de mensajes con argumentos sí funciona).
  *Pendiente: consumir el toggle con `PlayerChatEvent` y persistirlo.*
- **`resource-pack`**: la sección existe en config pero aún no está conectada a ningún listener.
- **`/broadcast`** no aplica límite de longitud ni escapa tags MiniMessage del mensaje (uso solo para staff de confianza).
- **`AGENTS.md`**: documentación interna de desarrollo del proyecto.
