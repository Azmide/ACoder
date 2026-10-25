package io.github.AzmiiD.aCoder.commands;

import io.github.AzmiiD.aCoder.ACoder;
import io.github.AzmiiD.aCoder.config.Lang;
import io.github.AzmiiD.aCoder.managers.CodeManager;
import io.github.AzmiiD.aCoder.util.Text;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

public final class ACoderCommand implements BasicCommand {

    private static final List<String> SUBCOMMANDS = List.of(
            "create", "delete", "info", "list", "setlimit", "setexpiry", "addreward", "removereward", "reset", "reload");

    private static final String[][] HELP = {
            {"/acoder create <code>", "Create a new code"},
            {"/acoder delete <code>", "Delete a code"},
            {"/acoder info <code>", "Show a code's rewards and settings"},
            {"/acoder list", "List all codes"},
            {"/acoder setlimit <code> <amount>", "Set usage limit (0 = unlimited)"},
            {"/acoder setexpiry <code> <DD-MM-YYYY|never>", "Set or clear the expiry date"},
            {"/acoder addreward <code> <command...>", "Add a reward command"},
            {"/acoder removereward <code> <number>", "Remove a reward (see /acoder info)"},
            {"/acoder reset <code>", "Let everyone redeem a code again"},
            {"/acoder reload", "Reload config.yml, lang.yml and data.yml"},
    };

    private final ACoder plugin;
    private final CodeManager codes;

    public ACoderCommand(ACoder plugin, CodeManager codes) {
        this.plugin = plugin;
        this.codes = codes;
    }

    private Lang lang() {
        return plugin.lang();
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        CommandSender sender = CommandUtil.actor(stack);
        if (!sender.hasPermission("acoder.admin")) {
            lang().send(sender, "no-permission");
            return;
        }
        if (args.length == 0) {
            help(sender);
            return;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create" -> create(sender, args);
            case "delete" -> delete(sender, args);
            case "info" -> info(sender, args);
            case "list" -> list(sender);
            case "setlimit" -> setLimit(sender, args);
            case "setexpiry" -> setExpiry(sender, args);
            case "addreward" -> addReward(sender, args);
            case "removereward" -> removeReward(sender, args);
            case "reset" -> reset(sender, args);
            case "reload" -> {
                plugin.reload();
                lang().send(sender, "reloaded");
            }
            default -> help(sender);
        }
    }

    /** Resolves args[1] to an existing code, or tells the sender why not. */
    private String existing(CommandSender sender, String[] args, String usageKey, int expectedArgs) {
        if (args.length != expectedArgs) {
            lang().send(sender, usageKey);
            return null;
        }
        String code = codes.resolve(args[1]);
        if (code == null) {
            lang().send(sender, "admin.code-not-found", Lang.ph("code", Text.escape(args[1])));
        }
        return code;
    }

    private void create(CommandSender sender, String[] args) {
        if (args.length != 2) {
            lang().send(sender, "admin.usage-create");
            return;
        }
        String code = args[1];
        if (!CodeManager.isValidName(code)) {
            lang().send(sender, "admin.invalid-name");
        } else if (codes.resolve(code) != null) {
            lang().send(sender, "admin.code-exists", Lang.ph("code", code));
        } else {
            codes.create(code);
            lang().send(sender, "admin.code-created", Lang.ph("code", code));
        }
    }

    private void delete(CommandSender sender, String[] args) {
        String code = existing(sender, args, "admin.usage-delete", 2);
        if (code == null) return;
        codes.delete(code);
        lang().send(sender, "admin.code-deleted", Lang.ph("code", code));
    }

    private void info(CommandSender sender, String[] args) {
        String code = existing(sender, args, "admin.usage-info", 2);
        if (code == null) return;
        lang().send(sender, "admin.info-header", Lang.ph("code", code));
        infoLine(sender, "Redeemed", codes.redemptionCount(code) + "/" + limitText(code));
        infoLine(sender, "Expires", codes.expiryDate(code) == null ? "never" : codes.expiryDate(code));
        infoLine(sender, "Status", Text.plain(statusRaw(code)));
        List<String> rewards = codes.rewards(code);
        infoLine(sender, "Rewards", rewards.isEmpty() ? "none" : String.valueOf(rewards.size()));
        for (int i = 0; i < rewards.size(); i++) {
            lang().send(sender, "admin.info-reward",
                    Lang.ph("index", String.valueOf(i + 1), "reward", Text.escape(rewards.get(i))));
        }
    }

    private void infoLine(CommandSender sender, String label, String value) {
        lang().send(sender, "admin.info-line", Lang.ph("label", label, "value", Text.escape(value)));
    }

    private void list(CommandSender sender) {
        List<String> all = codes.all();
        if (all.isEmpty()) {
            lang().send(sender, "admin.list-empty");
            return;
        }
        lang().send(sender, "admin.list-header", Lang.ph("count", String.valueOf(all.size())));
        for (String code : all) {
            lang().send(sender, "admin.list-entry", Lang.ph(
                    "code", code,
                    "count", String.valueOf(codes.redemptionCount(code)),
                    "limit", limitText(code),
                    "status", lang().raw(statusKey(code)).replace("{date}", String.valueOf(codes.expiryDate(code)))));
        }
    }

