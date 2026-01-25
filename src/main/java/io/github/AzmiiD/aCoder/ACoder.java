package io.github.AzmiiD.aCoder;

import io.github.AzmiiD.aCoder.commands.ACoderCommand;
import io.github.AzmiiD.aCoder.commands.RedeemCommand;
import io.github.AzmiiD.aCoder.managers.ConfigManager;
import io.github.AzmiiD.aCoder.managers.DataManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ACoder extends JavaPlugin {

    private static ACoder instance;
    private ConfigManager configManager;
    private DataManager dataManager;

    @Override
    public void onEnable() {
        instance = this;

        // Initialize managers
        configManager = new ConfigManager(this);
        dataManager = new DataManager(this);

        // Load configurations
        configManager.loadConfig();
        dataManager.loadData();

        // Register commands
        getCommand("redeem").setExecutor(new RedeemCommand(this));
        getCommand("acoder").setExecutor(new ACoderCommand(this));

        getLogger().info("ACoder has been enabled!");
    }

    @Override
    public void onDisable() {
        // Save data on shutdown
        if (dataManager != null) {
            dataManager.saveData();
        }
        getLogger().info("ACoder has been disabled!");
    }

    public static ACoder getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public void reload() {
        configManager.loadConfig();
        dataManager.loadData();
    }
}
