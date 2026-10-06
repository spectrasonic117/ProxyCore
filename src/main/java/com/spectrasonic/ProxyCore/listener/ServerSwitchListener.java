package com.spectrasonic.ProxyCore.listener;

import com.spectrasonic.ProxyCore.config.ConfigManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Sends a configurable MiniMessage line whenever a player finishes connecting to a backend server.
 *
 * <p><b>Velocity 4.x limitation:</b> there is no public event fired for system messages
 * (Velocity's own "Connected to &lt;server&gt;" announcement or backend-sent packets), so this
 * listener can only <i>add</i> a custom message — it cannot cancel Velocity's. Set
 * <c>announce-server-translations = false</c> in <c>velocity.toml</c> to suppress Velocity's own
 * announcement and let this listener be the only message the player sees on a server switch.
 */
public class ServerSwitchListener {

    /** Substitute for {@code {previous_server}} when the player has no prior backend. */
    private static final String PREVIOUS_SERVER_FALLBACK = "proxy";

    private final ConfigManager configManager;
    private final MiniMessage miniMessage;

    public ServerSwitchListener(ConfigManager configManager) {
        this.configManager = configManager;
        this.miniMessage = MiniMessage.miniMessage();
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        if (!configManager.isServerSwitchEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        String serverName = event.getServer().getServerInfo().getName();
        String previousServer = event.getPreviousServer()
                .map(server -> server.getServerInfo().getName())
                .orElse(PREVIOUS_SERVER_FALLBACK);

        String format = configManager.getServerSwitchMessage()
                .replace("{player}", player.getUsername())
                .replace("{server}", serverName)
                .replace("{previous_server}", previousServer);

        player.sendMessage(miniMessage.deserialize(format));
    }
}