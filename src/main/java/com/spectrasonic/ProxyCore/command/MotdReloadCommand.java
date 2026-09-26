package com.spectrasonic.ProxyCore.command;

import com.spectrasonic.ProxyCore.config.ConfigManager;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class MotdReloadCommand implements SimpleCommand {

    private final ConfigManager configManager;
    private final MiniMessage miniMessage;

    public MotdReloadCommand(ConfigManager configManager) {
        this.configManager = configManager;
        this.miniMessage = MiniMessage.miniMessage();
    }

    @Override
    public void execute(Invocation invocation) {
        if (!invocation.source().hasPermission("ProxyCore.motd")) {
            invocation.source().sendMessage(
                    miniMessage.deserialize("<red>You don't have permission to use this command.</red>"));
            return;
        }

        String[] args = invocation.arguments();
        if (args.length == 0 || !"reload".equalsIgnoreCase(args[0])) {
            invocation.source().sendMessage(
                    miniMessage.deserialize("<red>Usage: /motd reload</red>"));
            return;
        }
        configManager.load();
        invocation.source().sendMessage(
                miniMessage.deserialize("<green>✓ MOTD configuration reloaded from config.yml.</green>"));
    }
}
