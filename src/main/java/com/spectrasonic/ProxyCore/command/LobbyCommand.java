package com.spectrasonic.ProxyCore.command;

import com.spectrasonic.ProxyCore.config.ConfigManager;
import com.spectrasonic.ProxyCore.managers.MessageManager;
import com.spectrasonic.ProxyCore.util.MessageUtils;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.util.Optional;

public class LobbyCommand implements SimpleCommand {

    private final ProxyServer proxy;
    private final ConfigManager configManager;

    public LobbyCommand(ProxyServer proxy, ConfigManager configManager) {
        this.proxy = proxy;
        this.configManager = configManager;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
            MessageUtils.sendOnlyPlayers(invocation.source());
            return;
        }

        String targetServer = configManager.getTargetServer();
        Optional<RegisteredServer> server = proxy.getServer(targetServer);

        if (server.isEmpty()) {
            MessageUtils.rawMessage(player,
                    MessageManager.getMessage("messages.lobby.server_not_found", "server", targetServer));
            return;
        }

        RegisteredServer currentServer = player.getCurrentServer()
                .map(conn -> conn.getServer())
                .orElse(null);

        if (currentServer != null && currentServer.getServerInfo().getName().equalsIgnoreCase(targetServer)) {
            MessageUtils.rawMessage(player,
                    MessageManager.getMessage("messages.lobby.already_connected", "server", targetServer));
            return;
        }

        player.createConnectionRequest(server.get()).connect().thenAccept(result -> {
            if (result.isSuccessful()) {
                MessageUtils.successMessage(player,
                        MessageManager.getMessage("messages.lobby.connecting", "server", targetServer));
            } else {
                MessageUtils.denyMessage(player,
                        MessageManager.getMessage("messages.lobby.connection_failed", "server", targetServer));
            }
        });
    }
}