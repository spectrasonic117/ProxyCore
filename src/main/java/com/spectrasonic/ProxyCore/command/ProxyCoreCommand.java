package com.spectrasonic.ProxyCore.command;

import com.spectrasonic.ProxyCore.announce.AnnouncementManager;
import com.spectrasonic.ProxyCore.config.ConfigManager;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import java.util.Locale;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Root command for proxy-wide administration. Currently only supports {@code reload}, which
 * re-reads {@code config.yml} and restarts the scheduled announcements so every module picks up
 * the new values without a proxy restart.
 */
public class ProxyCoreCommand implements SimpleCommand {

    private static final String RELOAD_PERMISSION = "ProxyCore.reload";

    private final ConfigManager configManager;
    private final AnnouncementManager announcementManager;
    private final MiniMessage miniMessage;

    public ProxyCoreCommand(ConfigManager configManager, AnnouncementManager announcementManager) {
        this.configManager = configManager;
        this.announcementManager = announcementManager;
        this.miniMessage = MiniMessage.miniMessage();
    }

    @Override
    public void execute(Invocation invocation) {
        // The proxy console is always allowed; players need the permission.
        if (invocation.source() instanceof Player
                && !invocation.source().hasPermission(RELOAD_PERMISSION)) {
            invocation.source().sendMessage(
                    miniMessage.deserialize("<red>You don't have permission to use this command.</red>"));
            return;
        }

        String[] args = invocation.arguments();
        if (args.length == 0) {
            showUsage(invocation);
            return;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        if ("reload".equals(action)) {
            reload(invocation);
        } else {
            showUsage(invocation);
        }
    }

    private void reload(Invocation invocation) {
        if (!configManager.reload()) {
            invocation.source().sendMessage(miniMessage.deserialize(
                    "<red>✗ Could not parse config.yml. Check the console and fix the syntax; "
                            + "the previous configuration is still active.</red>"));
            return;
        }

        announcementManager.restart();

        invocation.source().sendMessage(miniMessage.deserialize(
                "<green>✓ ProxyCore configuration reloaded (MOTD, announcements, chat formats and "
                        + "command blocker).</green>"));
    }

    private void showUsage(Invocation invocation) {
        invocation.source().sendMessage(
                miniMessage.deserialize("<gray>Usage: <white>/proxycore reload</white></gray>"));
    }
}
