package com.spectrasonic.ProxyCore.command;

import com.spectrasonic.ProxyCore.announce.AnnouncementManager;
import com.spectrasonic.ProxyCore.config.ConfigManager;
import com.spectrasonic.ProxyCore.util.MessageUtils;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import java.util.Locale;

/**
 * Root command for proxy-wide administration. Currently only supports {@code reload}, which
 * re-reads {@code config.yml} AND {@code messages.yml} (so every translation/message edit takes
 * effect without a proxy restart) and restarts the scheduled announcements so every module picks
 * up the new values.
 */
public class ProxyCoreCommand implements SimpleCommand {

    private static final String RELOAD_PERMISSION = "ProxyCore.reload";

    private final ConfigManager configManager;
    private final AnnouncementManager announcementManager;

    public ProxyCoreCommand(ConfigManager configManager, AnnouncementManager announcementManager) {
        this.configManager = configManager;
        this.announcementManager = announcementManager;
    }

    @Override
    public void execute(Invocation invocation) {
        // The proxy console is always allowed; players need the permission.
        if (invocation.source() instanceof Player
                && !invocation.source().hasPermission(RELOAD_PERMISSION)) {
            MessageUtils.sendNoPermission(invocation.source());
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
        if (!configManager.reloadAll()) {
            MessageUtils.sendMessage(invocation.source(), "messages.proxycore.reload_failed");
            return;
        }

        announcementManager.restart();

        MessageUtils.sendMessage(invocation.source(), "messages.proxycore.reload_success");
    }

    private void showUsage(Invocation invocation) {
        MessageUtils.sendMessage(invocation.source(), "messages.proxycore.usage");
    }
}