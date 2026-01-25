package io.github.AzmiiD.aCoder.managers;

import io.github.AzmiiD.aCoder.ACoder;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ConfigManager {

    private final ACoder plugin;
    private FileConfiguration config;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");

    public ConfigManager(ACoder plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
    }

    public String getPrefix() {
        return colorize(config.getString("prefix", "&6[ACoder]&r"));
    }

    public String getMessage(String key) {
        return colorize(getPrefix() + " " + config.getString("messages." + key, "&cMessage not found: " + key));
    }

    public String getMessage(String key, String placeholder, String value) {
        return getMessage(key).replace(placeholder, value);
    }

    public boolean codeExists(String code) {
        return config.getConfigurationSection("codes." + code) != null;
    }

    public List<String> getCodeRewards(String code) {
        return config.getStringList("codes." + code + ".rewards");
    }

    public int getMaxUses(String code) {
        return config.getInt("codes." + code + ".max-uses", 0);
    }

    public String getExpiryDate(String code) {
        return config.getString("codes." + code + ".expiry-date", null);
    }

    public boolean isCodeExpired(String code) {
        String expiryDateStr = getExpiryDate(code);
        if (expiryDateStr == null || expiryDateStr.isEmpty()) {
            return false; // No expiry date set
        }

        try {
            Date expiryDate = dateFormat.parse(expiryDateStr);
            Date now = new Date();
            return now.after(expiryDate);
        } catch (ParseException e) {
            plugin.getLogger().warning("Invalid date format for code " + code + ": " + expiryDateStr);
            return false;
        }
    }

    public void createCode(String code) {
        config.set("codes." + code + ".rewards", List.of("give %player_name% diamond 1"));
        config.set("codes." + code + ".max-uses", 0);
        plugin.saveConfig();
    }

    public void deleteCode(String code) {
        config.set("codes." + code, null);
        plugin.saveConfig();
    }

    public void setMaxUses(String code, int limit) {
        config.set("codes." + code + ".max-uses", limit);
        plugin.saveConfig();
    }

    public void setExpiryDate(String code, String date) {
        config.set("codes." + code + ".expiry-date", date);
        plugin.saveConfig();
    }

    public boolean isValidDateFormat(String date) {
        try {
            dateFormat.parse(date);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    public List<String> getAllCodes() {
        if (config.getConfigurationSection("codes") == null) {
            return List.of();
        }
        return List.copyOf(config.getConfigurationSection("codes").getKeys(false));
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}