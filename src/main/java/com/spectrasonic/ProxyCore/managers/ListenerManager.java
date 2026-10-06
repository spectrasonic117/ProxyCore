package com.spectrasonic.ProxyCore.managers;

import com.spectrasonic.ProxyCore.config.ConfigManager;
import com.spectrasonic.ProxyCore.listener.CommandBlockerListener;
import com.spectrasonic.ProxyCore.listener.MOTDListener;
import com.spectrasonic.ProxyCore.listener.SeenListener;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

public class ListenerManager {

    private final ProxyServer proxy;
    private final ConfigManager configManager;
    private final Object plugin;
    private final Logger logger;
    private final SeenManager seenManager;

    public ListenerManager(ProxyServer proxy, ConfigManager configManager, Object plugin, Logger logger,
            SeenManager seenManager) {
        this.proxy = proxy;
        this.configManager = configManager;
        this.plugin = plugin;
        this.logger = logger;
        this.seenManager = seenManager;
    }

    public void registerAll() {
        MOTDListener motdListener = new MOTDListener(proxy, configManager);
        proxy.getEventManager().register(plugin, motdListener);

        CommandBlockerListener commandBlockerListener = new CommandBlockerListener(proxy, configManager, logger);
        proxy.getEventManager().register(plugin, commandBlockerListener);

        SeenListener seenListener = new SeenListener(configManager, seenManager);
        proxy.getEventManager().register(plugin, seenListener);
    }
}
