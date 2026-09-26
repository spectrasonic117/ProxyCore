package com.spectrasonic.ProxyCore.command;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.util.Optional;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class GotoCommand implements SimpleCommand {

    private final ProxyServer proxy;
    private final MiniMessage miniMessage;

    public GotoCommand(ProxyServer proxy) {
        this.proxy = proxy;
        this.miniMessage = MiniMessage.miniMessage();
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length < 1) {
            invocation.source().sendMessage(
                    miniMessage.deserialize("<red>Usage: /goto <player></red>"));
            return;
        }

        String targetName = args[0];

        if (!(invocation.source() instanceof Player sender)) {
            invocation.source().sendMessage(
                    miniMessage.deserialize("<red>This command can only be executed by a player.</red>"));
            return;
        }

        if (!sender.hasPermission("ProxyCore.goto")) {
            sender.sendMessage(miniMessage.deserialize("<red>You don't have permission to use this command.</red>"));
            return;
        }

        Optional<Player> targetOpt = proxy.getPlayer(targetName);
        if (targetOpt.isEmpty()) {
            sender.sendMessage(
                    miniMessage.deserialize("<red>Player <yellow>" + targetName + "</yellow> is not online.</red>"));
            return;
        }

        Player target = targetOpt.get();
        if (target.equals(sender)) {
            sender.sendMessage(miniMessage.deserialize("<yellow>You can't teleport to yourself!</yellow>"));
            return;
        }

        Optional<RegisteredServer> targetServer = target.getCurrentServer()
                .map(conn -> conn.getServer());

        if (targetServer.isEmpty()) {
            sender.sendMessage(
                    miniMessage.deserialize("<red>Could not determine " + target.getUsername() + "'s server.</red>"));
            return;
        }

        RegisteredServer server = targetServer.get();
        String targetServerName = server.getServerInfo().getName();

        sender.getCurrentServer().ifPresent(currentConn -> {
            if (currentConn.getServerInfo().getName().equalsIgnoreCase(targetServerName)) {
                sender.sendMessage(miniMessage.deserialize(
                        "<yellow>You are already on the same server as " + target.getUsername() + ".</yellow>"));
                return;
            }
        });

        sender.createConnectionRequest(server).connect().thenAccept(result -> {
            if (result.isSuccessful()) {
                sender.sendMessage(miniMessage.deserialize(
                        "<green>Teleported to <aqua>" + target.getUsername() + "</aqua> on <gold>" + targetServerName
                                + "</gold>.</green>"));
            } else {
                sender.sendMessage(miniMessage.deserialize(
                        "<red>Failed to connect to " + targetServerName + ".</red>"));
            }
        });
    }
}
