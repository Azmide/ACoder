package io.github.AzmiiD.aCoder;

import io.github.AzmiiD.aCoder.commands.ACoderCommand;
import io.github.AzmiiD.aCoder.commands.RedeemCommand;
import io.github.AzmiiD.aCoder.managers.CodeManager;
import io.github.AzmiiD.aCoder.managers.Messages;
import org.bukkit.plugin.java.JavaPlugin;

public class ACoder extends JavaPlugin {

    private CodeManager codes;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Messages messages = new Messages(this);
        codes = new CodeManager(this);

        getCommand("redeem").setExecutor(new RedeemCommand(codes, messages));

        ACoderCommand admin = new ACoderCommand(this, codes, messages);
        getCommand("acoder").setExecutor(admin);
        getCommand("acoder").setTabCompleter(admin);
    }

    @Override
    public void onDisable() {
        if (codes != null) {
            codes.saveData();
        }
    }

    /** Reloads config.yml and data.yml from disk. */
    public void reload() {
        reloadConfig();
        codes.loadData();
    }
}
