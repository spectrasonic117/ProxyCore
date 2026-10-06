package com.spectrasonic.ProxyCore.command;

import com.spectrasonic.ProxyCore.config.ConfigManager;
import com.spectrasonic.ProxyCore.util.MessageUtils;
import com.velocitypowered.api.command.SimpleCommand;

public class MotdReloadCommand implements SimpleCommand {

    private final ConfigManager configManager;

    public MotdReloadCommand(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!invocation.source().hasPermission("ProxyCore.motd")) {
            MessageUtils.sendNoPermission(invocation.source());
            return;
        }

        String[] args = invocation.arguments();
        if (args.length == 0 || !"reload".equalsIgnoreCase(args[0])) {
            MessageUtils.sendMessage(invocation.source(), "messages.motd.usage");
            return;
        }
        configManager.load();
        MessageUtils.sendMessage(invocation.source(), "messages.motd.reloaded");
    }
}