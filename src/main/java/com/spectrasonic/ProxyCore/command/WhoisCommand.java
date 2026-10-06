package com.spectrasonic.ProxyCore.command;

import com.spectrasonic.ProxyCore.managers.MessageManager;
import com.spectrasonic.ProxyCore.managers.SeenManager;
import com.spectrasonic.ProxyCore.util.MessageUtils;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class WhoisCommand implements SimpleCommand {

    private final ProxyServer proxy;
    private final SeenManager seenManager;

    public WhoisCommand(ProxyServer proxy, SeenManager seenManager) {
        this.proxy = proxy;
        this.seenManager = seenManager;
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length < 1) {
            MessageUtils.sendMessage(invocation.source(), "messages.whois.usage");
            return;
        }

        if (!invocation.source().hasPermission("ProxyCore.whois")) {
            MessageUtils.sendNoPermission(invocation.source());
            return;
        }

        String targetName = args[0];
        Optional<Player> targetOpt = proxy.getPlayer(targetName);
        if (targetOpt.isEmpty()) {
            MessageUtils.sendMessage(invocation.source(), "messages.whois.player_offline_use_seen",
                    "player", targetName);
            return;
        }

        Player target = targetOpt.get();

        String server = target.getCurrentServer()
                .map(conn -> conn.getServerInfo().getName())
                .orElse("unknown");

        StringBuilder session = new StringBuilder("unknown");
        if (seenManager.getRecord(target.getUniqueId()).isPresent()) {
            long lastJoin = seenManager.getRecord(target.getUniqueId()).get().lastJoin;
            if (lastJoin > 0) {
                session = new StringBuilder(
                        SeenManager.formatDuration(System.currentTimeMillis() - lastJoin));
            }
        }

        MessageUtils.sendMessage(invocation.source(), "messages.whois.header",
                "player", target.getUsername(),
                "server", server);
        MessageUtils.sendMessage(invocation.source(), "messages.whois.uuid_line",
                "uuid", target.getUniqueId().toString());
        MessageUtils.sendMessage(invocation.source(), "messages.whois.ip_ping_version_line",
                "ip", getIp(target),
                "ping", String.valueOf(target.getPing()),
                "version", target.getProtocolVersion().getName());
        MessageUtils.sendMessage(invocation.source(), "messages.whois.session_line",
                "session", session.toString());
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