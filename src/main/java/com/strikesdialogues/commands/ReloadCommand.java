package com.strikesdialogues.commands;

import com.strikesdialogues.StrikesDialogues;
import com.strikesdialogues.config.ConfigManager;
import com.strikesdialogues.util.TextUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import java.util.Set; // Import Set

public class ReloadCommand implements CommandExecutor {

    private final StrikesDialogues plugin;
    private final ConfigManager configManager;

    public ReloadCommand(StrikesDialogues plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("strikesdialogues.reload")) {
            sender.sendMessage(TextUtil.parseText(configManager.getMsgNoPermission()));
            return true;
        }

        try {
            configManager.reloadConfig();
            sender.sendMessage(TextUtil.parseText(configManager.getMsgReloadSuccess()));

            Set<String> invalidPaths = configManager.getInvalidPathsFound();
            if (!invalidPaths.isEmpty()) {
                plugin.getLogger().warning("Config contains forbidden format codes or unsupported characters (like emojis/symbols). This may cause display issues. Check paths below:");
                sender.sendMessage(TextUtil.parseText("&cWarning: Config contains forbidden format codes or unsupported characters. This may cause display issues. If you need help regarding this issue, contact us immediately."));
                sender.sendMessage(TextUtil.parseText("&eProblematic configuration paths found:"));
                // List each invalid path found
                for (String path : invalidPaths) {
                    sender.sendMessage(TextUtil.parseText("&e - " + path));
                    plugin.getLogger().warning("  - " + path); // Also log path detail to console
                }
            }

        } catch (Exception e) {
            sender.sendMessage(TextUtil.parseText(configManager.getMsgReloadFail()));
            plugin.getLogger().severe("Error during configuration reload: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }
}