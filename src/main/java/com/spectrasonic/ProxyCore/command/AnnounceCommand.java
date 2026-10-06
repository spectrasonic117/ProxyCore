package com.spectrasonic.ProxyCore.command;

import com.spectrasonic.ProxyCore.announce.AnnouncementManager;
import com.spectrasonic.ProxyCore.util.MessageUtils;
import com.velocitypowered.api.command.SimpleCommand;

public class AnnounceCommand implements SimpleCommand {

    private final AnnouncementManager announcementManager;

    public AnnounceCommand(AnnouncementManager announcementManager) {
        this.announcementManager = announcementManager;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!invocation.source().hasPermission("ProxyCore.announce")) {
            MessageUtils.sendNoPermission(invocation.source());
            return;
        }

        String[] args = invocation.arguments();

        if (args.length == 0) {
            boolean running = announcementManager.isRunning();
            boolean enabled = announcementManager.isEnabled();
            if (running) {
                MessageUtils.sendMessage(invocation.source(), "messages.announce.status_running");
            } else if (enabled) {
                MessageUtils.sendMessage(invocation.source(), "messages.announce.status_enabled_not_running");
            } else {
                MessageUtils.sendMessage(invocation.source(), "messages.announce.status_disabled");
            }
            return;
        }

        String action = args[0].toLowerCase();
        if ("true".equals(action)) {
            announcementManager.enable();
            if (announcementManager.isRunning()) {
                MessageUtils.sendMessage(invocation.source(), "messages.announce.enabled_running");
            } else {
                MessageUtils.sendMessage(invocation.source(), "messages.announce.enabled");
            }
        } else if ("false".equals(action)) {
            announcementManager.disable();
            MessageUtils.sendMessage(invocation.source(), "messages.announce.disabled");
        } else if ("reload".equals(action)) {
            announcementManager.reload();
            boolean running = announcementManager.isRunning();
            if (running) {
                MessageUtils.sendMessage(invocation.source(), "messages.announce.reloaded_running");
            } else {
                MessageUtils.sendMessage(invocation.source(), "messages.announce.reloaded_disabled");
            }
        } else {
            MessageUtils.sendMessage(invocation.source(), "messages.announce.usage");
        }
    }
}