package com.spectrasonic.ProxyCore.managers;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * Loads {@code messages.yml} from the plugin data folder and exposes every message by a dotted
 * key like {@code messages.lobby.already_connected}.
 *
 * <p>This is the Velocity port of the project's Bukkit-style messages pattern: the file is copied
 * from the bundled classpath resource on first run, parsed with SnakeYAML (the same library
 * already used by {@link ConfigManager}), and held in memory as a nested map.
 *
 * <p>Keys are dot-separated and walked top-down through the nested map, so a value at
 * {@code messages.lobby.already_connected} is resolved by following
 * {@code messages → lobby → already_connected}. Missing keys return a red
 * {@code "<Message not found: KEY>"} so they surface immediately in chat instead of silently
 * sending a blank line.
 *
 * <p>Placeholders use the {@code {name}} syntax (matching {@code command-blocker.deny-message}
 * and the rest of the project) and are filled in by passing alternating name/value pairs to
 * {@link #getMessage(String, String...)}. {@code MiniMessage} tags are NOT replaced - only
 * {@code {name}} placeholders are, so the message author stays in control of the styling.
 *
 * <p>The manager is a singleton: {@link #init(Path, Logger)} runs once from
 * {@link ConfigManager}'s constructor, and {@link #reload()} re-reads the file while preserving
 * the previous in-memory copy when the file is missing or corrupt (mirrors the resilience
 * pattern already used by {@link ConfigManager#reload()}).
 */
public final class MessageManager {

    /** Default mini-message returned when a key resolves to nothing - red so they are obvious. */
    private static final String FALLBACK_FORMAT = "<red>Message not found: %s</red>";

    /** Returned when {@link #getInstance()} is called before {@link #init(Path, Logger)}. */
    private static final String NOT_INITIALIZED_FORMAT = "<red>MessageManager not initialized. Missing: %s</red>";

    private static MessageManager instance;

    private final Logger logger;
    private final Path messagesPath;
    private final Yaml yaml;
    private Map<String, Object> messages;

    private MessageManager(Path dataDirectory, Logger logger) {
        this.logger = logger;
        this.messagesPath = dataDirectory.resolve("messages.yml");
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        this.yaml = new Yaml(options);
        loadMessages();
    }

    /**
     * Initializes the singleton. Idempotent: subsequent calls return the existing instance, so
     * calling it from {@code ConfigManager}'s constructor and from a reload command is safe.
     */
    public static synchronized MessageManager init(Path dataDirectory, Logger logger) {
        if (instance == null) {
            instance = new MessageManager(dataDirectory, logger);
        }
        return instance;
    }

    public static MessageManager getInstance() {
        return instance;
    }

    /**
     * Re-reads {@code messages.yml} from disk.
     *
     * <p>If the file is missing or corrupt, the previous in-memory copy is kept and the method
     * returns {@code false} so the caller can warn the user (mirrors {@link ConfigManager#reload()}).
     */
    public boolean reload() {
        Map<String, Object> previous = this.messages;
        try {
            loadMessages();
            return true;
        } catch (RuntimeException e) {
            this.messages = previous != null ? previous : createMinimalDefaults();
            logger.warn("Failed to reload messages.yml, keeping previous values: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Loads {@code messages.yml}, copying the bundled default resource if the file is missing on
     * disk. Errors during read are logged and fall back to {@link #createMinimalDefaults()} so the
     * plugin still boots even with a totally broken file.
     */
    private void loadMessages() {
        if (!Files.exists(messagesPath)) {
            copyDefaultMessages();
            return;
        }
        try (InputStreamReader reader = new InputStreamReader(
                new FileInputStream(messagesPath.toFile()), StandardCharsets.UTF_8)) {
            Map<String, Object> loaded = yaml.load(reader);
            messages = loaded instanceof Map ? castToMap(loaded) : createMinimalDefaults();
        } catch (IOException e) {
            logger.warn("Could not read messages.yml ({}), using minimal defaults", e.getMessage());
            messages = createMinimalDefaults();
        }
    }

    /**
     * Copies {@code messages.yml} from the classpath resource bundle to the plugin data folder,
     * then loads it. Falls back to programmatic defaults if the resource is missing (which would
     * be a packaging bug, not a runtime one).
     */
    private void copyDefaultMessages() {
        try {
            Files.createDirectories(messagesPath.getParent());
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("messages.yml")) {
                if (in != null) {
                    Files.copy(in, messagesPath);
                    logger.info("Created default messages.yml at {}", messagesPath);
                    try (InputStreamReader reader = new InputStreamReader(
                            new FileInputStream(messagesPath.toFile()), StandardCharsets.UTF_8)) {
                        Map<String, Object> loaded = yaml.load(reader);
                        messages = loaded != null ? loaded : createMinimalDefaults();
                    }
                    return;
                }
            }
        } catch (IOException e) {
            logger.warn("Could not copy bundled messages.yml: {}", e.getMessage());
        }
        messages = createMinimalDefaults();
    }

    /**
     * Saves the current in-memory messages back to disk. Useful for the {@code save} step of a
     * future in-game message editor; currently unused but kept so the manager is self-sufficient.
     */
    public void save() {
        try {
            Files.createDirectories(messagesPath.getParent());
            try (OutputStreamWriter writer = new OutputStreamWriter(
                    new FileOutputStream(messagesPath.toFile()), StandardCharsets.UTF_8)) {
                yaml.dump(messages, writer);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save messages.yml", e);
        }
    }

    /**
     * Returns the raw MiniMessage string for the given key, with no placeholders filled in.
     *
     * @see #getMessage(String, String...)
     */
    public static String getMessage(String key) {
        return getMessage(key, new String[0]);
    }

    /**
     * Returns the MiniMessage string for the given key with {@code {placeholder}} tokens
     * resolved. Pass placeholder names and values in alternating order:
     *
     * <pre>{@code
     * MessageManager.getMessage("messages.lobby.already_connected", "server", "lobby");
     * }</pre>
     *
     * <p>If the key is missing, a red {@code "<Message not found: KEY>"} is returned so the
     * broken entry is obvious in chat instead of silently blank.
     */
    public static String getMessage(String key, String... replacements) {
        if (instance == null) {
            return String.format(NOT_INITIALIZED_FORMAT, key);
        }
        String value = instance.resolveKey(key);
        if (value == null) {
            return String.format(FALLBACK_FORMAT, key);
        }
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            value = value.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        return value;
    }

    /**
     * Resolves a dotted key by walking the nested map. Returns {@code null} when any segment is
     * missing or the leaf is not a string-like value.
     */
    @SuppressWarnings("unchecked")
    private String resolveKey(String key) {
        if (messages == null || key == null || key.isEmpty()) {
            return null;
        }
        String[] parts = key.split("\\.");
        Object current = messages;
        for (String part : parts) {
            if (!(current instanceof Map)) {
                return null;
            }
            current = ((Map<String, Object>) current).get(part);
            if (current == null) {
                return null;
            }
        }
        return current.toString();
    }

    /**
     * Last-resort fallback so the plugin still has *something* to send if the file is missing,
     * corrupt, or the bundled resource itself fails to load. Mirrors the same defensive pattern
     * used by {@link ConfigManager#createDefaults()}.
     */
    private Map<String, Object> createMinimalDefaults() {
        Map<String, Object> defaults = new LinkedHashMap<>();
        Map<String, Object> common = new LinkedHashMap<>();
        common.put("no_permission", "<red>You don't have permission to use this command.</red>");
        common.put("only_players", "<red>This command can only be executed by a player.</red>");
        defaults.put("common", common);
        Map<String, Object> proxycore = new LinkedHashMap<>();
        proxycore.put("reload_success", "<green>✓ ProxyCore reloaded.</green>");
        defaults.put("proxycore", proxycore);
        return defaults;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castToMap(Object value) {
        return (Map<String, Object>) value;
    }
}