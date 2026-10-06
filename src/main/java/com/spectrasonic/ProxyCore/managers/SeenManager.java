package com.spectrasonic.ProxyCore.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.velocitypowered.api.proxy.Player;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;

/**
 * Tracks player connections so {@code /seen} can answer "when was this player last online?" even
 * for players who are offline right now.
 *
 * <p>Records are kept in {@code seen-data.json} inside the plugin data directory and written to
 * disk on every disconnect (the file is small, and Velocity fires connection events async, so a
 * blocking write there is safe). The manager is also called by {@code /whois} to report session
 * length for players that are online.
 */
public class SeenManager {

    /** Connection history of a single player. Timestamps are epoch millis; {@code 0} = unknown. */
    public static class SeenRecord {
        public UUID uuid;
        public String name;
        public long firstJoin;
        public long lastJoin;
        public long lastQuit;
        public String lastIp = "";
        public String lastServer = "";
    }

    private final Path dataFile;
    private final Logger logger;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Object saveLock = new Object();

    private final Map<UUID, SeenRecord> records = new ConcurrentHashMap<>();
    private final Map<String, UUID> nameIndex = new ConcurrentHashMap<>();

    public SeenManager(Path dataDirectory, Logger logger) {
        this.dataFile = dataDirectory.resolve("seen-data.json");
        this.logger = logger;
        load();
    }

    /** Records a login: creates the record on first join or refreshes the existing one. */
    public void onJoin(Player player) {
        SeenRecord record = records.computeIfAbsent(player.getUniqueId(), uuid -> {
            SeenRecord created = new SeenRecord();
            created.uuid = uuid;
            created.firstJoin = System.currentTimeMillis();
            return created;
        });
        record.name = player.getUsername();
        record.lastJoin = System.currentTimeMillis();
        record.lastIp = getIp(player);
        nameIndex.put(record.name.toLowerCase(Locale.ROOT), record.uuid);
        save();
    }

    /** Records a logout: closes the last session with the server the player was on. */
    public void onDisconnect(Player player) {
        SeenRecord record = records.get(player.getUniqueId());
        if (record == null) {
            // Player joined before tracking was enabled; still open a minimal record
            record = records.computeIfAbsent(player.getUniqueId(), uuid -> {
                SeenRecord created = new SeenRecord();
                created.uuid = uuid;
                created.firstJoin = System.currentTimeMillis();
                return created;
            });
            record.name = player.getUsername();
            record.lastJoin = 0;
            nameIndex.put(record.name.toLowerCase(Locale.ROOT), record.uuid);
        }
        record.lastQuit = System.currentTimeMillis();
        record.lastIp = getIp(player);
        record.lastServer = player.getCurrentServer()
                .map(conn -> conn.getServerInfo().getName())
                .orElse("");
        save();
    }

    public Optional<SeenRecord> getRecord(UUID uuid) {
        return Optional.ofNullable(records.get(uuid));
    }

    /** Case-insensitive lookup by player name (the name they last joined with). */
    public Optional<SeenRecord> findByName(String name) {
        UUID uuid = nameIndex.get(name.toLowerCase(Locale.ROOT));
        return uuid != null ? getRecord(uuid) : Optional.empty();
    }

    public boolean hasRecord(UUID uuid) {
        return records.containsKey(uuid);
    }

    /** Names of every player the proxy has seen, as stored in their records. */
    public List<String> getKnownNames() {
        List<String> names = new ArrayList<>();
        for (SeenRecord record : records.values()) {
            if (record.name != null && !record.name.isEmpty()) {
                names.add(record.name);
            }
        }
        return names;
    }

    public void save() {
        synchronized (saveLock) {
            try {
                Files.createDirectories(dataFile.getParent());
                try (OutputStreamWriter writer = new OutputStreamWriter(
                        new FileOutputStream(dataFile.toFile()), StandardCharsets.UTF_8)) {
                    gson.toJson(records, writer);
                }
            } catch (IOException e) {
                logger.warn("Could not save seen-data.json", e);
            }
        }
    }

    private void load() {
        if (!Files.exists(dataFile)) {
            return;
        }
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(dataFile.toFile()),
                StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<UUID, SeenRecord>>() {}.getType();
            Map<UUID, SeenRecord> loaded = gson.fromJson(reader, type);
            if (loaded == null) {
                return;
            }
            records.putAll(loaded);
            for (SeenRecord record : records.values()) {
                if (record.name != null && !record.name.isEmpty()) {
                    nameIndex.put(record.name.toLowerCase(Locale.ROOT), record.uuid);
                }
            }
        } catch (IOException | RuntimeException e) {
            // Corrupt file: start fresh instead of refusing to boot, old data is overwritten on next save
            logger.warn("Could not load seen-data.json, starting with empty history", e);
        }
    }

    private static String getIp(Player player) {
        return player.getRemoteAddress().getAddress() != null
                ? player.getRemoteAddress().getAddress().getHostAddress()
                : "";
    }

    /** Formats a duration as {@code 3d 5h 12m}, dropping leading zero units. */
    public static String formatDuration(long millis) {
        long totalMinutes = millis / 60000L;
        long days = totalMinutes / 1440L;
        long hours = (totalMinutes % 1440L) / 60L;
        long minutes = totalMinutes % 60L;
        StringBuilder builder = new StringBuilder();
        if (days > 0) {
            builder.append(days).append("d ");
        }
        if (days > 0 || hours > 0) {
            builder.append(hours).append("h ");
        }
        builder.append(minutes).append("m");
        return builder.toString();
    }
}
