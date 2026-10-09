package io.github.AzmiiD.aCoder.managers;

import io.github.AzmiiD.aCoder.ACoder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Map;

/**
 * Builds chat messages from config.yml.
 * Supports legacy colors (&a) and hex colors (&#RRGGBB).
 */
public class Messages {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .build();

    private final ACoder plugin;

    public Messages(ACoder plugin) {
        this.plugin = plugin;
    }

    /** A message from the "messages" section, with the prefix in front. */
    public Component get(String key) {
        return get(key, Map.of());
    }

    public Component get(String key, Map<String, String> placeholders) {
        String text = plugin.getConfig().getString("messages." + key, "&cMissing message: " + key);
        for (var entry : placeholders.entrySet()) {
            text = text.replace(entry.getKey(), entry.getValue());
        }
        return prefixed(text);
    }

    /** Any text (legacy/hex colors allowed), with the prefix in front. */
    public Component prefixed(String text) {
        return color(plugin.getConfig().getString("prefix", "&6[ACoder]&r") + " " + text);
    }

    public Component color(String text) {
        return LEGACY.deserialize(text);
    }
}
