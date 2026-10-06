package com.spectrasonic.ProxyCore.command;

import com.spectrasonic.ProxyCore.config.ConfigManager;
import com.spectrasonic.ProxyCore.managers.SeenManager;
import com.spectrasonic.ProxyCore.managers.SeenManager.SeenRecord;
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
import net.kyori.adventure.text.minimessage.MiniMessage;

public class SeenCommand implements SimpleCommand {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ProxyServer proxy;
    private final ConfigManager configManager;
    private final SeenManager seenManager;
    private final MiniMessage miniMessage;

    public SeenCommand(ProxyServer proxy, ConfigManager configManager, SeenManager seenManager) {
        this.proxy = proxy;
        this.configManager = configManager;
        this.seenManager = seenManager;
        this.miniMessage = MiniMessage.miniMessage();
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length < 1) {
            invocation.source().sendMessage(
                    miniMessage.deserialize("<red>Usage: /seen <player></red>"));
            return;
        }

        if (!invocation.source().hasPermission("ProxyCore.seen")) {
            invocation.source().sendMessage(
                    miniMessage.deserialize("<red>You don't have permission to use this command.</red>"));
            return;
        }

        if (!configManager.isSeenEnabled()) {
            invocation.source().sendMessage(
                    miniMessage.deserialize("<red>Seen tracking is disabled in the config.</red>"));
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
            invocation.source().sendMessage(
                    miniMessage.deserialize("<red>No record found for <yellow>" + targetName
                            + "</yellow>.</red>"));
            return;
        }

        sendOfflineInfo(invocation, recordOpt.get());
    }

    private void sendOnlineInfo(Invocation invocation, Player target) {
        String server = target.getCurrentServer()
                .map(conn -> conn.getServerInfo().getName())
                .orElse("<dark_gray>unknown</dark_gray>");

        String connectedFor = seenManager.getRecord(target.getUniqueId())
                .map(record -> record.lastJoin)
                .filter(lastJoin -> lastJoin > 0)
                .map(lastJoin -> SeenManager.formatDuration(System.currentTimeMillis() - lastJoin))
                .orElse("unknown");

        invocation.source().sendMessage(miniMessage.deserialize(
                "<dark_aqua>Seen</dark_aqua> <dark_gray>»</dark_gray> <aqua>" + target.getUsername()
                        + "</aqua> <green>is online now</green> <gray>on</gray> <green>" + server
                        + "</green> <gray>for</gray> <white>" + connectedFor + "</white>"));
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

        invocation.source().sendMessage(miniMessage.deserialize(
                "<dark_aqua>Seen</dark_aqua> <dark_gray>»</dark_gray> <aqua>" + record.name
                        + "</aqua> <red>is offline</red> <gray>last seen</gray> <white>" + lastSeen
                        + "</white>"));
        invocation.source().sendMessage(miniMessage.deserialize(
                "<gray>First join:</gray> <white>" + formatDate(record.firstJoin) + "</white> "
                        + "<gray>Last session:</gray> <white>" + lastSession + "</white>"));

        StringBuilder details = new StringBuilder();
        if (configManager.isSeenShowIp() && !record.lastIp.isEmpty()) {
            details.append("<gray>Last IP:</gray> <white>").append(record.lastIp).append("</white> ");
        }
        if (!record.lastServer.isEmpty()) {
            details.append("<gray>Last server:</gray> <green>").append(record.lastServer)
                    .append("</green>");
        }
        if (details.length() > 0) {
            invocation.source().sendMessage(miniMessage.deserialize(details.toString()));
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
