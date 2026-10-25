package io.github.AzmiiD.aCoder.managers;

import io.github.AzmiiD.aCoder.ACoder;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Codes and their settings live in config.yml.
 * Who redeemed what lives in data.yml as "uuid:ip" entries (older "name:ip" entries are still honored).
 */
public final class CodeManager {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-uuuu");
    private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9_-]{1,32}");

    private final ACoder plugin;
    private final File dataFile;
    private final Object saveLock = new Object();
    private YamlConfiguration data = new YamlConfiguration();

    public CodeManager(ACoder plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        load();
    }

    // ---- Code settings (config.yml) ----

    public static boolean isValidName(String code) {
        return VALID_NAME.matcher(code).matches();
    }

    /** The code as written in config.yml for a (case-insensitive) input, or null if there is none. */
    public String resolve(String input) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("codes");
        if (section == null) {
            return null;
        }
        for (String key : section.getKeys(false)) {
            if (key.equalsIgnoreCase(input)) {
                return key;
            }
        }
        return null;
    }

    public List<String> all() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("codes");
        if (section == null) {
            return List.of();
        }
        return section.getKeys(false).stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    public List<String> rewards(String code) {
        return plugin.getConfig().getStringList(path(code, "rewards"));
    }

    public void addReward(String code, String reward) {
        List<String> list = new ArrayList<>(rewards(code));
        list.add(reward);
        plugin.getConfig().set(path(code, "rewards"), list);
        plugin.saveConfig();
    }

    /** Removes the reward at a 0-based index and returns it. */
    public String removeReward(String code, int index) {
        List<String> list = new ArrayList<>(rewards(code));
        String removed = list.remove(index);
        plugin.getConfig().set(path(code, "rewards"), list);
        plugin.saveConfig();
        return removed;
    }

    /** 0 means unlimited. */
    public int maxUses(String code) {
        return Math.max(0, plugin.getConfig().getInt(path(code, "max-uses"), 0));
    }

    public String expiryDate(String code) {
        String text = plugin.getConfig().getString(path(code, "expiry-date"));
        return text == null || text.isBlank() ? null : text;
    }

    /** A code stays valid through the end of its expiry day. */
    public boolean isExpired(String code) {
        String text = expiryDate(code);
        if (text == null) {
            return false;
        }
        LocalDate expiry = parseDate(text);
        if (expiry == null) {
            plugin.getLogger().warning("Invalid expiry-date for code " + code + ": " + text);
            return false;
        }
        return LocalDate.now().isAfter(expiry);
    }

    public boolean isFull(String code) {
        int max = maxUses(code);
        return max > 0 && redemptionCount(code) >= max;
    }

    public void create(String code) {
        plugin.getConfig().set(path(code, "rewards"), new ArrayList<String>());
        plugin.getConfig().set(path(code, "max-uses"), 0);
        plugin.saveConfig();
    }

    public void delete(String code) {
        plugin.getConfig().set("codes." + code, null);
        plugin.saveConfig();
        data.set("redeemed." + code, null);
        saveAsync();
    }

    public void setMaxUses(String code, int limit) {
        plugin.getConfig().set(path(code, "max-uses"), limit);
        plugin.saveConfig();
    }

    /** Pass null to remove the expiry date. */
    public void setExpiryDate(String code, String date) {
        plugin.getConfig().set(path(code, "expiry-date"), date);
        plugin.saveConfig();
    }

    /** Parses DD-MM-YYYY, or returns null if it is not a real date. */
    public static LocalDate parseDate(String text) {
        try {
            return LocalDate.parse(text, DATE_FORMAT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String path(String code, String key) {
        return "codes." + code + "." + key;
    }

    // ---- Redemptions (data.yml) ----

    /** True if this player (UUID or legacy name) or, when checkIp is set, this IP already used the code. */
    public boolean hasRedeemed(String code, UUID uuid, String name, String ip, boolean checkIp) {
        for (String entry : data.getStringList("redeemed." + code)) {
            int split = entry.indexOf(':'); // IPv6 addresses contain ':' too
            if (split < 0) continue;
            String who = entry.substring(0, split);
            if (who.equalsIgnoreCase(uuid.toString()) || who.equalsIgnoreCase(name)) {
                return true;
            }
            if (checkIp && !ip.equals("unknown") && entry.substring(split + 1).equals(ip)) {
                return true;
            }
        }
        return false;
    }

    public void addRedemption(String code, UUID uuid, String ip) {
        List<String> entries = new ArrayList<>(data.getStringList("redeemed." + code));
        entries.add(uuid + ":" + ip);
        data.set("redeemed." + code, entries);
        saveAsync();
    }

    public int redemptionCount(String code) {
        return data.getStringList("redeemed." + code).size();
    }

    /** Forgets every redemption of a code and returns how many there were. */
    public int resetRedemptions(String code) {
        int count = redemptionCount(code);
        data.set("redeemed." + code, null);
        saveAsync();
        return count;
    }

    public void load() {
        data = YamlConfiguration.loadConfiguration(dataFile);
    }

    /** Serializes on the calling (main) thread, writes the file off it. */
    public void saveAsync() {
        String snapshot = data.saveToString();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> write(snapshot));
    }

    public void saveNow() {
        write(data.saveToString());
    }

    private void write(String content) {
        synchronized (saveLock) {
            try {
                Files.createDirectories(dataFile.toPath().getParent());
                Path tmp = dataFile.toPath().resolveSibling("data.yml.tmp");
                Files.writeString(tmp, content, StandardCharsets.UTF_8);
                Files.move(tmp, dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save data.yml: " + e.getMessage());
            }
        }
    }
}
