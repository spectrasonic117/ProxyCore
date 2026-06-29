package com.spectrasonic.ProxyCore.command;

import com.spectrasonic.ProxyCore.announce.AnnouncementManager;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class AnnounceCommand implements SimpleCommand {

    private final AnnouncementManager announcementManager;
    private final MiniMessage miniMessage;

    public AnnounceCommand(AnnouncementManager announcementManager) {
        this.announcementManager = announcementManager;
        this.miniMessage = MiniMessage.miniMessage();
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length == 0) {
            boolean running = announcementManager.isRunning();
            boolean enabled = announcementManager.isEnabled();
            if (running) {
                invocation.source().sendMessage(
                        miniMessage.deserialize(
                                "<green>Announcements: <aqua>running</aqua>. Use <white>/announce false</white> to disable.</green>"));
            } else if (enabled) {
                invocation.source().sendMessage(
                        miniMessage.deserialize(
                                "<yellow>Announcements: <gold>enabled but not running</gold>. Use <white>/announce true</white> to start.</yellow>"));
            } else {
                invocation.source().sendMessage(
                        miniMessage.deserialize(
                                "<red>Announcements: <dark_red>disabled</dark_red>. Use <white>/announce true</white> to enable.</red>"));
            }
            return;
        }

        String action = args[0].toLowerCase();
        if ("true".equals(action)) {
            announcementManager.enable();
            if (announcementManager.isRunning()) {
                invocation.source().sendMessage(
                        miniMessage.deserialize("<green>✓ Announcements <bold>enabled</bold> and running.</green>"));
            } else {
                invocation.source().sendMessage(
                        miniMessage.deserialize("<green>✓ Announcements <bold>enabled</bold>.</green>"));
            }
        } else if ("false".equals(action)) {
            announcementManager.disable();
            invocation.source().sendMessage(
                    miniMessage.deserialize("<green>✓ Announcements <bold>disabled</bold>.</green>"));
        } else if ("reload".equals(action)) {
            announcementManager.reload();
            boolean running = announcementManager.isRunning();
            if (running) {
                invocation.source().sendMessage(
                        miniMessage
                                .deserialize("<green>✓ Announcements configuration reloaded and restarted.</green>"));
            } else {
                invocation.source().sendMessage(
                        miniMessage.deserialize(
                                "<green>✓ Announcements configuration reloaded. Currently disabled.</green>"));
            }
        } else {
            invocation.source().sendMessage(
                    miniMessage.deserialize("<red>Usage: /announce <true|false|reload></red>"));
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("ProxyCore.announce");
    }
}
