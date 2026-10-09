package io.github.AzmiiD.aCoder.managers;

import io.github.AzmiiD.aCoder.ACoder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Codes and their settings live in config.yml.
 * Who redeemed what lives in data.yml as "player:ip" entries.
 */
public class CodeManager {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-uuuu");

    private final ACoder plugin;
    private final File dataFile;
    private YamlConfiguration data;

    public CodeManager(ACoder plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        loadData();
    }

    // ---- Code settings (config.yml) ----

    public boolean exists(String code) {
        return plugin.getConfig().isConfigurationSection("codes." + code);
    }

    public List<String> all() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("codes");
        return section == null ? List.of() : List.copyOf(section.getKeys(false));
    }

    public List<String> rewards(String code) {
        return plugin.getConfig().getStringList("codes." + code + ".rewards");
    }

    /** 0 means unlimited. */
    public int maxUses(String code) {
        return plugin.getConfig().getInt("codes." + code + ".max-uses", 0);
    }

    public String expiryDate(String code) {
        return plugin.getConfig().getString("codes." + code + ".expiry-date");
    }

    /** A code stays valid through the end of its expiry day. */
    public boolean isExpired(String code) {
        String text = expiryDate(code);
        if (text == null || text.isBlank()) {
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
        plugin.getConfig().set("codes." + code + ".rewards", List.of("give %player_name% diamond 1"));
        plugin.getConfig().set("codes." + code + ".max-uses", 0);
        plugin.saveConfig();
    }

    public void delete(String code) {
        plugin.getConfig().set("codes." + code, null);
        plugin.saveConfig();
        data.set("redeemed." + code, null);
        saveData();
    }

    public void setMaxUses(String code, int limit) {
        plugin.getConfig().set("codes." + code + ".max-uses", limit);
        plugin.saveConfig();
    }

    public void setExpiryDate(String code, String date) {
        plugin.getConfig().set("codes." + code + ".expiry-date", date);
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

    // ---- Redemptions (data.yml) ----

    /** True if this player name or IP already used the code. */
    public boolean hasRedeemed(String code, String player, String ip) {
        for (String entry : data.getStringList("redeemed." + code)) {
            int split = entry.indexOf(':'); // IPv6 addresses contain ':' too
            if (split < 0) continue;
            if (entry.substring(0, split).equalsIgnoreCase(player) || entry.substring(split + 1).equals(ip)) {
                return true;
            }
        }
        return false;
    }

    public void addRedemption(String code, String player, String ip) {
        List<String> entries = new ArrayList<>(data.getStringList("redeemed." + code));
        entries.add(player + ":" + ip);
        data.set("redeemed." + code, entries);
        saveData();
    }

    public int redemptionCount(String code) {
        return data.getStringList("redeemed." + code).size();
    }

    public void loadData() {
        data = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void saveData() {
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data.yml: " + e.getMessage());
        }
    }
}
