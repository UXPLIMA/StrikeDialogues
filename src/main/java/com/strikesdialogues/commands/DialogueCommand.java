package com.strikesdialogues.commands;

import com.strikesdialogues.StrikesDialogues;
import com.strikesdialogues.config.ConfigManager;
import com.strikesdialogues.dialogue.Dialogue;
import com.strikesdialogues.dialogue.DialogueManager;
import com.strikesdialogues.dialogue.DialoguePage;
import com.strikesdialogues.util.CharacterMapper;
import com.strikesdialogues.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DialogueCommand implements CommandExecutor, TabCompleter {

    private final StrikesDialogues plugin;
    private final ConfigManager configManager;
    private final DialogueManager dialogueManager;
    private final Logger logger;

    public DialogueCommand(StrikesDialogues plugin, ConfigManager configManager, DialogueManager dialogueManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.dialogueManager = dialogueManager;
        this.logger = plugin.getLogger();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        if ("custom".equals(subCommand)) {
            handleCustomCommand(sender, label, args);
        } else {
            handlePredefinedCommand(sender, label, args);
        }

        return true;
    }

    private void handleCustomCommand(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(TextUtil.parseText(configManager.getMsgPlayerOnly()));
            return;
        }
        if (!player.hasPermission("strikesdialogues.custom")) {
            player.sendMessage(TextUtil.parseText(player, configManager.getMsgNoPermission()));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(TextUtil.parseText(player, "&cUsage: /" + label + " custom <line1>,[line2],[line3],[line4]"));
            return;
        }

        String fullMessageInput = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        String[] linesRaw = fullMessageInput.split(",");
        int maxLines = configManager.getCustomCommandMaxLines();

        if (linesRaw.length > maxLines) {
            player.sendMessage(TextUtil.parseText(player, configManager.getMsgCustomLineLimit()));
            return;
        }

        List<String> textLinesOnly = new ArrayList<>();
        for (int i = 0; i < linesRaw.length; i++) {
            if (i >= maxLines) break;
            String currentRawLine = linesRaw[i].trim();
            textLinesOnly.add(currentRawLine);
        }

        textLinesOnly.removeIf(String::isBlank);
        if (textLinesOnly.isEmpty()) {
            player.sendMessage(TextUtil.parseText(player,"&cCannot start dialogue with empty message content."));
            return;
        }

        BossBar customBackgroundBar = null;
        String backgroundCharRaw = configManager.getCustomCommandBackgroundChar();
        if (backgroundCharRaw != null && !backgroundCharRaw.isBlank()) {
            try {
                Component bgComponent = TextUtil.parseText(player, backgroundCharRaw);
                String bgLegacyString = TextUtil.toLegacyWithSections(bgComponent);
                String bgTranslatedString = CharacterMapper.translate(bgLegacyString);
                BarColor bgColor = configManager.getDefaultBarColor();
                BarStyle bgStyle = configManager.getDefaultBarStyle();
                customBackgroundBar = Bukkit.createBossBar(bgTranslatedString, bgColor, bgStyle);
                customBackgroundBar.setProgress(1.0);
                customBackgroundBar.addPlayer(player);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to create temporary background bar for /sd custom", e);
                customBackgroundBar = null;
            }
        }

        DialoguePage customPage = new DialoguePage(textLinesOnly);
        String customDialogueId = "custom-" + player.getUniqueId().toString();

        if (customPage.getRawLines() == null || customPage.getRawLines().isEmpty()) {
            logger.warning("DialoguePage created with empty or null text lines for /sd custom command!");
            if (customBackgroundBar != null) {
                customBackgroundBar.setVisible(false);
                customBackgroundBar.removePlayer(player);
            }
            return;
        }

        Dialogue customDialogue = new Dialogue(
                customDialogueId,
                Collections.singletonList(customPage),
                Dialogue.DialogueSettings.EMPTY,
                Collections.emptyList()
        );

        dialogueManager.startDialogue(player, customDialogue, customBackgroundBar);
    }

    private void handlePredefinedCommand(CommandSender sender, String label, String[] args) {
        String dialogueId = args[0];
        Player targetPlayer = null;

        if (args.length > 1) {
            if (!sender.hasPermission("strikesdialogues.start.others")) {
                sender.sendMessage(TextUtil.parseText(configManager.getMsgNoPermission()));
                return;
            }
            targetPlayer = Bukkit.getPlayerExact(args[1]);
            if (targetPlayer == null) {
                sender.sendMessage(TextUtil.parseText("&cPlayer '" + args[1] + "' not found."));
                return;
            }
        } else if (sender instanceof Player) {
            targetPlayer = (Player) sender;
        } else {
            sender.sendMessage(TextUtil.parseText(configManager.getMsgPlayerOnly() + " or specify a player name."));
            return;
        }

        if (!sender.hasPermission("strikesdialogues.use")) {
            sender.sendMessage(TextUtil.parseText(configManager.getMsgNoPermission()));
            return;
        }

        Optional<Dialogue> dialogueOpt = configManager.getDialogue(dialogueId);
        if (dialogueOpt.isEmpty()) {
            sender.sendMessage(TextUtil.parseText(configManager.getMsgDialogueNotFound(dialogueId)));
            return;
        }

        dialogueManager.startDialogue(targetPlayer, dialogueOpt.get(), null);
    }

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(TextUtil.parseText("&eStrikesDialogues Usage:"));
        sender.sendMessage(TextUtil.parseText("&a/" + label + " <dialogueId> [player] &7- Start pre-defined dialogue."));
        sender.sendMessage(TextUtil.parseText("&a/" + label + " custom <line1>,[line2],... &7- Show live dialogue (Max " + configManager.getCustomCommandMaxLines() + ")."));
        if (sender.hasPermission("strikesdialogues.reload")) {
            sender.sendMessage(TextUtil.parseText("&a/sdreload &7- Reload configuration."));
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            configManager.getLoadedDialogueIds().stream()
                    .filter(id -> id.startsWith(input))
                    .forEach(completions::add);
            if ("custom".startsWith(input) && sender.hasPermission("strikesdialogues.custom")) {
                completions.add("custom");
            }
            return completions;
        } else if (args.length == 2) {
            if (!"custom".equalsIgnoreCase(args[0])) {
                if(sender.hasPermission("strikesdialogues.start.others")) {
                    String input = args[1].toLowerCase();
                    Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase().startsWith(input))
                            .forEach(completions::add);
                    return completions;
                }
            }
        }
        return Collections.emptyList();
    }
}