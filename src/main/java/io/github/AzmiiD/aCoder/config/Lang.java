package io.github.AzmiiD.aCoder.config;

import io.github.AzmiiD.aCoder.util.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Messages from lang.yml; keys missing from the server's copy fall back to the bundled one. */
public final class Lang {

    private FileConfiguration file = new YamlConfiguration();
    private FileConfiguration defaults = new YamlConfiguration();
    private String prefix = "";

    public void load(File langFile, InputStream bundled, String prefix) {
        this.prefix = prefix;
        this.file = YamlConfiguration.loadConfiguration(langFile);
        this.defaults = bundled == null ? new YamlConfiguration()
                : YamlConfiguration.loadConfiguration(new InputStreamReader(bundled, StandardCharsets.UTF_8));
    }

    public static Map<String, String> ph(String... pairs) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put("{" + pairs[i] + "}", pairs[i + 1]);
        }
        return map;
    }

    public String raw(String key) {
        String value = file.getString(key);
        if (value == null) {
            value = defaults.getString(key, key);
        }
        return value.replace("{prefix}", prefix);
    }

    public void send(CommandSender to, String key, Map<String, String> placeholders) {
        String raw = raw(key);
        if (raw.isBlank()) {
            return;
        }
        to.sendMessage(Text.parse(Text.replace(raw, placeholders)));
    }

    public void send(CommandSender to, String key) {
        send(to, key, Map.of());
    }
}
