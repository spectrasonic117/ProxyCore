package com.spectrasonic.ProxyCore.listener;

import com.spectrasonic.ProxyCore.config.ConfigManager;
import com.spectrasonic.ProxyCore.managers.SeenManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;

/**
 * Feeds {@link SeenManager} with connection events so {@code /seen} can report history for
 * players even after they leave the proxy.
 */
public class SeenListener {

    private final ConfigManager configManager;
    private final SeenManager seenManager;

    public SeenListener(ConfigManager configManager, SeenManager seenManager) {
        this.configManager = configManager;
        this.seenManager = seenManager;
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        if (!configManager.isSeenEnabled()) {
            return;
        }
        seenManager.onJoin(event.getPlayer());
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        // Always close the session of a player that is already tracked, even if tracking was
        // turned off mid-session, so the stored record does not end with a missing lastQuit.
        if (configManager.isSeenEnabled() || seenManager.hasRecord(event.getPlayer().getUniqueId())) {
            seenManager.onDisconnect(event.getPlayer());
        }
    }
}
