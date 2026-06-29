package com.spectrasonic.ProxyCore.announce;

import com.spectrasonic.ProxyCore.config.ConfigManager;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class AnnouncementManager {

    private static final MinecraftChannelIdentifier ANNOUNCE_CHANNEL = MinecraftChannelIdentifier.create("dopamine",
            "announcement");

    private final ProxyServer proxy;
    private final ConfigManager configManager;
    private final MiniMessage miniMessage;
    private ScheduledTask task;
    private int messageIndex;

    public AnnouncementManager(ProxyServer proxy, ConfigManager configManager) {
        this.proxy = proxy;
        this.configManager = configManager;
        this.miniMessage = MiniMessage.miniMessage();
        this.messageIndex = 0;
    }

    public void start() {
        if (!configManager.isAnnouncementsEnabled()) {
            return;
        }
        if (task != null) {
            task.cancel();
        }
        int intervalSeconds = configManager.getAnnouncementIntervalSeconds();
        task = proxy.getScheduler().buildTask(this, this::broadcastAnnouncement)
                .repeat(intervalSeconds, TimeUnit.SECONDS)
                .schedule();
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void enable() {
        configManager.setAnnouncementsEnabled(true);
        start();
    }

    public void disable() {
        configManager.setAnnouncementsEnabled(false);
        stop();
    }

    public boolean isEnabled() {
        return configManager.isAnnouncementsEnabled();
    }

    public void reload() {
        configManager.load();
        stop();
        start();
    }

    public boolean isRunning() {
        return task != null;
    }

    private void broadcastAnnouncement() {
        List<String> messages = configManager.getAnnouncementMessages();
        if (messages.isEmpty()) {
            return;
        }

        String rawMessage = messages.get(messageIndex);
        messageIndex = (messageIndex + 1) % messages.size();

        Component formatted = miniMessage.deserialize(rawMessage);

        for (Player online : proxy.getAllPlayers()) {
            online.sendMessage(formatted);
        }

        byte[] pluginMessageData = serializeAnnouncement(rawMessage);
        for (RegisteredServer server : proxy.getAllServers()) {
            server.sendPluginMessage(ANNOUNCE_CHANNEL, pluginMessageData);
        }
    }

    private byte[] serializeAnnouncement(String message) {
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
