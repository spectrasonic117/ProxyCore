package com.spectrasonic.ProxyCore.listener;

import com.spectrasonic.ProxyCore.config.ConfigManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class MOTDListener {

    private static final int MAX_SAMPLE_PLAYERS = 10;
    private static final int MIN_MAX_PLAYERS = 100;

    private final ProxyServer proxy;
    private final ConfigManager configManager;
    private final MiniMessage miniMessage;

    public MOTDListener(ProxyServer proxy, ConfigManager configManager) {
        this.proxy = proxy;
        this.configManager = configManager;
        this.miniMessage = MiniMessage.miniMessage();
    }

    @Subscribe
    public void onProxyPing(ProxyPingEvent event) {
        // Custom MOTD disabled: leave Velocity's default ping untouched
        if (!configManager.isMotdEnabled()) {
            return;
        }

        ServerPing ping = event.getPing();
        ServerPing.Builder builder = ping.asBuilder();
        int onlinePlayers = proxy.getPlayerCount();
        int maxPlayers = Math.max(onlinePlayers + 1, MIN_MAX_PLAYERS);

        List<String> lines = configManager.isMaintenance()
                ? configManager.getMotdMaintenanceLines()
                : configManager.getMotdPlayers();

        builder.description(parseLines(lines));
        builder.onlinePlayers(onlinePlayers);
        builder.maximumPlayers(maxPlayers);

        if (configManager.isMaintenance()) {
            builder.clearSamplePlayers();
        } else {
            builder.clearSamplePlayers();
            builder.samplePlayers(buildSamplePlayers(onlinePlayers));
        }

        event.setPing(builder.build());
    }

    private Component parseLines(List<String> lines) {
        List<Component> components = new ArrayList<>(lines.size());
        for (String line : lines) {
            components.add(miniMessage.deserialize(line));
        }
        return Component.join(JoinConfiguration.newlines(), components);
    }

    private List<ServerPing.SamplePlayer> buildSamplePlayers(int onlinePlayers) {
        List<ServerPing.SamplePlayer> samplePlayers = new ArrayList<>();
        int count = Math.min(onlinePlayers, MAX_SAMPLE_PLAYERS);
        for (Player player : proxy.getAllPlayers()) {
            if (samplePlayers.size() >= count) {
                break;
            }
            samplePlayers.add(new ServerPing.SamplePlayer(player.getUsername(), player.getUniqueId()));
        }
        return samplePlayers;
    }
}
