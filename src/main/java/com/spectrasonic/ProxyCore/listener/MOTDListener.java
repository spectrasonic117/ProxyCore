package com.spectrasonic.ProxyCore.listener;

import com.spectrasonic.ProxyCore.config.ConfigManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

@SuppressWarnings("deprecation")
public class MOTDListener {

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
        ServerPing ping = event.getPing();
        ServerPing.Builder builder = ping.asBuilder();
        int onlinePlayers = proxy.getPlayerCount();
        int maxPlayers = Math.max(onlinePlayers + 1, 100);

        if (configManager.isMaintenance()) {
            List<Component> maintenanceLines = new ArrayList<>();
            for (String line : configManager.getMotdMaintenanceLines()) {
                maintenanceLines.add(miniMessage.deserialize(line));
            }
            builder.description(Component.join(Component.newline(), maintenanceLines));
            builder.onlinePlayers(onlinePlayers);
            builder.maximumPlayers(maxPlayers);
            builder.clearSamplePlayers();
            event.setPing(builder.build());
            return;
        }

        List<Component> motdLines = new ArrayList<>();
        for (String line : configManager.getMotdPlayers()) {
            motdLines.add(miniMessage.deserialize(line));
        }
        Component description = Component.join(Component.newline(), motdLines);
        builder.description(description);

        builder.onlinePlayers(onlinePlayers);
        builder.maximumPlayers(maxPlayers);

        builder.clearSamplePlayers();
        List<ServerPing.SamplePlayer> samplePlayers = new ArrayList<>();
        int count = Math.min(onlinePlayers, 10);
        int added = 0;
        for (var player : proxy.getAllPlayers()) {
            if (added >= count)
                break;
            samplePlayers.add(new ServerPing.SamplePlayer(player.getUsername(), UUID.randomUUID()));
            added++;
        }
        builder.samplePlayers(samplePlayers.toArray(new ServerPing.SamplePlayer[0]));

        event.setPing(builder.build());
    }
}
