package com.spectrasonic.ProxyCore.util;

import com.spectrasonic.ProxyCore.managers.MessageManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Centralised send helpers. Every command-facing string in the plugin must go
 * through this class so {@link MiniMessage} deserialisation lives in one place.
 *
 * <p>
 * Two layers:
 * <ul>
 * <li>{@link #rawMessage(Audience, String)} resolves a MiniMessage string and
 * sends it directly to an {@link Audience}. No prefix is added: the styling is
 * owned entirely by {@code messages.yml}.</li>
 * <li>{@link #sendMessage(Audience, String, String...)} combines
 * {@link MessageManager} with {@link #rawMessage(Audience, String)}, so a
 * typical command reads as a single line:
 * {@code MessageUtils.sendMessage(sender, "messages.lobby.already_connected", "server", serverName)}.</li>
 * </ul>
 *
 * <p>
 * Prefix helpers were removed on purpose - the plugin no longer prepends chat
 * tags like {@code [✔]} or {@code [✖]} to any message. Add the desired tags (if
 * any) directly in {@code messages.yml}.
 */
public final class MessageUtils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private MessageUtils() {
    }

    // -----------------------------------------------------------------
    // Raw senders (caller passes a fully-built MiniMessage string)
    // -----------------------------------------------------------------

    public static void rawMessage(Audience audience, String message) {
        audience.sendMessage(MINI_MESSAGE.deserialize(message));
    }

    // -----------------------------------------------------------------
    // Convenience helpers that combine MessageManager + sender
    // -----------------------------------------------------------------

    public static void sendMessage(Audience audience, String key, String... replacements) {
        rawMessage(audience, MessageManager.getMessage(key, replacements));
    }

    // -----------------------------------------------------------------
    // Common shortcuts - saves a few characters per call site
    // -----------------------------------------------------------------

    public static void sendNoPermission(Audience audience) {
        sendMessage(audience, "messages.common.no_permission");
    }

    public static void sendOnlyPlayers(Audience audience) {
        sendMessage(audience, "messages.common.only_players");
    }

    public static void sendPlayerNotFound(Audience audience, String player) {
        sendMessage(audience, "messages.common.player_not_found", "player", player);
    }
}
