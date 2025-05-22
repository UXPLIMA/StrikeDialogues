package com.strikesdialogues.util;

import com.strikesdialogues.StrikesDialogues;
import com.strikesdialogues.config.ConfigManager;
import com.strikesdialogues.dialogue.DialogueAction;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ActionExecutor {

    private static StrikesDialogues plugin = null;
    private static ConfigManager configManager = null;
    private static boolean papiHooked = false;
    private static final Pattern SOUND_OPTIONS_PATTERN = Pattern.compile("\\s*(pitch|volume):([\\d.]+)\\s*", Pattern.CASE_INSENSITIVE);

    public static void initialize(StrikesDialogues pluginInstance) {
        plugin = pluginInstance;
        configManager = pluginInstance.getConfigManager();
        papiHooked = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        if (configManager == null) {
            plugin.getLogger().severe("ActionExecutor could not get ConfigManager instance!");
        }
    }

    public static void executeActions(Player player, List<DialogueAction> actions) {
        if (actions == null || actions.isEmpty() || player == null || !player.isOnline()) return;
        if (plugin == null || configManager == null) { System.err.println("[StrikesDialogues] ActionExecutor not initialized!"); return; }

        for (DialogueAction action : actions) {
            String argument = action.getArgument();

            if (papiHooked && configManager.isPapiEnabled()) {
                try {
                    argument = PlaceholderAPI.setPlaceholders(player, argument);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error parsing PlaceholderAPI placeholders for action argument: " + argument, e);
                }
            }

            try {
                switch (action.getType()) {
                    case CONSOLE:
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), argument);
                        break;
                    case PLAYER:
                        player.performCommand(argument);
                        break;
                    case MESSAGE:
                        player.sendMessage(TextUtil.parseText(player, argument));
                        break;
                    case BROADCAST:
                        Bukkit.broadcast(TextUtil.parseText(player, argument));
                        break;
                    case SOUND:
                        executeSoundAction(player, argument);
                        break;
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error executing dialogue action for " + player.getName() + ": [" + action.getType() + "] " + action.getArgument(), e);
            }
        }
    }

    private static void executeSoundAction(Player player, String argument) {
        if (argument == null || argument.isBlank()) return;
        String soundKey = argument; float volume = 1.0f; float pitch = 1.0f;
        Matcher matcher = SOUND_OPTIONS_PATTERN.matcher(argument); StringBuffer sb = new StringBuffer();
        while (matcher.find()) { String key = matcher.group(1).toLowerCase(); try { float value = Float.parseFloat(matcher.group(2)); if ("volume".equals(key)) { volume = value; } else if ("pitch".equals(key)) { pitch = value; } } catch (NumberFormatException e) { plugin.getLogger().warning("Invalid number format for sound option " + key + ": " + matcher.group(2)); } matcher.appendReplacement(sb, ""); } matcher.appendTail(sb); soundKey = sb.toString().trim();
        if (soundKey.isEmpty()) { plugin.getLogger().warning("No sound key specified: " + argument); return; }
        try { player.playSound(player.getLocation(), soundKey, SoundCategory.MASTER, volume, pitch); }
        catch (Exception e) { plugin.getLogger().log(Level.SEVERE, "Could not play sound '" + soundKey + "' for player " + player.getName(), e); }
    }
}