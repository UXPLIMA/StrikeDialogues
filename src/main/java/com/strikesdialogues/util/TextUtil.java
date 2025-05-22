package com.strikesdialogues.util;

import com.strikesdialogues.config.ConfigManager;
import com.strikesdialogues.StrikesDialogues;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextUtil {

    private static MiniMessage miniMessageInstance = null;
    private static boolean papiHooked = false;
    private static ConfigManager configManager = null;
    private static Logger logger = null;

    private static final LegacyComponentSerializer LEGACY_SECTION_SERIALIZER = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();
    private static final LegacyComponentSerializer LEGACY_AMPERSAND_SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();
    private static final Map<Character, String> legacyToMiniMap = new HashMap<>();
    static {
        legacyToMiniMap.put('0', "black");
        legacyToMiniMap.put('1', "dark_blue");
        legacyToMiniMap.put('2', "dark_green"); legacyToMiniMap.put('3', "dark_aqua");
        legacyToMiniMap.put('4', "dark_red"); legacyToMiniMap.put('5', "dark_purple");
        legacyToMiniMap.put('6', "gold"); legacyToMiniMap.put('7', "gray");
        legacyToMiniMap.put('8', "dark_gray"); legacyToMiniMap.put('9', "blue");
        legacyToMiniMap.put('a', "green");
        legacyToMiniMap.put('b', "aqua");
        legacyToMiniMap.put('c', "red"); legacyToMiniMap.put('d', "light_purple");
        legacyToMiniMap.put('e', "yellow"); legacyToMiniMap.put('f', "white");
        legacyToMiniMap.put('k', "obfuscated"); legacyToMiniMap.put('l', "bold");
        legacyToMiniMap.put('m', "strikethrough");
        legacyToMiniMap.put('n', "underline");
        legacyToMiniMap.put('o', "italic");
        legacyToMiniMap.put('r', "reset");
    }

    private static final Pattern LEGACY_PATTERN = Pattern.compile("&([0-9a-fk-orA-FK-ORnmNM])|(&#([0-9a-fA-F]{6}))|(&[xX])");

    public static void initialize(StrikesDialogues plugin) {
        logger = plugin.getLogger();
        miniMessageInstance = MiniMessage.builder()
                .tags(net.kyori.adventure.text.minimessage.tag.resolver.TagResolver.standard())
                .build();
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            papiHooked = true;
        } else {
            papiHooked = false;
        }
        configManager = plugin.getConfigManager();
        if (configManager == null) {
            logger.severe("TextUtil could not get ConfigManager instance!");
        }
    }

    private static String preprocessLegacyCodes(String text) {
        if (text == null || text.isEmpty() || text.indexOf('&') == -1) {
            return text;
        }

        Matcher matcher = LEGACY_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String legacyCode = matcher.group(1);
            String hexGroup = matcher.group(2);
            String hexValue = matcher.group(3);
            String xCode = matcher.group(4);

            String replacement = null;
            if (hexGroup != null && hexValue != null) {
                replacement = "<#" + hexValue + ">";
            } else if (legacyCode != null) {
                char codeChar = legacyCode.toLowerCase().charAt(0);
                if (legacyToMiniMap.containsKey(codeChar)) {
                    replacement = "<" + legacyToMiniMap.get(codeChar) + ">";
                }
            } else if (xCode != null) {
            }

            if (replacement != null) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            } else {
                matcher.appendReplacement(sb, matcher.group(0));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }


    public static Component parseText(Player player, String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        if (logger == null && StrikesDialogues.getInstance() != null) {
            logger = StrikesDialogues.getInstance().getLogger();
        }
        if (configManager == null || miniMessageInstance == null) {
            if (logger != null) logger.severe("TextUtil cannot parse text, ConfigManager or MiniMessage is null!");
            return LegacyComponentSerializer.legacySection().deserialize(text.replace('&', '§'));
        }

        String processedText = text;
        if (papiHooked && player != null && configManager.isPapiEnabled()) {
            try {
                processedText = PlaceholderAPI.setPlaceholders(player, processedText);
            } catch (Exception e) {
                if (logger != null) logger.log(Level.WARNING, "Error applying PAPI placeholders for text: " + text, e);
            }
        }

        processedText = processedText.replace('§', '&');
        String textWithMiniTags = preprocessLegacyCodes(processedText);
        try {
            return miniMessageInstance.deserialize(textWithMiniTags);
        } catch (Exception mmException) {
            if (logger != null) {
                logger.log(Level.SEVERE, "Failed parsing text with MiniMessage: \"" + textWithMiniTags + "\". Original after PAPI: \"" + processedText + "\"", mmException);
            }
            try {
                return LegacyComponentSerializer.legacySection().deserialize(processedText.replace('&','§'));
            } catch (Exception legacyFallbackException) {
                if (logger != null) {
                    logger.log(Level.SEVERE, "Legacy fallback ALSO failed for text: \"" + processedText + "\"", legacyFallbackException);
                }
                return Component.text(processedText);
            }
        }
    }

    public static Component parseText(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        if (logger == null && StrikesDialogues.getInstance() != null) {
            logger = StrikesDialogues.getInstance().getLogger();
        }
        if (configManager == null || miniMessageInstance == null) {
            if (logger != null) logger.severe("TextUtil cannot parse text, ConfigManager or MiniMessage is null!");
            return LegacyComponentSerializer.legacySection().deserialize(text.replace('&', '§'));
        }

        String processedText = text.replace('§', '&');
        String textWithMiniTags = preprocessLegacyCodes(processedText);
        try {
            return miniMessageInstance.deserialize(textWithMiniTags);
        } catch (Exception mmException) {
            if (logger != null) {
                logger.log(Level.SEVERE, "Failed parsing text with MiniMessage (no player): \"" + textWithMiniTags + "\". Original: \"" + text + "\"", mmException);
            }
            try {
                return LegacyComponentSerializer.legacySection().deserialize(processedText.replace('&','§'));
            } catch (Exception legacyFallbackException) {
                if (logger != null) {
                    logger.log(Level.SEVERE, "Legacy fallback ALSO failed for text (no player): \"" + processedText + "\"", legacyFallbackException);
                }
                return Component.text(processedText);
            }
        }
    }

    public static String toLegacyWithSections(Component component) {
        if (component == null) return "";
        return LEGACY_SECTION_SERIALIZER.serialize(component);
    }

    public static String toLegacy(Component component) {
        if (component == null) return "";
        return LEGACY_AMPERSAND_SERIALIZER.serialize(component);
    }
}