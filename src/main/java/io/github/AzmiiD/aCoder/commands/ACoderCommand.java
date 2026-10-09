package io.github.AzmiiD.aCoder.commands;

import io.github.AzmiiD.aCoder.ACoder;
import io.github.AzmiiD.aCoder.managers.CodeManager;
import io.github.AzmiiD.aCoder.managers.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;
import java.util.Map;

public class ACoderCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("create", "delete", "setlimit", "setexpiry", "list", "reload");

    private final ACoder plugin;
    private final CodeManager codes;
    private final Messages messages;

    public ACoderCommand(ACoder plugin, CodeManager codes, Messages messages) {
        this.plugin = plugin;
        this.codes = codes;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("acoder.admin")) {
            sender.sendMessage(messages.get("no-permission"));
            return true;
        }
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> create(sender, args);
            case "delete" -> delete(sender, args);
            case "setlimit" -> setLimit(sender, args);
            case "setexpiry" -> setExpiry(sender, args);
            case "list" -> list(sender);
            case "reload" -> {
                plugin.reload();
                sender.sendMessage(messages.get("reload"));
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private void create(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(messages.get("usage-create"));
            return;
        }
        String code = args[1];
        if (codes.exists(code)) {
            sender.sendMessage(messages.get("code-exists", Map.of("%code%", code)));
            return;
        }
        codes.create(code);
        sender.sendMessage(messages.get("code-created", Map.of("%code%", code)));
    }

    private void delete(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(messages.get("usage-delete"));
            return;
        }
        String code = args[1];
        if (!codes.exists(code)) {
            sender.sendMessage(messages.get("code-not-found", Map.of("%code%", code)));
            return;
        }
        codes.delete(code);
        sender.sendMessage(messages.get("code-deleted", Map.of("%code%", code)));
    }

    private void setLimit(CommandSender sender, String[] args) {
        if (args.length != 3) {
            sender.sendMessage(messages.get("usage-setlimit"));
            return;
        }
        String code = args[1];
        if (!codes.exists(code)) {
            sender.sendMessage(messages.get("code-not-found", Map.of("%code%", code)));
            return;
        }
        int limit;
        try {
            limit = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            limit = -1;
        }
        if (limit < 0) {
            sender.sendMessage(messages.get("invalid-number"));
            return;
        }
        codes.setMaxUses(code, limit);
        sender.sendMessage(messages.get("limit-set", Map.of("%code%", code, "%limit%", String.valueOf(limit))));
    }

    private void setExpiry(CommandSender sender, String[] args) {
        if (args.length != 3) {
            sender.sendMessage(messages.get("usage-setexpiry"));
            return;
        }
        String code = args[1];
        String date = args[2];
        if (!codes.exists(code)) {
            sender.sendMessage(messages.get("code-not-found", Map.of("%code%", code)));
            return;
        }
        if (CodeManager.parseDate(date) == null) {
            sender.sendMessage(messages.get("invalid-date"));
            return;
        }
        codes.setExpiryDate(code, date);
        sender.sendMessage(messages.get("expiry-set", Map.of("%code%", code, "%date%", date)));
    }

    private void list(CommandSender sender) {
        List<String> all = codes.all();
        if (all.isEmpty()) {
            sender.sendMessage(messages.get("list-empty"));
            return;
        }
        var config = plugin.getConfig();
        sender.sendMessage(messages.color(config.getString("messages.list-header", "&6=== Codes ===")));
        for (String code : all) {
            int max = codes.maxUses(code);
            String line = config.getString("messages.list-entry", "%code% %status%")
                    .replace("%code%", code)
                    .replace("%count%", String.valueOf(codes.redemptionCount(code)))
                    .replace("%limit%", max > 0 ? String.valueOf(max) : "∞")
                    .replace("%status%", status(code));
            sender.sendMessage(messages.color(line));
        }
    }

    private String status(String code) {
        if (codes.isExpired(code)) return "&c[EXPIRED]";
        if (codes.isFull(code)) return "&c[LIMIT REACHED]";
        String expiry = codes.expiryDate(code);
        return expiry == null || expiry.isBlank() ? "&a[Active]" : "&a[Expires: " + expiry + "]";
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(messages.prefixed("&6ACoder Commands:"));
        sender.sendMessage(messages.color("&e/acoder create <code> &7- Create a new code"));
        sender.sendMessage(messages.color("&e/acoder delete <code> &7- Delete a code"));
        sender.sendMessage(messages.color("&e/acoder setlimit <code> <amount> &7- Set usage limit (0 = unlimited)"));
        sender.sendMessage(messages.color("&e/acoder setexpiry <code> <DD-MM-YYYY> &7- Set expiry date"));
        sender.sendMessage(messages.color("&e/acoder list &7- List all codes"));
        sender.sendMessage(messages.color("&e/acoder reload &7- Reload configuration"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("acoder.admin")) {
            return List.of();
        }
        List<String> options = List.of();
        if (args.length == 1) {
            options = SUBCOMMANDS;
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "delete", "setlimit", "setexpiry" -> options = codes.all();
                default -> { }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("setlimit")) {
            options = List.of("0", "50", "100");
        }
        String typed = args[args.length - 1].toLowerCase();
        return options.stream().filter(o -> o.toLowerCase().startsWith(typed)).toList();
    }
}
