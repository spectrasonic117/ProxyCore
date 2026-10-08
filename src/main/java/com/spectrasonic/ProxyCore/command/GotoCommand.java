package com.spectrasonic.ProxyCore.command;

import com.spectrasonic.ProxyCore.managers.MessageManager;
import com.spectrasonic.ProxyCore.util.MessageUtils;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.util.Optional;

public class GotoCommand implements SimpleCommand {

    private final ProxyServer proxy;

    public GotoCommand(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length < 1) {
            MessageUtils.sendMessage(invocation.source(), "messages.goto.usage");
            return;
        }

        String targetName = args[0];

        if (!(invocation.source() instanceof Player sender)) {
            MessageUtils.sendOnlyPlayers(invocation.source());
            return;
        }

        if (!sender.hasPermission("ProxyCore.goto")) {
            MessageUtils.sendNoPermission(sender);
            return;
        }

        Optional<Player> targetOpt = proxy.getPlayer(targetName);
        if (targetOpt.isEmpty()) {
            MessageUtils.sendPlayerNotFound(sender, targetName);
            return;
        }

        Player target = targetOpt.get();
        if (target.equals(sender)) {
            MessageUtils.rawMessage(sender,
                    MessageManager.getMessage("messages.goto.cannot_teleport_self"));
            return;
        }

        Optional<RegisteredServer> targetServer = target.getCurrentServer()
                .map(conn -> conn.getServer());

        if (targetServer.isEmpty()) {
            MessageUtils.sendMessage(sender, "messages.common.could_not_determine_server",
                    "player", target.getUsername());
            return;
        }

        RegisteredServer server = targetServer.get();
        String targetServerName = server.getServerInfo().getName();

        sender.getCurrentServer().ifPresent(currentConn -> {
            if (currentConn.getServerInfo().getName().equalsIgnoreCase(targetServerName)) {
                MessageUtils.rawMessage(sender,
                        MessageManager.getMessage("messages.goto.already_same_server",
                                "player", target.getUsername()));
                return;
            }
        });

        sender.createConnectionRequest(server).connect().thenAccept(result -> {
            if (result.isSuccessful()) {
                MessageUtils.sendMessage(sender,
                        "messages.goto.teleported",
                                "player", target.getUsername(),
                                "server", targetServerName);
            } else {
                MessageUtils.sendMessage(sender,
                        "messages.goto.connection_failed",
                                "server", targetServerName);
            }
        });
    }
}