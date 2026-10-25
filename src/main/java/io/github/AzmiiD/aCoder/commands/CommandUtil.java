package io.github.AzmiiD.aCoder.commands;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

final class CommandUtil {

    private CommandUtil() {
    }

    /** The /execute-as target if there is one, otherwise whoever ran the command. */
    static CommandSender actor(CommandSourceStack stack) {
        return stack.getExecutor() instanceof Player player ? player : stack.getSender();
    }

    static String[] completionArgs(String[] args) {
        return args.length == 0 ? new String[]{""} : args;
    }

    static List<String> filter(List<String> options, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(p)).distinct().toList();
    }
}
