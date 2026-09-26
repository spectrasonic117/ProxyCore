package com.spectrasonic.ProxyCore.listener;

import com.spectrasonic.ProxyCore.config.ConfigManager;
import com.spectrasonic.ProxyCore.util.CommandNormalizer;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.player.TabCompleteEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;

/**
 * Proxy-side command blocker driven by a strict whitelist.
 *
 * <p>Every command a player runs is checked against {@code command-blocker.whitelist} before it can
 * reach the proxy handler or be forwarded to a backend server. Matching is done on the bare command
 * name (see {@link CommandNormalizer}), so listing {@code gamemode} covers
 * {@code minecraft:gamemode}, {@code GAMEMODE} and {@code /gamemode creative} alike.
 *
 * <p>Both hooks run in the command hot path: they only read in-memory config and build short
 * strings, never touching disk.
 */
public class CommandBlockerListener {

    /** Grants a full bypass of the whitelist. */
    public static final String BYPASS_PERMISSION = "ProxyCore.commandblocker.bypass";

    private static final String UNKNOWN_SERVER = "unknown";

    private final ProxyServer proxy;
    private final ConfigManager configManager;
    private final Logger logger;
    private final MiniMessage miniMessage;

    public CommandBlockerListener(ProxyServer proxy, ConfigManager configManager, Logger logger) {
        this.proxy = proxy;
        this.configManager = configManager;
        this.logger = logger;
        this.miniMessage = MiniMessage.miniMessage();
    }

    @Subscribe
    public void onCommandExecute(CommandExecuteEvent event) {
        if (!configManager.isCommandBlockerEnabled()) {
            return;
        }
        // The console and any other non-player source is never restricted.
        if (!(event.getCommandSource() instanceof Player player)) {
            return;
        }
        if (player.hasPermission(BYPASS_PERMISSION)) {
            return;
        }

        String rawCommand = event.getCommand();
        String command = CommandNormalizer.normalize(rawCommand);
        if (command.isEmpty() || isAllowed(player, command)) {
            return;
        }

        // Denying stops the command here: it is neither handled by the proxy nor forwarded.
        event.setResult(CommandExecuteEvent.CommandResult.denied());

        String server = getServerName(player);
        String template = configManager.getCommandBlockerDenyMessage()
                .replace("{player}", player.getUsername())
                .replace("{command}", command)
                .replace("{server}", server);
        player.sendMessage(miniMessage.deserialize(template));

        if (configManager.isCommandBlockerLoggingEnabled()) {
            String logEntry = configManager.getCommandBlockerLogFormat()
                    .replace("{player}", player.getUsername())
                    .replace("{server}", server)
                    .replace("{command}", command)
                    .replace("{raw}", rawCommand);
            logger.info("{}", logEntry);
        }
    }

    /**
     * Hides tab-complete suggestions for commands the player may not run.
     *
     * <p>Only applied to inputs that actually start a command, so block and player name
     * completions are left untouched.
     */
    @Subscribe
    public void onTabComplete(TabCompleteEvent event) {
        if (!configManager.isCommandBlockerEnabled()
                || !configManager.isCommandBlockerTabCompleteBlocked()) {
            return;
        }
        String partialMessage = event.getPartialMessage();
        if (partialMessage == null || !partialMessage.trim().startsWith("/")) {
            return;
        }

        Player player = event.getPlayer();
        if (player.hasPermission(BYPASS_PERMISSION)) {
            return;
        }

        String command = CommandNormalizer.normalize(partialMessage);
        if (command.isEmpty() || isAllowed(player, command)) {
            return;
        }
        event.getSuggestions().clear();
    }

    private boolean isAllowed(Player player, String command) {
        if (configManager.getCommandBlockerWhitelist().contains(command)) {
            return true;
        }
        // Safety net: never lock admins out of the commands the proxy itself handles.
        return configManager.isCommandBlockerAlwaysAllowProxyCommands()
                && proxy.getCommandManager().hasCommand(command, player);
    }

    private String getServerName(Player player) {
        return player.getCurrentServer()
                .map(connection -> connection.getServerInfo().getName())
                .orElse(UNKNOWN_SERVER);
    }
}
