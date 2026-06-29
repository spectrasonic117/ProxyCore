package com.spectrasonic.ProxyCore.command;

import com.spectrasonic.ProxyCore.config.ConfigManager;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
            invocation.source().sendMessage(
                    Component.text("This command can only be executed by a player.", NamedTextColor.RED));
            return;
        }

        String targetServer = configManager.getTargetServer();
        Optional<RegisteredServer> server = proxy.getServer(targetServer);

        if (server.isEmpty()) {
            player.sendMessage(Component.text("Server not found: " + targetServer, NamedTextColor.RED));
            return;
        }

        RegisteredServer currentServer = player.getCurrentServer()
                .map(conn -> conn.getServer())
                .orElse(null);

        if (currentServer != null && currentServer.getServerInfo().getName().equalsIgnoreCase(targetServer)) {
            player.sendMessage(
                    Component.text("You are already connected to " + targetServer + "!", NamedTextColor.YELLOW));
            return;
        }

        player.createConnectionRequest(server.get()).connect().thenAccept(result -> {
            if (result.isSuccessful()) {
                player.sendMessage(Component.text("Connecting to " + targetServer + "...", NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("Failed to connect to " + targetServer + ".", NamedTextColor.RED));
            }
        });
    }
}
