package io.github.AzmiiD.aCoder.commands;

import io.github.AzmiiD.aCoder.ACoder;
import io.github.AzmiiD.aCoder.config.Lang;
import io.github.AzmiiD.aCoder.managers.CodeManager;
import io.github.AzmiiD.aCoder.managers.RewardRunner;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RedeemCommand implements BasicCommand {

    private final ACoder plugin;
    private final CodeManager codes;
    private final RewardRunner rewards;
    private final Map<UUID, Integer> failures = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lockedUntil = new ConcurrentHashMap<>();

    public RedeemCommand(ACoder plugin, CodeManager codes, RewardRunner rewards) {
        this.plugin = plugin;
        this.codes = codes;
        this.rewards = rewards;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        CommandSender sender = CommandUtil.actor(stack);
        Lang lang = plugin.lang();
        if (!(sender instanceof Player player)) {
            lang.send(sender, "player-only");
            return;
        }
        if (!player.hasPermission("acoder.redeem")) {
            lang.send(player, "no-permission");
            return;
        }
        if (args.length != 1) {
            lang.send(player, "redeem.usage");
            return;
        }
        long wait = lockoutSeconds(player.getUniqueId());
        if (wait > 0) {
            lang.send(player, "redeem.locked", Lang.ph("seconds", String.valueOf(wait)));
            return;
        }

        String code = codes.resolve(args[0]);
        if (code == null) {
            registerFailure(player.getUniqueId());
            lang.send(player, "redeem.invalid");
            return;
        }
        FileConfiguration config = plugin.getConfig();
        if (codes.isExpired(code)) {
            lang.send(player, "redeem.expired");
        } else if (codes.isFull(code)) {
            lang.send(player, "redeem.limit-reached");
        } else if (codes.hasRedeemed(code, player.getUniqueId(), player.getName(), ipOf(player),
                config.getBoolean("redeem.block-same-ip", true))) {
            lang.send(player, "redeem.used");
        } else {
            failures.remove(player.getUniqueId());
            // recorded before the rewards run, so a failing reward can never be claimed twice
            codes.addRedemption(code, player.getUniqueId(), ipOf(player));
            rewards.run(player, code, codes.rewards(code));
            lang.send(player, "redeem.success");
            playSuccessSound(player, config.getString("redeem.success-sound", ""));
        }
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        return List.of();
    }

    private void registerFailure(UUID id) {
        int max = plugin.getConfig().getInt("redeem.max-failed-attempts", 5);
        if (max <= 0) {
            return;
        }
        if (failures.merge(id, 1, Integer::sum) >= max) {
            failures.remove(id);
            long seconds = Math.max(1, plugin.getConfig().getLong("redeem.lockout-seconds", 60));
            lockedUntil.put(id, System.currentTimeMillis() + seconds * 1000);
        }
    }

    /** Seconds left of a lockout, 0 if the player is free to try. */
    private long lockoutSeconds(UUID id) {
        Long until = lockedUntil.get(id);
        if (until == null) {
            return 0;
        }
        long left = until - System.currentTimeMillis();
        if (left <= 0) {
            lockedUntil.remove(id);
            return 0;
        }
        return (left + 999) / 1000;
    }

    private static void playSuccessSound(Player player, String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        NamespacedKey namespaced = NamespacedKey.fromString(key.toLowerCase());
        Sound sound = namespaced == null ? null : Registry.SOUNDS.get(namespaced);
        if (sound != null) {
            player.playSound(player.getLocation(), sound, 1f, 1f);
        }
    }

    private static String ipOf(Player player) {
        var address = player.getAddress();
        return address == null ? "unknown" : address.getAddress().getHostAddress();
    }
}
