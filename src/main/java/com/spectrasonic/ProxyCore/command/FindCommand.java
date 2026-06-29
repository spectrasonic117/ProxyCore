package com.spectrasonic.ProxyCore.command;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import java.util.Optional;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class FindCommand implements SimpleCommand {

    private final ProxyServer proxy;
    private final MiniMessage miniMessage;

    public FindCommand(ProxyServer proxy) {
        this.proxy = proxy;
        this.miniMessage = MiniMessage.miniMessage();
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length < 1) {
            invocation.source().sendMessage(
                    miniMessage.deserialize("<red>Usage: /find <player></red>"));
            return;
        }

        String targetName = args[0];

        if (!(invocation.source() instanceof Player sender)) {
            sendResultToConsole(invocation, targetName);
            return;
        }

        if (!sender.hasPermission("ProxyCore.find")) {
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
        target.getCurrentServer().ifPresentOrElse(
                conn -> sender.sendMessage(miniMessage.deserialize(
                        "<aqua>" + target.getUsername() + "</aqua> <gray>is on</gray> <green>"
                                + conn.getServerInfo().getName() + "</green>")),
                () -> sender.sendMessage(miniMessage.deserialize(
                        "<red>Could not determine " + target.getUsername() + "'s server.</red>")));
    }

    private void sendResultToConsole(Invocation invocation, String targetName) {
        Optional<Player> targetOpt = proxy.getPlayer(targetName);
        if (targetOpt.isEmpty()) {
            invocation.source().sendMessage(
                    miniMessage.deserialize("<red>Player " + targetName + " is not online.</red>"));
            return;
        }
        Player target = targetOpt.get();
        target.getCurrentServer().ifPresentOrElse(
                conn -> invocation.source().sendMessage(miniMessage.deserialize(
                        "<aqua>" + target.getUsername() + "</aqua> <gray>is on</gray> <green>"
                                + conn.getServerInfo().getName() + "</green>")),
                () -> invocation.source().sendMessage(miniMessage.deserialize(
                        "<red>Could not determine " + target.getUsername() + "'s server.</red>")));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("ProxyCore.find");
    }
}
