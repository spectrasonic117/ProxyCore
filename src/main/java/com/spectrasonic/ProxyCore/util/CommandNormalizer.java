package com.spectrasonic.ProxyCore.util;

import java.util.Locale;

/**
 * Turns raw command input into the bare command name used for whitelist lookups.
 *
 * <p>Velocity hands over the command line without the leading slash, but players may prepend
 * whitespace and clients/plugins may namespace a command with a plugin prefix. Normalization makes
 * {@code gamemode}, {@code GAMEMODE}, {@code minecraft:gamemode} and {@code /gamemode creative} all
 * resolve to the same key: {@code gamemode}.
 */
public final class CommandNormalizer {

    private CommandNormalizer() {
        throw new AssertionError();
    }

    /**
     * Extracts the normalized command name from raw input.
     *
     * <p>Steps: trim surrounding whitespace, drop a leading slash, cut at the first whitespace to
     * discard arguments, lowercase, then drop any {@code namespace:} prefix.
     *
     * @param rawCommand the raw command line, with or without leading slash
     * @return the normalized command name, or an empty string when there is nothing to block
     */
    public static String normalize(String rawCommand) {
        if (rawCommand == null) {
            return "";
        }

        String trimmed = rawCommand.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1).trim();
        }
        if (trimmed.isEmpty()) {
            return "";
        }

        int separator = indexOfWhitespace(trimmed);
        String name = separator >= 0 ? trimmed.substring(0, separator) : trimmed;
        name = name.toLowerCase(Locale.ROOT);

        int colon = name.lastIndexOf(':');
        if (colon >= 0 && colon < name.length() - 1) {
            name = name.substring(colon + 1);
        }
        return name;
    }

    private static int indexOfWhitespace(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isWhitespace(value.charAt(i))) {
                return i;
            }
        }
        return -1;
    }
}
