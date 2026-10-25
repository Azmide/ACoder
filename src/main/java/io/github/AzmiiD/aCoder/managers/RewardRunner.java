package io.github.AzmiiD.aCoder.managers;

import io.github.AzmiiD.aCoder.ACoder;
import io.github.AzmiiD.aCoder.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;

/** Runs the reward lines of a code. A failing line is logged and never stops the others. */
public final class RewardRunner {

    private final ACoder plugin;

    public RewardRunner(ACoder plugin) {
        this.plugin = plugin;
    }

    public void run(Player player, String code, Iterable<String> rewards) {
        Map<String, String> placeholders = Map.of(
                "%player_name%", player.getName(),
                "{player}", player.getName(),
                "{uuid}", player.getUniqueId().toString());

        for (String reward : rewards) {
            String line = Text.replace(reward, placeholders).strip();
            try {
                if (line.regionMatches(true, 0, "[message]", 0, 9)) {
                    player.sendMessage(Text.parse(line.substring(9).strip()));
                } else if (line.regionMatches(true, 0, "[broadcast]", 0, 11)) {
                    Bukkit.broadcast(Text.parse(line.substring(11).strip()));
                } else if (line.regionMatches(true, 0, "[player]", 0, 8)) {
                    player.performCommand(stripSlash(line.substring(8)));
                } else {
                    String command = line.regionMatches(true, 0, "[console]", 0, 9) ? line.substring(9) : line;
                    if (!Bukkit.dispatchCommand(Bukkit.getConsoleSender(), stripSlash(command))) {
                        plugin.getLogger().warning("Reward of code " + code + " did not run: " + line);
                    }
                }
            } catch (RuntimeException e) {
                plugin.getLogger().warning("Reward of code " + code + " failed (" + line + "): " + e.getMessage());
            }
        }
    }

    private static String stripSlash(String command) {
        String trimmed = command.strip();
        return trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
    }
}
