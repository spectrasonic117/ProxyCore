package com.spectrasonic.ProxyCore.chat;

import com.spectrasonic.ProxyCore.config.ConfigManager;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class StaffChatCommand implements SimpleCommand {

    private static final Set<UUID> STAFF_CHAT_TOGGLED = Collections.synchronizedSet(new HashSet<>());
    private static final MinecraftChannelIdentifier STAFF_CHANNEL = MinecraftChannelIdentifier.create("dopamine",
            "staffchat");

    private final ProxyServer proxy;
    private final ConfigManager configManager;
    private final MiniMessage miniMessage;

    public StaffChatCommand(ProxyServer proxy, ConfigManager configManager) {
        this.proxy = proxy;
        this.configManager = configManager;
        this.miniMessage = MiniMessage.miniMessage();
    }

    public static boolean isToggled(UUID uuid) {
        return STAFF_CHAT_TOGGLED.contains(uuid);
    }

    public static void removePlayer(UUID uuid) {
        STAFF_CHAT_TOGGLED.remove(uuid);
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
            invocation.source().sendMessage(
                    MiniMessage.miniMessage().deserialize("<red>This command can only be executed by a player.</red>"));
            return;
        }

        if (!player.hasPermission("ProxyCore.staffchat")) {
            player.sendMessage(
                    MiniMessage.miniMessage().deserialize("<red>You don't have permission to use staff chat.</red>"));
            return;
        }

        String[] args = invocation.arguments();
        if (args.length == 0) {
            toggleStaffChat(player);
            return;
        }

        String message = String.join(" ", args);
        broadcastStaffMessage(player, message);
    }

    private void toggleStaffChat(Player player) {
        UUID uuid = player.getUniqueId();
        if (STAFF_CHAT_TOGGLED.remove(uuid)) {
            player.sendMessage(MiniMessage.miniMessage()
                    .deserialize("<green>Staff chat <bold>disabled</bold>.</green>"));
        } else {
            STAFF_CHAT_TOGGLED.add(uuid);
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<green>Staff chat <bold>enabled</bold>. All your messages will be sent to staff.</green>"));
        }
    }

    public void broadcastStaffMessage(Player sender, String rawMessage) {
        String serverName = sender.getCurrentServer()
                .map(conn -> conn.getServerInfo().getName())
                .orElse("unknown");

        String format = configManager.getStaffChatFormat();
        Component formatted = formatMessage(format, sender.getUsername(), serverName, rawMessage);

        for (Player online : proxy.getAllPlayers()) {
            if (online.hasPermission("ProxyCore.staffchat")) {
                online.sendMessage(formatted);
            }
        }

        byte[] pluginMessageData = serializeStaffChatMessage(sender.getUsername(), serverName, rawMessage);
        for (RegisteredServer server : proxy.getAllServers()) {
            server.sendPluginMessage(STAFF_CHANNEL, pluginMessageData);
        }
    }

    private Component formatMessage(String format, String playerName, String server, String message) {
        String miniMessageStr = format
                .replace("{player}", playerName)
                .replace("{server}", server)
                .replace("{message}", message);
        return miniMessage.deserialize(miniMessageStr);
    }

    private byte[] serializeStaffChatMessage(String player, String server, String message) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(buffer);
            out.writeUTF(player);
            out.writeUTF(server);
            out.writeUTF(message);
            return buffer.toByteArray();
        } catch (IOException e) {
            return new byte[0];
        }
    }
}
