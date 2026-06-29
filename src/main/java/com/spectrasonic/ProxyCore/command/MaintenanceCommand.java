package com.spectrasonic.ProxyCore.command;

import com.spectrasonic.ProxyCore.config.ConfigManager;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class MaintenanceCommand implements SimpleCommand {

    private final ProxyServer proxy;
    private final ConfigManager configManager;
    private final MiniMessage miniMessage;

    public MaintenanceCommand(ProxyServer proxy, ConfigManager configManager) {
        this.proxy = proxy;
        this.configManager = configManager;
        this.miniMessage = MiniMessage.miniMessage();
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length == 0) {
            boolean current = configManager.isMaintenance();
            if (current) {
                invocation.source().sendMessage(
                        miniMessage.deserialize(
                                "<yellow>⚠ Maintenance mode is currently <red>ENABLED</red>. Use <white>/maintenance off</white> to disable.</yellow>"));
            } else {
                invocation.source().sendMessage(
                        miniMessage.deserialize(
                                "<green>Maintenance mode is currently <aqua>DISABLED</aqua>. Use <white>/maintenance on</white> to enable.</green>"));
            }
            return;
        }

        String action = args[0].toLowerCase();
        if ("on".equals(action) || "enable".equals(action)) {
            configManager.setMaintenance(true);
            notifyStaff("<red>🔒 Maintenance mode has been <bold>ENABLED</bold>.</red>");
            invocation.source().sendMessage(miniMessage.deserialize("<green>✓ Maintenance mode enabled.</green>"));
        } else if ("off".equals(action) || "disable".equals(action)) {
            configManager.setMaintenance(false);
            notifyStaff("<green>🔓 Maintenance mode has been <bold>DISABLED</bold>.</green>");
            invocation.source().sendMessage(miniMessage.deserialize("<green>✓ Maintenance mode disabled.</green>"));
        } else {
            invocation.source().sendMessage(
                    miniMessage.deserialize("<red>Usage: /maintenance <on|off></red>"));
        }
    }

    private void notifyStaff(String message) {
        for (Player player : proxy.getAllPlayers()) {
            if (player.hasPermission("ProxyCore.maintenance")) {
                player.sendMessage(miniMessage.deserialize(message));
            }
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("ProxyCore.maintenance");
    }
}
