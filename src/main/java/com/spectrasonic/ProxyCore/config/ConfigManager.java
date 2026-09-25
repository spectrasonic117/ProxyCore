package com.spectrasonic.ProxyCore.config;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class ConfigManager {

    private final Path configPath;
    private final Yaml yaml;
    private Map<String, Object> config;

    public ConfigManager(Path dataDirectory) {
        this.configPath = dataDirectory.resolve("config.yml");
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        this.yaml = new Yaml(options);
    }

    public void load() {
        if (!Files.exists(configPath)) {
            copyDefaultConfig();
            return;
        }
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(configPath.toFile()),
                StandardCharsets.UTF_8)) {
            Map<String, Object> loaded = yaml.load(reader);
            config = loaded != null ? loaded : createDefaults();
        } catch (IOException e) {
            config = createDefaults();
        }
    }

    private void copyDefaultConfig() {
        try {
            Files.createDirectories(configPath.getParent());
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.yml")) {
                if (in != null) {
                    Files.copy(in, configPath);
                    // After copying, load the config from the file
                    try (InputStreamReader reader = new InputStreamReader(new FileInputStream(configPath.toFile()),
                            StandardCharsets.UTF_8)) {
                        Map<String, Object> loaded = yaml.load(reader);
                        config = loaded != null ? loaded : createDefaults();
                    }
                    return;
                }
            }
        } catch (IOException e) {
            // If anything fails, fall back to programmatic defaults
        }
        config = createDefaults();
        save();
    }

    public void save() {
        try {
            Files.createDirectories(configPath.getParent());
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(configPath.toFile()),
                    StandardCharsets.UTF_8)) {
                yaml.dump(config, writer);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save config", e);
        }
    }

    public String getTargetServer() {
        Object value = config.get("target-server");
        return value != null ? value.toString() : "lobby";
    }

    // --- MOTD ---

    public boolean isMotdEnabled() {
        Map<String, Object> motd = getSection("motd");
        Object value = motd.get("enabled");
        return value instanceof Boolean ? (Boolean) value : true;
    }

    public boolean isMaintenance() {
        Map<String, Object> motd = getSection("motd");
        Object value = motd.get("maintenance");
        return value instanceof Boolean ? (Boolean) value : false;
    }

    public void setMaintenance(boolean maintenance) {
        Map<String, Object> motd = getSection("motd");
        motd.put("maintenance", maintenance);
        save();
    }

    public List<String> getMotdMaintenanceLines() {
        Map<String, Object> motd = getSection("motd");
        Object value = motd.get("maintenance-lines");
        if (value instanceof List) {
            List<String> result = new ArrayList<>();
            for (Object item : (List<?>) value) {
                result.add(item.toString());
            }
            return result;
        }
        List<String> defaults = new ArrayList<>();
        defaults.add("<red>⚠ Server under maintenance</red>");
        defaults.add("<gray>We'll be back soon!</gray>");
        return defaults;
    }

    public List<String> getMotdPlayers() {
        Map<String, Object> motd = getSection("motd");
        Object value = motd.get("players");
        if (value instanceof List) {
            List<String> result = new ArrayList<>();
            for (Object item : (List<?>) value) {
                result.add(item.toString());
            }
            return result;
        }
        List<String> defaults = new ArrayList<>();
        defaults.add("<gradient:gold:yellow>✦ Dopamine Network ✦</gradient>");
        defaults.add("<green>Join us at play.dopamine.xyz</green>");
        return defaults;
    }

    // --- Announcements ---

    public int getAnnouncementIntervalSeconds() {
        Map<String, Object> announcements = getSection("announcements");
        Object value = announcements.get("interval-seconds");
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 300;
    }

    public List<String> getAnnouncementMessages() {
        Map<String, Object> announcements = getSection("announcements");
        Object value = announcements.get("messages");
        if (value instanceof List) {
            List<String> result = new ArrayList<>();
            for (Object item : (List<?>) value) {
                result.add(item.toString());
            }
            return result;
        }
        List<String> defaults = new ArrayList<>();
        defaults.add("<gradient:aqua:light_purple>★ Welcome to Dopamine Network!</gradient>");
        defaults.add("<yellow>⚡ Use /hub to return to the lobby</yellow>");
        return defaults;
    }

    public boolean isAnnouncementsEnabled() {
        Map<String, Object> announcements = getSection("announcements");
        Object value = announcements.get("enabled");
        return value instanceof Boolean ? (Boolean) value : true;
    }

    public void setAnnouncementsEnabled(boolean enabled) {
        Map<String, Object> announcements = getSection("announcements");
        announcements.put("enabled", enabled);
        save();
    }

    // --- Chat ---

    public String getBroadcastFormat() {
        Map<String, Object> chat = getSection("chat");
        Map<String, Object> broadcast = getSection(chat, "broadcast");
        Object value = broadcast.get("format");
        return value != null ? value.toString()
                : "<dark_gray>[</dark_gray><red>Broadcast</red><dark_gray>]</dark_gray> <white>{message}</white>";
    }

    public String getStaffChatFormat() {
        Map<String, Object> chat = getSection("chat");
        Map<String, Object> staff = getSection(chat, "staff");
        Object value = staff.get("format");
        return value != null ? value.toString()
                : "<dark_gray>[</dark_gray><red>🛡 Staff</red><dark_gray>]</dark_gray> <dark_red>{player}</dark_red><dark_gray>:</dark_gray> <white>{message}</white>";
    }

    // --- Helpers ---

    @SuppressWarnings("unchecked")
    private Map<String, Object> getSection(String key) {
        Object value = config.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        Map<String, Object> empty = new LinkedHashMap<>();
        config.put(key, empty);
        return empty;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getSection(Map<String, Object> parent, String key) {
        Object value = parent.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        Map<String, Object> empty = new LinkedHashMap<>();
        parent.put(key, empty);
        return empty;
    }

    private Map<String, Object> createDefaults() {
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("target-server", "lobby");

        Map<String, Object> motd = new LinkedHashMap<>();
        motd.put("enabled", true);
        motd.put("maintenance", false);
        List<String> maintenanceLines = new ArrayList<>();
        maintenanceLines.add("<red>⚠ Server under maintenance</red>");
        maintenanceLines.add("<gray>We'll be back soon!</gray>");
        motd.put("maintenance-lines", maintenanceLines);
        List<String> motdPlayers = new ArrayList<>();
        motdPlayers.add("<gradient:gold:yellow>✦ Dopamine Network ✦</gradient>");
        motdPlayers.add("<green>Join us at play.dopamine.xyz</green>");
        motd.put("players", motdPlayers);
        defaults.put("motd", motd);

        Map<String, Object> announcements = new LinkedHashMap<>();
        announcements.put("enabled", false);
        announcements.put("interval-seconds", 300);
        List<String> announcementMessages = new ArrayList<>();
        announcementMessages.add("<gradient:aqua:light_purple>★ Welcome to Dopamine Network!</gradient>");
        announcementMessages.add("<yellow>⚡ Use /hub to return to the lobby</yellow>");
        announcements.put("messages", announcementMessages);
        defaults.put("announcements", announcements);

        Map<String, Object> chat = new LinkedHashMap<>();

        Map<String, Object> broadcastChat = new LinkedHashMap<>();
        broadcastChat.put("format",
                "<dark_gray>[</dark_gray><red>Broadcast</red><dark_gray>]</dark_gray> <white>{message}</white>");
        chat.put("broadcast", broadcastChat);

        Map<String, Object> staffChat = new LinkedHashMap<>();
        staffChat.put("format",
                "<dark_gray>[</dark_gray><red>🛡 Staff</red><dark_gray>]</dark_gray> <dark_red>{player}</dark_red><dark_gray>:</dark_gray> <white>{message}</white>");
        chat.put("staff", staffChat);

        defaults.put("chat", chat);

        Map<String, Object> resourcePack = new LinkedHashMap<>();
        resourcePack.put("enabled", false);
        resourcePack.put("url", "");
        resourcePack.put("sha1", "");
        defaults.put("resource-pack", resourcePack);

        return defaults;
    }

    public boolean isResourcePackEnabled() {
        Map<String, Object> resourcePack = getSection("resource-pack");
        Object value = resourcePack.get("enabled");
        return value instanceof Boolean ? (Boolean) value : false;
    }

    public String getResourcePackUrl() {
        Map<String, Object> resourcePack = getSection("resource-pack");
        Object value = resourcePack.get("url");
        return value != null ? value.toString() : "";
    }

    public String getResourcePackSha1() {
        Map<String, Object> resourcePack = getSection("resource-pack");
        Object value = resourcePack.get("sha1");
        return value != null ? value.toString() : "";
    }
}