    private String limitText(String code) {
        int max = codes.maxUses(code);
        return max > 0 ? String.valueOf(max) : "∞";
    }

    private String statusKey(String code) {
        if (codes.isExpired(code)) return "admin.status-expired";
        if (codes.isFull(code)) return "admin.status-full";
        return codes.expiryDate(code) == null ? "admin.status-active" : "admin.status-expires";
    }

    private net.kyori.adventure.text.Component statusRaw(String code) {
        return Text.parse(lang().raw(statusKey(code)).replace("{date}", String.valueOf(codes.expiryDate(code))));
    }

    private void setLimit(CommandSender sender, String[] args) {
        String code = existing(sender, args, "admin.usage-setlimit", 3);
        if (code == null) return;
        int limit;
        try {
            limit = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            limit = -1;
        }
        if (limit < 0) {
            lang().send(sender, "admin.invalid-number");
            return;
        }
        codes.setMaxUses(code, limit);
        lang().send(sender, "admin.limit-set", Lang.ph("code", code, "limit", limit == 0 ? "∞" : String.valueOf(limit)));
    }

    private void setExpiry(CommandSender sender, String[] args) {
        String code = existing(sender, args, "admin.usage-setexpiry", 3);
        if (code == null) return;
        String date = args[2];
        if (date.equalsIgnoreCase("never") || date.equalsIgnoreCase("none")) {
            codes.setExpiryDate(code, null);
            lang().send(sender, "admin.expiry-cleared", Lang.ph("code", code));
        } else if (CodeManager.parseDate(date) == null) {
            lang().send(sender, "admin.invalid-date");
        } else {
            codes.setExpiryDate(code, date);
            lang().send(sender, "admin.expiry-set", Lang.ph("code", code, "date", Text.escape(date)));
        }
    }

    private void addReward(CommandSender sender, String[] args) {
        if (args.length < 3) {
            lang().send(sender, "admin.usage-addreward");
            return;
        }
        String code = codes.resolve(args[1]);
        if (code == null) {
            lang().send(sender, "admin.code-not-found", Lang.ph("code", Text.escape(args[1])));
            return;
        }
        String reward = String.join(" ", List.of(args).subList(2, args.length));
        codes.addReward(code, reward);
        lang().send(sender, "admin.reward-added", Lang.ph("code", code, "reward", Text.escape(reward)));
    }

    private void removeReward(CommandSender sender, String[] args) {
        String code = existing(sender, args, "admin.usage-removereward", 3);
        if (code == null) return;
        int size = codes.rewards(code).size();
        int number;
        try {
            number = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            number = -1;
        }
        if (number < 1 || number > size) {
            lang().send(sender, "admin.invalid-index", Lang.ph("max", String.valueOf(size)));
            return;
        }
        String removed = codes.removeReward(code, number - 1);
        lang().send(sender, "admin.reward-removed", Lang.ph("code", code, "reward", Text.escape(removed)));
    }

    private void reset(CommandSender sender, String[] args) {
        String code = existing(sender, args, "admin.usage-reset", 2);
        if (code == null) return;
        int count = codes.resetRedemptions(code);
        lang().send(sender, "admin.reset-done", Lang.ph("code", code, "count", String.valueOf(count)));
    }

    private void help(CommandSender sender) {
        lang().send(sender, "admin.help-header");
        for (String[] line : HELP) {
            lang().send(sender, "admin.help-line",
                    Lang.ph("usage", Text.escape(line[0]), "description", line[1]));
        }
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        if (!stack.getSender().hasPermission("acoder.admin")) {
            return List.of();
        }
        String[] a = CommandUtil.completionArgs(args);
        String typed = a[a.length - 1];
        if (a.length == 1) {
            return CommandUtil.filter(SUBCOMMANDS, typed);
        }
        String sub = a[0].toLowerCase(Locale.ROOT);
        if (a.length == 2) {
            return switch (sub) {
                case "delete", "info", "setlimit", "setexpiry", "addreward", "removereward", "reset" ->
                        CommandUtil.filter(codes.all(), typed);
                default -> List.of();
            };
        }
        if (a.length == 3) {
            return switch (sub) {
                case "setlimit" -> CommandUtil.filter(List.of("0", "50", "100"), typed);
                case "setexpiry" -> CommandUtil.filter(List.of("never"), typed);
                case "addreward" -> CommandUtil.filter(List.of("[message]", "[broadcast]", "[player]", "give %player_name% "), typed);
                case "removereward" -> {
                    String code = codes.resolve(a[1]);
                    int size = code == null ? 0 : codes.rewards(code).size();
                    yield CommandUtil.filter(java.util.stream.IntStream.rangeClosed(1, size)
                            .mapToObj(String::valueOf).toList(), typed);
                }
                default -> List.of();
            };
        }
        return List.of();
    }
}
