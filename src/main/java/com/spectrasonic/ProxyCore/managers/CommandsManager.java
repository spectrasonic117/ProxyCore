package com.spectrasonic.ProxyCore.managers;

import com.spectrasonic.ProxyCore.announce.AnnouncementManager;
import com.spectrasonic.ProxyCore.chat.StaffChatCommand;
import com.spectrasonic.ProxyCore.command.AnnounceCommand;
import com.spectrasonic.ProxyCore.command.BroadcastCommand;
import com.spectrasonic.ProxyCore.command.FindCommand;
import com.spectrasonic.ProxyCore.command.GotoCommand;
import com.spectrasonic.ProxyCore.command.LobbyCommand;
import com.spectrasonic.ProxyCore.command.MaintenanceCommand;
import com.spectrasonic.ProxyCore.command.MotdReloadCommand;
import com.spectrasonic.ProxyCore.config.ConfigManager;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.proxy.ProxyServer;

public class CommandsManager {

    private final ProxyServer proxy;
    private final ConfigManager configManager;
    private final Object plugin;
    private final AnnouncementManager announcementManager;
    private StaffChatCommand staffChatCommand;

    public CommandsManager(ProxyServer proxy, ConfigManager configManager, Object plugin,
            AnnouncementManager announcementManager) {
        this.proxy = proxy;
        this.configManager = configManager;
        this.plugin = plugin;
        this.announcementManager = announcementManager;
    }

    public void registerAll() {
        CommandManager commandManager = proxy.getCommandManager();

        LobbyCommand lobbyCommand = new LobbyCommand(proxy, configManager);
        for (String alias : new String[] { "lobby", "hub", "spawn", "leave" }) {
            commandManager.register(commandManager.metaBuilder(alias).build(), lobbyCommand);
        }

        staffChatCommand = new StaffChatCommand(proxy, configManager);
        commandManager.register(commandManager.metaBuilder("staffchat").plugin(plugin).build(), staffChatCommand);
        commandManager.register(commandManager.metaBuilder("sc").plugin(plugin).build(), staffChatCommand);

        FindCommand findCommand = new FindCommand(proxy);
        commandManager.register(commandManager.metaBuilder("find").plugin(plugin).build(), findCommand);

        GotoCommand gotoCommand = new GotoCommand(proxy);
        commandManager.register(commandManager.metaBuilder("goto").plugin(plugin).build(), gotoCommand);

        MaintenanceCommand maintenanceCommand = new MaintenanceCommand(proxy, configManager);
        commandManager.register(commandManager.metaBuilder("maintenance").plugin(plugin).build(), maintenanceCommand);

        MotdReloadCommand motdReloadCommand = new MotdReloadCommand(configManager);
        commandManager.register(commandManager.metaBuilder("motd").plugin(plugin).build(), motdReloadCommand);

        AnnounceCommand announceCommand = new AnnounceCommand(announcementManager);
        commandManager.register(commandManager.metaBuilder("announce").plugin(plugin).build(), announceCommand);

        BroadcastCommand broadcastCommand = new BroadcastCommand(proxy, configManager);
        commandManager.register(commandManager.metaBuilder("broadcast").plugin(plugin).build(), broadcastCommand);
        commandManager.register(commandManager.metaBuilder("br").plugin(plugin).build(), broadcastCommand);
    }

    public StaffChatCommand getStaffChatCommand() {
        return staffChatCommand;
    }
}
