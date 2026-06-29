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
import org.slf4j.Logger;

import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;

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
        sendLogo();

        ConfigManager configManager = new ConfigManager(dataDirectory);
        configManager.load();
        logger.info("Target server: {}", configManager.getTargetServer());

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
            logger.info("{}", serializeToAnsi(component));
        }
    }

    private String serializeToAnsi(Component component) {
        StringBuilder sb = new StringBuilder();
        appendAnsi(component, sb);
        return sb.toString();
    }

    private void appendAnsi(Component component, StringBuilder sb) {
        Style style = component.style();
        TextColor color = style.color();
        boolean hasColor = color != null;

        if (hasColor) {
            sb.append("\033[").append(getAnsiColorCode(color)).append("m");
        }

        if (component instanceof TextComponent textComp) {
            sb.append(textComp.content());
        }

        for (Component child : component.children()) {
            appendAnsi(child, sb);
        }

        if (hasColor) {
            sb.append("\033[0m");
        }
    }

    private String getAnsiColorCode(TextColor color) {
        int red = color.red();
        int green = color.green();
        int blue = color.blue();
        if (red == 0 && green == 255 && blue == 255)
            return "36"; // aqua
        if (red == 255 && green == 255 && blue == 255)
            return "37"; // white
        if (red == 255 && green == 255 && blue == 0)
            return "33"; // yellow
        if (red == 255 && green == 0 && blue == 0)
            return "31"; // red
        if (red == 0 && green == 255 && blue == 0)
            return "32"; // green
        if (red == 255 && green == 170 && blue == 0)
            return "33"; // gold
        if (red == 0 && green == 0 && blue == 255)
            return "34"; // blue
        if (red == 170 && green == 0 && blue == 170)
            return "35"; // purple
        if (red == 255 && green == 170 && blue == 170)
            return "91"; // light red
        if (red == 170 && green == 170 && blue == 255)
            return "94"; // light blue
        if (red == 170 && green == 170 && blue == 170)
            return "90"; // gray
        return "37";
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
