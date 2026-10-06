package com.spectrasonic.ProxyCore.command;

import com.spectrasonic.ProxyCore.managers.SeenManager;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class WhoisCommand implements SimpleCommand {

    private final ProxyServer proxy;
    private final SeenManager seenManager;
    private final MiniMessage miniMessage;

    public WhoisCommand(ProxyServer proxy, SeenManager seenManager) {
        this.proxy = proxy;
        this.seenManager = seenManager;
        this.miniMessage = MiniMessage.miniMessage();
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length < 1) {
            invocation.source().sendMessage(
                    miniMessage.deserialize("<red>Usage: /whois <player></red>"));
            return;
        }

        if (!invocation.source().hasPermission("ProxyCore.whois")) {
            invocation.source().sendMessage(
                    miniMessage.deserialize("<red>You don't have permission to use this command.</red>"));
            return;
        }

        String targetName = args[0];
        Optional<Player> targetOpt = proxy.getPlayer(targetName);
        if (targetOpt.isEmpty()) {
            invocation.source().sendMessage(
                    miniMessage.deserialize("<red>Player <yellow>" + targetName
                            + "</yellow> is not online. Use <white>/seen " + targetName
                            + "</white> for connection history.</red>"));
            return;
        }

        Player target = targetOpt.get();

        String server = target.getCurrentServer()
                .map(conn -> conn.getServerInfo().getName())
                .orElse("<dark_gray>unknown</dark_gray>");

        StringBuilder session = new StringBuilder("<dark_gray>unknown</dark_gray>");
        if (seenManager.getRecord(target.getUniqueId()).isPresent()) {
            long lastJoin = seenManager.getRecord(target.getUniqueId()).get().lastJoin;
            if (lastJoin > 0) {
                session = new StringBuilder(
                        SeenManager.formatDuration(System.currentTimeMillis() - lastJoin));
            }
        }

        invocation.source().sendMessage(miniMessage.deserialize(
                "<dark_aqua>Whois</dark_aqua> <dark_gray>»</dark_gray> <aqua>" + target.getUsername()
                        + "</aqua> <gray>on</gray> <green>" + server + "</green>"));
        invocation.source().sendMessage(miniMessage.deserialize(
                "<gray>UUID:</gray> <white>" + target.getUniqueId() + "</white>"));
        invocation.source().sendMessage(miniMessage.deserialize(
                "<gray>IP:</gray> <white>" + getIp(target) + "</white> "
                        + "<gray>Ping:</gray> <white>" + target.getPing() + "ms</white> "
                        + "<gray>Version:</gray> <white>" + target.getProtocolVersion().getName()
                        + "</white>"));
        invocation.source().sendMessage(miniMessage.deserialize(
                "<gray>Session:</gray> <white>" + session + "</white>"));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        String prefix = args.length > 0 ? args[args.length - 1].toLowerCase(Locale.ROOT) : "";
        List<String> suggestions = new ArrayList<>();
        for (Player player : proxy.getAllPlayers()) {
            if (player.getUsername().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                suggestions.add(player.getUsername());
            }
        }
        return suggestions;
    }

    private static String getIp(Player player) {
        return player.getRemoteAddress().getAddress() != null
                ? player.getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
    }
}
