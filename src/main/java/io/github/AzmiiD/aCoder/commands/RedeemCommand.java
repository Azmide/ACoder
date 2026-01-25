package io.github.AzmiiD.aCoder.commands;

import io.github.AzmiiD.aCoder.ACoder;
import io.github.AzmiiD.aCoder.managers.ConfigManager;
import io.github.AzmiiD.aCoder.managers.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class RedeemCommand implements CommandExecutor {

    private final ACoder plugin;
    private final ConfigManager configManager;
    private final DataManager dataManager;

    public RedeemCommand(ACoder plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.dataManager = plugin.getDataManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }

        if (!player.hasPermission("acoder.redeem")) {
            player.sendMessage(configManager.getMessage("no-permission"));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(configManager.getMessage("usage-redeem"));
            return true;
        }

        String code = args[0];

        // Check if code exists
        if (!configManager.codeExists(code)) {
            player.sendMessage(configManager.getMessage("invalid"));
            return true;
        }

        // Check if code is expired
        if (configManager.isCodeExpired(code)) {
            player.sendMessage(configManager.getMessage("code-expired"));
            return true;
        }

        // Check usage limit
        int maxUses = configManager.getMaxUses(code);
        if (maxUses > 0) {
            int currentUses = dataManager.getRedemptionCount(code);
            if (currentUses >= maxUses) {
                player.sendMessage(configManager.getMessage("limit-reached"));
                return true;
            }
        }

        // Get player IP
        String ip = player.getAddress().getAddress().getHostAddress();
        String playerName = player.getName();

        // Check if already redeemed
        if (dataManager.hasRedeemed(code, playerName, ip)) {
            player.sendMessage(configManager.getMessage("used"));
            return true;
        }

        // Execute reward commands
        List<String> rewards = configManager.getCodeRewards(code);
        for (String reward : rewards) {
            String processedCommand = reward.replace("%player_name%", playerName);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
        }

        // Mark as redeemed
        dataManager.addRedemption(code, playerName, ip);

        // Send success message
        player.sendMessage(configManager.getMessage("success"));

        return true;
    }
}