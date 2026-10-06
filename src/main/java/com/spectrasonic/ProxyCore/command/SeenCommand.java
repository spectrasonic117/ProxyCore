package com.spectrasonic.ProxyCore.command;

import com.spectrasonic.ProxyCore.config.ConfigManager;
import com.spectrasonic.ProxyCore.managers.MessageManager;
import com.spectrasonic.ProxyCore.managers.SeenManager;
import com.spectrasonic.ProxyCore.managers.SeenManager.SeenRecord;
import com.spectrasonic.ProxyCore.util.MessageUtils;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public class SeenCommand implements SimpleCommand {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ProxyServer proxy;
    private final ConfigManager configManager;
    private final SeenManager seenManager;

    public SeenCommand(ProxyServer proxy, ConfigManager configManager, SeenManager seenManager) {
        this.proxy = proxy;
        this.configManager = configManager;
        this.seenManager = seenManager;
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length < 1) {
            MessageUtils.sendMessage(invocation.source(), "messages.seen.usage");
            return;
        }

        if (!invocation.source().hasPermission("ProxyCore.seen")) {
            MessageUtils.sendNoPermission(invocation.source());
            return;
        }

        if (!configManager.isSeenEnabled()) {
            MessageUtils.sendMessage(invocation.source(), "messages.seen.tracking_disabled");
            return;
        }

        String targetName = args[0];

        Optional<Player> onlineOpt = proxy.getPlayer(targetName);
        if (onlineOpt.isPresent()) {
            sendOnlineInfo(invocation, onlineOpt.get());
            return;
        }

        Optional<SeenRecord> recordOpt = seenManager.findByName(targetName);
        if (recordOpt.isEmpty()) {
            MessageUtils.sendMessage(invocation.source(), "messages.seen.no_record",
                    "player", targetName);
            return;
        }

        sendOfflineInfo(invocation, recordOpt.get());
    }

    private void sendOnlineInfo(Invocation invocation, Player target) {
        String server = target.getCurrentServer()
                .map(conn -> conn.getServerInfo().getName())
                .orElse("unknown");

        String connectedFor = seenManager.getRecord(target.getUniqueId())
                .map(record -> record.lastJoin)
                .filter(lastJoin -> lastJoin > 0)
                .map(lastJoin -> SeenManager.formatDuration(System.currentTimeMillis() - lastJoin))
                .orElse("unknown");

        MessageUtils.sendMessage(invocation.source(), "messages.seen.online",
                "player", target.getUsername(),
                "server", server,
                "duration", connectedFor);
    }

    private void sendOfflineInfo(Invocation invocation, SeenRecord record) {
        long now = System.currentTimeMillis();

        String lastSeen = record.lastQuit > 0
                ? SeenManager.formatDuration(now - record.lastQuit) + " ago ("
                        + formatDate(record.lastQuit) + ")"
                : "unknown";
        String lastSession = record.lastJoin > 0 && record.lastQuit > 0
                ? SeenManager.formatDuration(record.lastQuit - record.lastJoin)
                : "unknown";

        MessageUtils.sendMessage(invocation.source(), "messages.seen.offline",
                "player", record.name,
                "last_seen", lastSeen);
        MessageUtils.sendMessage(invocation.source(), "messages.seen.first_join_last_session",
                "first_join", formatDate(record.firstJoin),
                "last_session", lastSession);

        if (configManager.isSeenShowIp() && !record.lastIp.isEmpty()) {
            MessageUtils.sendMessage(invocation.source(), "messages.seen.last_ip_line",
                    "ip", record.lastIp);
        }
        if (!record.lastServer.isEmpty()) {
            MessageUtils.sendMessage(invocation.source(), "messages.seen.last_server_line",
                    "server", record.lastServer);
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        String prefix = args.length > 0 ? args[args.length - 1].toLowerCase(Locale.ROOT) : "";
        Set<String> suggestions = new LinkedHashSet<>();
        for (Player player : proxy.getAllPlayers()) {
            suggestions.add(player.getUsername());
        }
        suggestions.addAll(seenManager.getKnownNames());
        List<String> matches = new ArrayList<>();
        for (String name : suggestions) {
            if (name.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                matches.add(name);
            }
        }
        return matches;
    }

    private static String formatDate(long epochMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault())
                .format(DATE_FORMAT);
    }
}