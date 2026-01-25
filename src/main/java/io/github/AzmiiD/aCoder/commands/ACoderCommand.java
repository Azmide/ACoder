package io.github.AzmiiD.aCoder.commands;

import io.github.AzmiiD.aCoder.ACoder;
import io.github.AzmiiD.aCoder.managers.ConfigManager;
import io.github.AzmiiD.aCoder.managers.DataManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class ACoderCommand implements CommandExecutor, TabCompleter {

    private final ACoder plugin;
    private final ConfigManager configManager;
    private final DataManager dataManager;

    public ACoderCommand(ACoder plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.dataManager = plugin.getDataManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("acoder.admin")) {
            sender.sendMessage(configManager.getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create" -> handleCreate(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "list" -> handleList(sender);
            case "reload" -> handleReload(sender);
            case "setlimit" -> handleSetLimit(sender, args);
            case "setexpiry" -> handleSetExpiry(sender, args);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(configManager.getMessage("usage-create"));
            return;
        }

        String code = args[1];

        if (configManager.codeExists(code)) {
            sender.sendMessage(configManager.getMessage("code-exists", "%code%", code));
            return;
        }

        configManager.createCode(code);
        sender.sendMessage(configManager.getMessage("code-created", "%code%", code));
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(configManager.getMessage("usage-delete"));
            return;
        }

        String code = args[1];

        if (!configManager.codeExists(code)) {
            sender.sendMessage(configManager.getMessage("code-not-found", "%code%", code));
            return;
        }

        configManager.deleteCode(code);
        dataManager.removeCodeData(code);
        sender.sendMessage(configManager.getMessage("code-deleted", "%code%", code));
    }

    private void handleSetLimit(CommandSender sender, String[] args) {
        if (args.length != 3) {
            sender.sendMessage(configManager.getMessage("usage-setlimit"));
            return;
        }

        String code = args[1];

        if (!configManager.codeExists(code)) {
            sender.sendMessage(configManager.getMessage("code-not-found", "%code%", code));
            return;
        }

        try {
            int limit = Integer.parseInt(args[2]);
            if (limit < 0) {
                sender.sendMessage(configManager.getPrefix() + " §cLimit must be 0 or greater!");
                return;
            }

            configManager.setMaxUses(code, limit);
            sender.sendMessage(configManager.getMessage("limit-set", "%code%", code)
                    .replace("%limit%", String.valueOf(limit)));
        } catch (NumberFormatException e) {
            sender.sendMessage(configManager.getPrefix() + " §cInvalid number!");
        }
    }

    private void handleSetExpiry(CommandSender sender, String[] args) {
        if (args.length != 3) {
            sender.sendMessage(configManager.getMessage("usage-setexpiry"));
            return;
        }

        String code = args[1];
        String date = args[2];

        if (!configManager.codeExists(code)) {
            sender.sendMessage(configManager.getMessage("code-not-found", "%code%", code));
            return;
        }

        if (!configManager.isValidDateFormat(date)) {
            sender.sendMessage(configManager.getMessage("invalid-date"));
            return;
        }

        configManager.setExpiryDate(code, date);
        sender.sendMessage(configManager.getMessage("expiry-set", "%code%", code)
                .replace("%date%", date));
    }

    private void handleList(CommandSender sender) {
        List<String> codes = configManager.getAllCodes();

        if (codes.isEmpty()) {
            sender.sendMessage(configManager.getMessage("list-empty"));
            return;
        }

        sender.sendMessage(configManager.getMessage("list-header"));
        for (String code : codes) {
            int count = dataManager.getRedemptionCount(code);
            int maxUses = configManager.getMaxUses(code);
            String expiryDate = configManager.getExpiryDate(code);

            // Determine status
            String status = "";
            if (configManager.isCodeExpired(code)) {
                status = "§c[EXPIRED]";
            } else if (maxUses > 0 && count >= maxUses) {
                status = "§c[LIMIT REACHED]";
            } else if (expiryDate != null && !expiryDate.isEmpty()) {
                status = "§a[Expires: " + expiryDate + "]";
            } else {
                status = "§a[Active]";
            }

            String limitText = maxUses > 0 ? String.valueOf(maxUses) : "∞";

            String message = configManager.getMessage("list-entry", "%code%", code)
                    .replace("%count%", String.valueOf(count))
                    .replace("%limit%", limitText)
                    .replace("%status%", status);
            sender.sendMessage(message);
        }
    }

    private void handleReload(CommandSender sender) {
        plugin.reload();
        sender.sendMessage(configManager.getMessage("reload"));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(configManager.getPrefix() + " §6ACoder Commands:");
        sender.sendMessage("§e/acoder create <code> §7- Create a new code");
        sender.sendMessage("§e/acoder delete <code> §7- Delete a code");
        sender.sendMessage("§e/acoder setlimit <code> <amount> §7- Set usage limit (0 = unlimited)");
        sender.sendMessage("§e/acoder setexpiry <code> <DD-MM-YYYY> §7- Set expiry date");
        sender.sendMessage("§e/acoder list §7- List all codes");
        sender.sendMessage("§e/acoder reload §7- Reload configuration");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!sender.hasPermission("acoder.admin")) {
            return completions;
        }

        if (args.length == 1) {
            completions.add("create");
            completions.add("delete");
            completions.add("setlimit");
            completions.add("setexpiry");
            completions.add("list");
            completions.add("reload");
        } else if (args.length == 2) {
            String subCmd = args[0].toLowerCase();
            if (subCmd.equals("delete") || subCmd.equals("setlimit") || subCmd.equals("setexpiry")) {
                completions.addAll(configManager.getAllCodes());
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("setlimit")) {
                completions.add("0");
                completions.add("50");
                completions.add("100");
            } else if (args[0].equalsIgnoreCase("setexpiry")) {
                completions.add("31-12-2025");
                completions.add("DD-MM-YYYY");
            }
        }

        return completions;
    }
}