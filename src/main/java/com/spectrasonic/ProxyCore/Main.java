package com.spectrasonic.ProxyCore;

import com.google.inject.Inject;
import com.spectrasonic.ProxyCore.announce.AnnouncementManager;
import com.spectrasonic.ProxyCore.config.ConfigManager;
import com.spectrasonic.ProxyCore.managers.CommandsManager;
import com.spectrasonic.ProxyCore.managers.ListenerManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import java.nio.file.Path;
import org.slf4j.Logger;

@Plugin(id = "proxycore", name = "ProxyCore", version = "${project.version}", description = "A Spectrasonic Proxy Plugin", url = "spectrasonic.xyz", authors = {
        "Spectrasonic" })
public class Main {

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;
    private AnnouncementManager announcementManager;

    @Inject
    public Main(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        ConfigManager configManager = new ConfigManager(dataDirectory);
        configManager.load();
        logger.info("ProxyCore config loaded. Target server: {}", configManager.getTargetServer());

        announcementManager = new AnnouncementManager(proxy, configManager);

        CommandsManager commandManager = new CommandsManager(proxy, configManager, this,
                announcementManager);
        commandManager.registerAll();

        ListenerManager listenerManager = new ListenerManager(
                proxy, configManager, this);
        listenerManager.registerAll();

        announcementManager.start();

        logger.info(
                "ProxyCore modules initialized: Lobby, StaffChat, Broadcast, PlayerFind, Goto, Maintenance, MOTD, Announcements");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (announcementManager != null) {
            announcementManager.stop();
        }
    }
}
