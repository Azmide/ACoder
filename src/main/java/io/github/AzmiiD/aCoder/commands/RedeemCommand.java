package io.github.AzmiiD.aCoder.commands;

import io.github.AzmiiD.aCoder.managers.CodeManager;
import io.github.AzmiiD.aCoder.managers.Messages;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RedeemCommand implements CommandExecutor {

    private final CodeManager codes;
    private final Messages messages;

    public RedeemCommand(CodeManager codes, Messages messages) {
        this.codes = codes;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }
        if (!player.hasPermission("acoder.redeem")) {
            player.sendMessage(messages.get("no-permission"));
            return true;
        }
        if (args.length != 1) {
            player.sendMessage(messages.get("usage-redeem"));
            return true;
        }

        String code = args[0];
        String name = player.getName();
        String ip = ipOf(player);

        if (!codes.exists(code)) {
            player.sendMessage(messages.get("invalid"));
        } else if (codes.isExpired(code)) {
            player.sendMessage(messages.get("code-expired"));
        } else if (codes.isFull(code)) {
            player.sendMessage(messages.get("limit-reached"));
        } else if (codes.hasRedeemed(code, name, ip)) {
            player.sendMessage(messages.get("used"));
        } else {
            for (String reward : codes.rewards(code)) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), reward.replace("%player_name%", name));
            }
            codes.addRedemption(code, name, ip);
            player.sendMessage(messages.get("success"));
        }
        return true;
    }

    private static String ipOf(Player player) {
        var address = player.getAddress();
        return address == null ? "unknown" : address.getAddress().getHostAddress();
    }
}
