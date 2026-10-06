package com.spectrasonic.ProxyCore.command;

import com.spectrasonic.ProxyCore.config.ConfigManager;
import com.spectrasonic.ProxyCore.managers.MessageManager;
import com.spectrasonic.ProxyCore.util.MessageUtils;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

public class MaintenanceCommand implements SimpleCommand {

    private final ProxyServer proxy;
    private final ConfigManager configManager;

    public MaintenanceCommand(ProxyServer proxy, ConfigManager configManager) {
        this.proxy = proxy;
        this.configManager = configManager;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!invocation.source().hasPermission("ProxyCore.maintenance")) {
            MessageUtils.sendNoPermission(invocation.source());
            return;
        }

        String[] args = invocation.arguments();

        if (args.length == 0) {
            boolean current = configManager.isMaintenance();
            if (current) {
                MessageUtils.sendMessage(invocation.source(), "messages.maintenance.status_enabled");
            } else {
                MessageUtils.sendMessage(invocation.source(), "messages.maintenance.status_disabled");
            }
            return;
        }

        String action = args[0].toLowerCase();
        if ("on".equals(action) || "enable".equals(action)) {
            configManager.setMaintenance(true);
            notifyStaff(MessageManager.getMessage("messages.maintenance.notify_enabled"));
            MessageUtils.sendMessage(invocation.source(), "messages.maintenance.enabled");
        } else if ("off".equals(action) || "disable".equals(action)) {
            configManager.setMaintenance(false);
            notifyStaff(MessageManager.getMessage("messages.maintenance.notify_disabled"));
            MessageUtils.sendMessage(invocation.source(), "messages.maintenance.disabled");
        } else {
            MessageUtils.sendMessage(invocation.source(), "messages.maintenance.usage");
        }
    }

    private void notifyStaff(String message) {
        for (Player player : proxy.getAllPlayers()) {
            if (player.hasPermission("ProxyCore.maintenance")) {
                MessageUtils.warningMessage(player, message);
            }
        }
    }
}