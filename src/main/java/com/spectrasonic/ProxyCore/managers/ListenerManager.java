package com.spectrasonic.ProxyCore.managers;

import com.spectrasonic.ProxyCore.config.ConfigManager;
import com.spectrasonic.ProxyCore.listener.MOTDListener;
import com.velocitypowered.api.proxy.ProxyServer;

public class ListenerManager {

    private final ProxyServer proxy;
    private final ConfigManager configManager;
    private final Object plugin;

    public ListenerManager(ProxyServer proxy, ConfigManager configManager, Object plugin) {
        this.proxy = proxy;
        this.configManager = configManager;
        this.plugin = plugin;
    }

    public void registerAll() {
        MOTDListener motdListener = new MOTDListener(proxy, configManager);
        proxy.getEventManager().register(plugin, motdListener);
    }
}
