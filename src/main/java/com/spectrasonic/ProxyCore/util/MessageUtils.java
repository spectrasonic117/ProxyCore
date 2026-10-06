package com.spectrasonic.ProxyCore.util;

import com.spectrasonic.ProxyCore.managers.MessageManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Centralised send helpers. Every command-facing string in the plugin must go
 * through this class
 * so prefixes stay aligned and {@link MiniMessage} deserialisation lives in one
 * place.
 *
 * <p>
 * Two layers:
 * <ul>
 * <li>{@link #rawMessage(Audience, String)} /
 * {@code success/alert/deny/warning/infoMessage}
 * resolve a MiniMessage string and send it directly to an {@link Audience}. Use
 * the
 * prefixed variants when the message comes from {@code messages.yml}; the
 * prefix and the
 * close-tag together bracket the body so colours from the message body do not
 * leak into
 * later chat lines.</li>
 * <li>The static {@code send*} helpers in the second section combine
 * {@link MessageManager}
 * with the prefixed senders, so a typical command reads as a single line:
 * {@code MessageUtils.denyMessage(sender, MessageManager.getMessage("messages.lobby.already_connected", "server", serverName))}.</li>
 * </ul>
 */
public final class MessageUtils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    /**
     * Close-tag used after every prefixed body so the prefix colour does not leak
     * into chat.
     */
    private static final String CLOSE_GRAY = "</gray>";

    public static final String SUCCESS_PREFIX = "<green><bold>[✔]</bold></green> <gray>";
    public static final String ALERT_PREFIX = "<yellow><bold>[!]</bold></yellow> <gray>";
    public static final String DENY_PREFIX = "<red><bold>[✖]</bold></red> <gray>";
    public static final String WARNING_PREFIX = "<red><bold>[⚠]</bold></red> <gray>";
    public static final String INFO_PREFIX = "<aqua><bold>[i]</bold></aqua> <gray>";

    private MessageUtils() {
    }

    // -----------------------------------------------------------------
    // Raw senders (caller passes a fully-built MiniMessage string)
    // -----------------------------------------------------------------

    public static void rawMessage(Audience audience, String message) {
        audience.sendMessage(MINI_MESSAGE.deserialize(message));
    }

    public static void successMessage(Audience audience, String message) {
        audience.sendMessage(MINI_MESSAGE.deserialize(SUCCESS_PREFIX + message + CLOSE_GRAY));
    }

    public static void alertMessage(Audience audience, String message) {
        audience.sendMessage(MINI_MESSAGE.deserialize(ALERT_PREFIX + message + CLOSE_GRAY));
    }

    public static void denyMessage(Audience audience, String message) {
        audience.sendMessage(MINI_MESSAGE.deserialize(DENY_PREFIX + message + CLOSE_GRAY));
    }

    public static void warningMessage(Audience audience, String message) {
        audience.sendMessage(MINI_MESSAGE.deserialize(WARNING_PREFIX + message + CLOSE_GRAY));
    }

    public static void infoMessage(Audience audience, String message) {
        audience.sendMessage(MINI_MESSAGE.deserialize(INFO_PREFIX + message + CLOSE_GRAY));
    }

    // -----------------------------------------------------------------
    // Convenience helpers that combine MessageManager + senders
    // -----------------------------------------------------------------

    public static void sendMessage(Audience audience, String key, String... replacements) {
        rawMessage(audience, MessageManager.getMessage(key, replacements));
    }

    public static void sendSuccessMessage(Audience audience, String key, String... replacements) {
        successMessage(audience, MessageManager.getMessage(key, replacements));
    }

    public static void sendAlertMessage(Audience audience, String key, String... replacements) {
        alertMessage(audience, MessageManager.getMessage(key, replacements));
    }

    public static void sendDenyMessage(Audience audience, String key, String... replacements) {
        denyMessage(audience, MessageManager.getMessage(key, replacements));
    }

    public static void sendWarningMessage(Audience audience, String key, String... replacements) {
        warningMessage(audience, MessageManager.getMessage(key, replacements));
    }

    public static void sendInfoMessage(Audience audience, String key, String... replacements) {
        infoMessage(audience, MessageManager.getMessage(key, replacements));
    }

    // -----------------------------------------------------------------
    // Common shortcuts - saves a few characters per call site
    // -----------------------------------------------------------------

    public static void sendNoPermission(Audience audience) {
        denyMessage(audience, MessageManager.getMessage("messages.common.no_permission"));
    }

    public static void sendOnlyPlayers(Audience audience) {
        denyMessage(audience, MessageManager.getMessage("messages.common.only_players"));
    }

    public static void sendPlayerNotFound(Audience audience, String player) {
        denyMessage(audience, MessageManager.getMessage("messages.common.player_not_found", "player", player));
    }
}