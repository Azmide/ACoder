package io.github.AzmiiD.aCoder.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Parses MiniMessage text; legacy (&a) and hex (&#RRGGBB) codes are converted so old configs keep working. */
public final class Text {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final String COLOR_CODES = "0123456789abcdef";

    private Text() {
    }

    public static Component parse(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        return MM.deserialize(legacyToMini(input));
    }

    public static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    /** For admin-typed values put into messages, so they can't inject click/hover tags. */
    public static String escape(String input) {
        return input == null ? "" : MM.escapeTags(input);
    }

    public static String replace(String input, Map<String, String> placeholders) {
        if (input == null) {
            return "";
        }
        String out = input;
        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            out = out.replace(e.getKey(), e.getValue() == null ? "" : e.getValue());
        }
        return out;
    }

    static String legacyToMini(String in) {
        if (in.indexOf('&') < 0 && in.indexOf('§') < 0) {
            return in;
        }
        StringBuilder sb = new StringBuilder(in.length() + 16);
        Set<String> openDecorations = new LinkedHashSet<>();
        int len = in.length();
        for (int i = 0; i < len; i++) {
            char c = in.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < len) {
                char next = Character.toLowerCase(in.charAt(i + 1));
                if (next == '#' && isHex(in, i + 2)) { // &#rrggbb
                    closeDecorations(sb, openDecorations);
                    sb.append("<#").append(in, i + 2, i + 8).append('>');
                    i += 7;
                    continue;
                }
                if (COLOR_CODES.indexOf(next) >= 0) {
                    closeDecorations(sb, openDecorations);
                    sb.append('<').append(colorName(next)).append('>');
                    i++;
                    continue;
                }
                String deco = decorationTag(next);
                if (deco != null) {
                    sb.append('<').append(deco).append('>');
                    openDecorations.add(deco);
                    i++;
                    continue;
                }
                if (next == 'r') {
                    sb.append("<reset>");
                    openDecorations.clear();
                    i++;
                    continue;
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }

    // a legacy color code also resets decorations
    private static void closeDecorations(StringBuilder sb, Set<String> open) {
        for (String deco : open) {
            sb.append("<!").append(deco).append('>');
        }
        open.clear();
    }

    private static boolean isHex(String s, int start) {
        if (start + 6 > s.length()) {
            return false;
        }
        for (int i = start; i < start + 6; i++) {
            if (Character.digit(s.charAt(i), 16) < 0) {
                return false;
            }
        }
        return true;
    }

    private static String decorationTag(char c) {
        return switch (c) {
            case 'l' -> "bold";
            case 'o' -> "italic";
            case 'n' -> "underlined";
            case 'm' -> "strikethrough";
            case 'k' -> "obfuscated";
            default -> null;
        };
    }

    private static String colorName(char c) {
        return switch (c) {
            case '0' -> "black";
            case '1' -> "dark_blue";
            case '2' -> "dark_green";
            case '3' -> "dark_aqua";
            case '4' -> "dark_red";
            case '5' -> "dark_purple";
            case '6' -> "gold";
            case '7' -> "gray";
            case '8' -> "dark_gray";
            case '9' -> "blue";
            case 'a' -> "green";
            case 'b' -> "aqua";
            case 'c' -> "red";
            case 'd' -> "light_purple";
            case 'e' -> "yellow";
            default -> "white";
        };
    }
}
