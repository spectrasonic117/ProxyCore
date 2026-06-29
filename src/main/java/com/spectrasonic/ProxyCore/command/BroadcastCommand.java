package com.spectrasonic.ProxyCore.command;

import com.spectrasonic.ProxyCore.config.ConfigManager;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class BroadcastCommand implements SimpleCommand {

    private static final MinecraftChannelIdentifier BROADCAST_CHANNEL = MinecraftChannelIdentifier.create("dopamine",
            "broadcast");

    private final ProxyServer proxy;
    private final ConfigManager configManager;
    private final MiniMessage miniMessage;

    public BroadcastCommand(ProxyServer proxy, ConfigManager configManager) {
        this.proxy = proxy;
        this.configManager = configManager;
        this.miniMessage = MiniMessage.miniMessage();
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 0) {
            invocation.source().sendMessage(
                    MiniMessage.miniMessage().deserialize("<red>Usage: /broadcast <message></red>"));
            return;
        }

        String message = String.join(" ", args);
        broadcastMessage(message);
    }

    private void broadcastMessage(String message) {
        String format = configManager.getBroadcastFormat();
        String miniMessageStr = format.replace("{message}", message);
        Component formatted = miniMessage.deserialize(miniMessageStr);

        for (Player online : proxy.getAllPlayers()) {
            online.sendMessage(formatted);
        }

        byte[] data = serializeBroadcast(message);
        for (RegisteredServer server : proxy.getAllServers()) {
            server.sendPluginMessage(BROADCAST_CHANNEL, data);
        }
    }

    private byte[] serializeBroadcast(String message) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(buffer);
            out.writeUTF(message);
            return buffer.toByteArray();
        } catch (IOException e) {
            return new byte[0];
        }
    }
}
