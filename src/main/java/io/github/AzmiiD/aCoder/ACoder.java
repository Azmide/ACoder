package io.github.AzmiiD.aCoder;

import io.github.AzmiiD.aCoder.commands.ACoderCommand;
import io.github.AzmiiD.aCoder.commands.RedeemCommand;
import io.github.AzmiiD.aCoder.config.Lang;
import io.github.AzmiiD.aCoder.managers.CodeManager;
import io.github.AzmiiD.aCoder.managers.RewardRunner;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;

public final class ACoder extends JavaPlugin {

    private final Lang lang = new Lang();
    private CodeManager codes;

    @Override
    public void onEnable() {
        registerPermissions();
        saveDefaults();
        codes = new CodeManager(this);
        loadLang();
        registerCommands();
    }

    @Override
    public void onDisable() {
        if (codes != null) {
            codes.saveNow();
        }
    }

    private void registerCommands() {
        RedeemCommand redeem = new RedeemCommand(this, codes, new RewardRunner(this));
        ACoderCommand admin = new ACoderCommand(this, codes);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();
            commands.register("redeem", "Redeem a code for rewards", List.of("code"), redeem);
            commands.register("acoder", "Manage redeem codes", List.of(), admin);
        });
    }

    private void registerPermissions() {
        PluginManager pm = getServer().getPluginManager();
        Object[][] nodes = {
                {"acoder.redeem", PermissionDefault.TRUE, "Redeem codes"},
                {"acoder.admin", PermissionDefault.OP, "Create, edit and delete codes, reload"},
        };
        for (Object[] n : nodes) {
            if (pm.getPermission((String) n[0]) == null) {
                pm.addPermission(new Permission((String) n[0], (String) n[2], (PermissionDefault) n[1]));
            }
        }
    }

    private void saveDefaults() {
        saveDefaultConfig();
        if (!new File(getDataFolder(), "lang.yml").exists()) {
            saveResource("lang.yml", false);
        }
    }

    private void loadLang() {
        lang.load(new File(getDataFolder(), "lang.yml"), getResource("lang.yml"), getConfig().getString("prefix", ""));
    }

    /** Reloads config.yml, lang.yml and data.yml from disk. */
    public void reload() {
        saveDefaults();
        reloadConfig();
        loadLang();
        codes.load();
    }

    public Lang lang() {
        return lang;
    }
}
