package io.github.AzmiiD.aCoder.managers;

import io.github.AzmiiD.aCoder.ACoder;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DataManager {

    private final ACoder plugin;
    private File dataFile;
    private FileConfiguration dataConfig;

    public DataManager(ACoder plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
    }

    public void loadData() {
        if (!dataFile.exists()) {
            plugin.saveResource("data.yml", false);
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void saveData() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data.yml: " + e.getMessage());
        }
    }

    public boolean hasRedeemed(String code, String playerName, String ip) {
        List<String> redeemed = dataConfig.getStringList("redeemed." + code);

        for (String entry : redeemed) {
            String[] parts = entry.split(":");
            if (parts.length == 2) {
                String savedPlayer = parts[0];
                String savedIp = parts[1];

                // Check if player name or IP matches
                if (savedPlayer.equalsIgnoreCase(playerName) || savedIp.equals(ip)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void addRedemption(String code, String playerName, String ip) {
        List<String> redeemed = dataConfig.getStringList("redeemed." + code);
        if (redeemed == null) {
            redeemed = new ArrayList<>();
        }

        redeemed.add(playerName + ":" + ip);
        dataConfig.set("redeemed." + code, redeemed);
        saveData();
    }

    public int getRedemptionCount(String code) {
        List<String> redeemed = dataConfig.getStringList("redeemed." + code);
        return redeemed != null ? redeemed.size() : 0;
    }

    public void removeCodeData(String code) {
        dataConfig.set("redeemed." + code, null);
        saveData();
    }
}