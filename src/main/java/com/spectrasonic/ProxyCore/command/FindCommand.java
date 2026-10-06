package com.spectrasonic.ProxyCore.command;

import com.spectrasonic.ProxyCore.managers.MessageManager;
import com.spectrasonic.ProxyCore.util.MessageUtils;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import java.util.Optional;

public class FindCommand implements SimpleCommand {

    private final ProxyServer proxy;

    public FindCommand(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length < 1) {
            MessageUtils.sendMessage(invocation.source(), "messages.find.usage");
            return;
        }

        String targetName = args[0];

        if (!(invocation.source() instanceof Player sender)) {
            sendResultToConsole(invocation, targetName);
            return;
        }

        if (!sender.hasPermission("ProxyCore.find")) {
            MessageUtils.sendNoPermission(sender);
            return;
        }

        Optional<Player> targetOpt = proxy.getPlayer(targetName);
        if (targetOpt.isEmpty()) {
            MessageUtils.sendPlayerNotFound(sender, targetName);
            return;
        }

        Player target = targetOpt.get();
        target.getCurrentServer().ifPresentOrElse(
                conn -> MessageUtils.sendMessage(sender, "messages.find.player_location",
                        "player", target.getUsername(),
                        "server", conn.getServerInfo().getName()),
                () -> MessageUtils.sendMessage(sender, "messages.common.could_not_determine_server",
                        "player", target.getUsername()));
    }

    private void sendResultToConsole(Invocation invocation, String targetName) {
        Optional<Player> targetOpt = proxy.getPlayer(targetName);
        if (targetOpt.isEmpty()) {
            MessageUtils.sendMessage(invocation.source(), "messages.find.player_not_found_console",
                    "player", targetName);
            return;
        }
        Player target = targetOpt.get();
        target.getCurrentServer().ifPresentOrElse(
                conn -> MessageUtils.sendMessage(invocation.source(), "messages.find.player_location",
                        "player", target.getUsername(),
                        "server", conn.getServerInfo().getName()),
                () -> MessageUtils.sendMessage(invocation.source(), "messages.common.could_not_determine_server",
                        "player", target.getUsername()));
    }
}