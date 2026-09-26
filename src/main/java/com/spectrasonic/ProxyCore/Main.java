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
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer;
import org.slf4j.Logger;

@Plugin(id = "proxycore", name = "ProxyCore", version = "${project.version}", description = "A Spectrasonic Proxy Plugin", url = "spectrasonic.xyz", authors = {
        "Spectrasonic" })
public class Main {

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;
    private AnnouncementManager announcementManager;

    /**
     * Serializes {@link Component}s to ANSI escape codes for the console. Bundled with
     * Velocity ({@code adventure-text-serializer-ansi}); auto-detects the terminal color
     * level ({@code ColorLevel.compute()}) and emits plain text when ANSI is unsupported,
     * so headless consoles never receive raw escape sequences.
     */
    private static final ANSIComponentSerializer ANSI_SERIALIZER = ANSIComponentSerializer.ansi();

    @Inject
    public Main(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        sendLogo();

        ConfigManager configManager = new ConfigManager(dataDirectory);
        configManager.load();
        logger.info("Target server: {}", configManager.getTargetServer());

        announcementManager = new AnnouncementManager(proxy, configManager);

        CommandsManager commandManager = new CommandsManager(proxy, configManager, this,
                announcementManager);
        commandManager.registerAll();

        ListenerManager listenerManager = new ListenerManager(
                proxy, configManager, this, logger);
        listenerManager.registerAll();

        announcementManager.start();

        logger.info(
                "ProxyCore modules initialized: Lobby, StaffChat, Broadcast, PlayerFind, Goto, Maintenance, MOTD, Announcements, CommandBlocker");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (announcementManager != null) {
            announcementManager.stop();
        }
    }

    private void sendLogo() {
        String version = getVersion();

        MiniMessage miniMessage = MiniMessage.miniMessage();

        String[] lines = {
                "",
                "<aqua>▄▖▄▖▄▖▖▖▖▖  ▄▖▄▖▄▖▄▖ <reset><white>Version: <yellow>" + version + "<reset>",
                "<aqua>▙▌▙▘▌▌▚▘▌▌  ▌ ▌▌▙▘▙▖ <reset><white>Author: <red>Spectrasonic<reset>",
                "<aqua>▌ ▌▌▙▌▌▌▐   ▙▖▙▌▌▌▙▖ <reset><green>Plugin Enabled Successfully!<reset>",
                ""
        };

        for (String line : lines) {
            Component component = miniMessage.deserialize(line);
            logger.info("{}", ANSI_SERIALIZER.serialize(component));
        }
    }

    private String getVersion() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("version.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                return props.getProperty("version", "unknown");
            }
        } catch (Exception e) {
            logger.warn("Could not load version from properties", e);
        }
        return "unknown";
    }
}
