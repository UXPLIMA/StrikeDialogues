package com.strikesdialogues.dialogue;

import com.strikesdialogues.StrikesDialogues;
import com.strikesdialogues.config.ConfigManager;
import com.strikesdialogues.listeners.MovementListener;
import com.strikesdialogues.util.ActionExecutor;
import com.strikesdialogues.util.CharacterMapper;
import com.strikesdialogues.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DialogueManager {

    private final StrikesDialogues plugin;
    private final ConfigManager configManager;
    private final Logger logger;
    private final Map<UUID, CurrentDialogueState> activeDialogues = new HashMap<>();
    private final Map<UUID, Long> dialogueCooldowns = new HashMap<>();

    private static class CurrentDialogueState {
        final Dialogue dialogue;
        int currentPageIndex;
        final List<BossBar> currentBars;
        final BossBar backgroundBar;
        BukkitTask currentBukkitTask;
        CurrentDialogueState(Dialogue dialogue, int pageIndex, List<BossBar> pageBars, @Nullable BossBar backgroundBar, BukkitTask bukkitTask) {
            this.dialogue = dialogue;
            this.currentPageIndex = pageIndex;
            this.currentBars = pageBars;
            this.backgroundBar = backgroundBar;
            this.currentBukkitTask = bukkitTask;
        }
    }

    public DialogueManager(StrikesDialogues plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.logger = plugin.getLogger();
        if (this.configManager == null) {
            logger.severe("DialogueManager could not get ConfigManager instance during initialization!");
        }
    }

    public void startDialogue(Player player, Dialogue dialogue) {
        startDialogue(player, dialogue, null);
    }

    public void startDialogue(Player player, Dialogue dialogue, @Nullable BossBar potentialBackgroundBar) {
        if (player == null || !player.isOnline()) {
            return;
        }

        boolean onGround = player.isOnGround() || player.getVelocity().getY() > -0.05 && player.getVelocity().getY() < 0.05;
        if (!onGround) {
            player.sendMessage(TextUtil.parseText(player, configManager.getMsgMustBeOnGround()));
            if (potentialBackgroundBar != null) {
                potentialBackgroundBar.setVisible(false);
                try { potentialBackgroundBar.removePlayer(player); } catch (IllegalArgumentException ignored) {}
            }
            return;
        }

        if (dialogue == null || !dialogue.isValid()) {
            if (potentialBackgroundBar != null) {
                potentialBackgroundBar.setVisible(false);
                try { potentialBackgroundBar.removePlayer(player); } catch (IllegalArgumentException ignored) {}
            }
            return;
        }

        UUID playerUUID = player.getUniqueId();

        if (activeDialogues.containsKey(playerUUID)) {
            if (potentialBackgroundBar != null) {
                potentialBackgroundBar.setVisible(false);
                try { potentialBackgroundBar.removePlayer(player); } catch (IllegalArgumentException ignored) {}
            }
            return;
        }

        BossBar finalBackgroundBar = null;
        boolean isCustom = dialogue.getId().startsWith("custom-");
        if (isCustom) {
            finalBackgroundBar = potentialBackgroundBar;
        } else {
            String persistentLineRaw = dialogue.getSettings().getPersistentBackgroundLine()
                    .orElse(configManager.getGlobalPersistentBackgroundLine());
            if (persistentLineRaw != null && !persistentLineRaw.isBlank()) {
                try {
                    Component bgComponent = TextUtil.parseText(player, persistentLineRaw);
                    String bgLegacyString = TextUtil.toLegacyWithSections(bgComponent);
                    String bgTranslatedString = CharacterMapper.translate(bgLegacyString);
                    BarColor bgColor = configManager.getDefaultBarColor();
                    BarStyle bgStyle = configManager.getDefaultBarStyle();
                    finalBackgroundBar = Bukkit.createBossBar(bgTranslatedString, bgColor, bgStyle);
                    finalBackgroundBar.setProgress(1.0);
                    finalBackgroundBar.addPlayer(player);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Failed to create persistent background bar for dialogue " + dialogue.getId(), e);
                    finalBackgroundBar = null;
                }
            }
        }

        try {
            Dialogue.DialogueSettings settings = dialogue.getSettings();
            String startSoundKey = settings.getStartSound().orElse(configManager.getDefaultStartSound().orElse(null));

            if (startSoundKey != null && !startSoundKey.isBlank()) {
                float volume = settings.getStartVolume().orElse(configManager.getDefaultStartVolume());
                float pitch = settings.getStartPitch().orElse(configManager.getDefaultStartPitch());
                player.playSound(player.getLocation(), startSoundKey, volume, pitch);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception caught while trying to play start sound for dialogue " + dialogue.getId(), e);
        }

        displayPage(player, dialogue, 0, finalBackgroundBar);
    }

    private void displayPage(Player player, Dialogue dialogue, int pageIndex, @Nullable BossBar backgroundBar) {
        UUID playerUUID = player.getUniqueId();
        if (!player.isOnline()) {
            stopDialogue(null, playerUUID, 0L); return;
        }

        boolean pageExists = pageIndex >= 0 && pageIndex < dialogue.getPages().size();
        DialoguePage page = pageExists ? dialogue.getPages().get(pageIndex) : new DialoguePage(Collections.emptyList());
        Dialogue.DialogueSettings settings = dialogue.getSettings();
        long animationDelay = settings.getAnimationDelayTicks().orElseGet(configManager::getDefaultAnimationDelayTicks);
        BarColor barColor = settings.getBarColor().orElseGet(configManager::getDefaultBarColor);
        BarStyle barStyle = settings.getBarStyle().orElseGet(configManager::getDefaultBarStyle);
        String typingSound = settings.getTypingSound().orElse(configManager.getDefaultTypingSound().orElse(null));
        float typingVolume = settings.getTypingVolume().orElseGet(configManager::getDefaultTypingVolume);
        float typingPitch = settings.getTypingPitch().orElseGet(configManager::getDefaultTypingPitch);
        long holdTime = configManager.getDefaultHoldTimeTicks();

        List<String> rawLines = page.getRawLines();
        int numLines = rawLines.size();
        List<BossBar> pageBars = new ArrayList<>();
        for (int i = 0; i < numLines; i++) {
            BossBar bar = Bukkit.createBossBar("", barColor, barStyle);
            bar.setProgress(0);
            bar.addPlayer(player);
            pageBars.add(bar);
        }

        PageDisplayTask taskRunnable = new PageDisplayTask(playerUUID, dialogue, pageIndex, rawLines, pageBars, holdTime, typingSound, typingVolume, typingPitch);
        BukkitTask bukkitTask = taskRunnable.runTaskTimer(plugin, 0L, animationDelay);

        activeDialogues.put(playerUUID, new CurrentDialogueState(dialogue, pageIndex, pageBars, backgroundBar, bukkitTask));
    }

    private void moveToNextPageOrEnd(Player player, Dialogue dialogue, int finishedPageIndex, long holdTimeTicks) {
        UUID playerUUID = player.getUniqueId();
        CurrentDialogueState state = activeDialogues.get(playerUUID);

        if (state == null || state.dialogue != dialogue || state.currentPageIndex != finishedPageIndex) {
            return;
        }

        final BossBar backgroundBarToPass = state.backgroundBar;
        final List<BossBar> barsToClean = new ArrayList<>(state.currentBars);
        int nextPage = finishedPageIndex + 1;

        if (nextPage < dialogue.getPages().size()) {
            new BukkitRunnable() {
                @Override public void run() {
                    Player pOnline = Bukkit.getPlayer(playerUUID);
                    if (pOnline != null && pOnline.isOnline()) {
                        CurrentDialogueState currentStateCheck = activeDialogues.get(playerUUID);
                        if (currentStateCheck == state) {
                            cleanupBars(playerUUID, barsToClean);
                            displayPage(pOnline, dialogue, nextPage, backgroundBarToPass);
                        }
                    } else {
                        stopDialogue(null, playerUUID, 0L);
                    }
                }
            }.runTaskLater(plugin, holdTimeTicks);
        } else {
            long finalHoldTime = configManager.getDefaultHoldTimeTicks();
            long actionDelay = configManager.getEndActionDelayTicks();
            List<DialogueAction> endActions = dialogue.getEndActions();

            new BukkitRunnable() {
                @Override public void run() {
                    Player pOnline = Bukkit.getPlayer(playerUUID);
                    CurrentDialogueState currentStateCheck = activeDialogues.get(playerUUID);
                    if (currentStateCheck == state) {
                        stopDialogue(pOnline, playerUUID, 0L);
                    }
                }
            }.runTaskLater(plugin, finalHoldTime);

            if (endActions != null && !endActions.isEmpty()) {
                new BukkitRunnable() {
                    @Override public void run() {
                        Player pOnline = Bukkit.getPlayer(playerUUID);
                        if (pOnline != null && pOnline.isOnline()) {
                            ActionExecutor.executeActions(pOnline, endActions);
                        }
                    }
                }.runTaskLater(plugin, finalHoldTime + actionDelay);
            }
        }
    }

    public void stopDialogue(Player player) { stopDialogue(player, (player != null) ? player.getUniqueId() : null, configManager.getDefaultHoldTimeTicks()); }
    public void stopDialogue(Player player, UUID uuid) { stopDialogue(player, uuid, configManager.getDefaultHoldTimeTicks()); }
    public void stopDialogue(Player player, UUID uuid, long cleanupDelayTicks) {

        if (uuid == null) return;

        CurrentDialogueState state = activeDialogues.get(uuid);
        Player pCheck = (player != null && player.isOnline()) ? player : Bukkit.getPlayer(uuid);

        if (state == null) {
            if (pCheck != null) MovementListener.unrestrictPlayer(pCheck);
            else MovementListener.removeRestriction(uuid);
            return;
        }

        if (state.currentBukkitTask != null && !state.currentBukkitTask.isCancelled()) {
            state.currentBukkitTask.cancel();
            state.currentBukkitTask = null;
        }

        if (cleanupDelayTicks > 0) {
            activeDialogues.remove(uuid);

            new BukkitRunnable() {
                @Override
                public void run() {
                    Player playerOnlineCheck = Bukkit.getPlayer(uuid);
                    cleanupBars(uuid, state.currentBars);
                    if (state.backgroundBar != null) {
                        state.backgroundBar.setVisible(false);
                        if (playerOnlineCheck != null && playerOnlineCheck.isOnline()) {
                            try { state.backgroundBar.removePlayer(playerOnlineCheck); } catch (IllegalArgumentException ignored) {}
                        } else {
                            state.backgroundBar.removeAll();
                        }
                    }
                    if (playerOnlineCheck != null && playerOnlineCheck.isOnline()) {
                        MovementListener.unrestrictPlayer(playerOnlineCheck);
                    } else {
                        MovementListener.removeRestriction(uuid);
                    }
                }
            }.runTaskLater(plugin, cleanupDelayTicks);
        } else {
            activeDialogues.remove(uuid);
            cleanupBars(uuid, state.currentBars);
            if (state.backgroundBar != null) {
                state.backgroundBar.setVisible(false);
                if (pCheck != null && pCheck.isOnline()) {
                    try { state.backgroundBar.removePlayer(pCheck); } catch (IllegalArgumentException ignored) {}
                } else {
                    state.backgroundBar.removeAll();
                }
            }
            if (pCheck != null && pCheck.isOnline()) {
                MovementListener.unrestrictPlayer(pCheck);
            } else {
                MovementListener.removeRestriction(uuid);
            }
        }
    }

    private void cleanupBars(UUID uuid, List<BossBar> barsToClean) {
        if (barsToClean != null && !barsToClean.isEmpty()) {
            Player pOnline = Bukkit.getPlayer(uuid);
            for (BossBar bar : barsToClean) {
                bar.setVisible(false);
                if (pOnline != null && pOnline.isOnline()) {
                    try { bar.removePlayer(pOnline); } catch (IllegalArgumentException ignored) {}
                } else {
                    bar.removeAll();
                }
            }
        }
    }

    private class PageDisplayTask extends BukkitRunnable {
        private final UUID playerUUID;
        private final Dialogue dialogue;
        private final int pageIndex;
        private final List<String> pageTranslatedLines;
        private final List<BossBar> pageBars;
        private final long holdTimeTicks;
        private final String typingSoundKey;
        private final float soundVolume;
        private final float soundPitch;
        private int currentLineIndex = 0;
        private int charIndexInLine = 0;
        private final StringBuilder[] builtLines;
        private boolean restrictionApplied = false;
        private BukkitTask ownBukkitTask = null;

        public PageDisplayTask(UUID uuid, Dialogue dia, int pIndex, List<String> rawLns, List<BossBar> textBars,
                               long hold, String sound, float vol, float pitch) {
            this.playerUUID=uuid;
            this.dialogue=dia; this.pageIndex=pIndex; this.pageBars=textBars; this.holdTimeTicks=hold; this.typingSoundKey=sound; this.soundVolume=vol; this.soundPitch=pitch;

            Player p = Bukkit.getPlayer(playerUUID);
            pageTranslatedLines = new ArrayList<>();
            builtLines = new StringBuilder[rawLns.size()];
            for (int i=0; i < rawLns.size(); i++) {
                String rawLine=rawLns.get(i);
                Component componentLine = TextUtil.parseText(p, rawLine);
                String legacyString = TextUtil.toLegacyWithSections(componentLine);
                String translatedLine = CharacterMapper.translate(legacyString);
                this.pageTranslatedLines.add(translatedLine);
                this.builtLines[i]=new StringBuilder();
            }
        }

        @Override
        public synchronized BukkitTask runTaskTimer(org.bukkit.plugin.Plugin plugin, long delay, long period) throws IllegalArgumentException, IllegalStateException {
            this.ownBukkitTask = super.runTaskTimer(plugin, delay, period);
            return this.ownBukkitTask;
        }

        @Override
        public void run() {
            Player player = Bukkit.getPlayer(playerUUID);
            if (player == null || !player.isOnline()) {
                forceCancelCleanup(false);
                return;
            }

            CurrentDialogueState currentState = activeDialogues.get(playerUUID);
            if (currentState == null || currentState.currentBukkitTask == null || currentState.currentBukkitTask.getTaskId() != this.getTaskId()) {
                if (!isCancelled()) {
                    cancel();
                }
                return;
            }

            if (!restrictionApplied) {
                MovementListener.restrictPlayer(player);
                restrictionApplied = true;
            }

            if (pageTranslatedLines.isEmpty() || pageBars.isEmpty()) {
                moveToNextPageOrEnd(player, dialogue, pageIndex, holdTimeTicks);
                if (!isCancelled()) cancel();
                return;
            }

            if (currentLineIndex >= pageTranslatedLines.size()) {
                moveToNextPageOrEnd(player, dialogue, pageIndex, holdTimeTicks);
                if (!isCancelled()) cancel();
                return;
            }

            if (currentLineIndex >= pageBars.size()) {
                logger.severe("Text BossBar index out of bounds in PageDisplayTask! LineIndex: " + currentLineIndex + ", Bars: " + pageBars.size());
                forceCancelCleanup(true);
                return;
            }

            String targetLine = pageTranslatedLines.get(currentLineIndex);
            BossBar currentBar = pageBars.get(currentLineIndex);
            int targetLength = targetLine.length();

            if (builtLines[currentLineIndex] == null) {
                builtLines[currentLineIndex] = new StringBuilder();
            }

            if (charIndexInLine >= targetLength) {
                if (!builtLines[currentLineIndex].toString().equals(targetLine)) {
                    builtLines[currentLineIndex] = new StringBuilder(targetLine);
                    currentBar.setTitle(targetLine);
                }
                currentBar.setProgress(1.0);

                currentLineIndex++;
                charIndexInLine = 0;
                if (currentLineIndex >= pageTranslatedLines.size()) {
                    moveToNextPageOrEnd(player, dialogue, pageIndex, holdTimeTicks);
                    if (!isCancelled()) cancel();
                }
                return;
            }

            boolean processedVisibleChar = false;
            while (charIndexInLine < targetLength && !processedVisibleChar && !isCancelled()) {
                char charToAdd = targetLine.charAt(charIndexInLine);
                boolean playSoundForThisChar = true;

                if (charToAdd == '§' && charIndexInLine + 1 < targetLength) {
                    playSoundForThisChar = false;
                    char codeChar = targetLine.charAt(charIndexInLine + 1);
                    int charsToSkip = 0;

                    if ((codeChar == 'x' || codeChar == 'X') && charIndexInLine + 13 < targetLength) {
                        boolean isFullHex = true;
                        for (int k = 0; k < 6; k++) {
                            if (targetLine.charAt(charIndexInLine + 2 + (k * 2)) != '§' || charIndexInLine + 3 + (k * 2) >= targetLength) {
                                isFullHex = false; break;
                            }
                        }
                        if (isFullHex) {
                            charsToSkip = 14;
                        } else {
                            charsToSkip = 2;
                        }
                    }
                    else if ("0123456789abcdefklmnorABCDEFKLMNOR".indexOf(codeChar) != -1) {
                        charsToSkip = 2;
                    }
                    else {
                        charsToSkip = 1;
                    }

                    builtLines[currentLineIndex].append(targetLine, charIndexInLine, charIndexInLine + charsToSkip);
                    charIndexInLine += charsToSkip;
                }
                else {
                    builtLines[currentLineIndex].append(charToAdd);
                    charIndexInLine += 1;
                    processedVisibleChar = true;

                    if (charToAdd == ' ') {
                        playSoundForThisChar = false;
                    }

                    if (playSoundForThisChar && typingSoundKey != null && !typingSoundKey.isBlank()) {
                        try {
                            player.playSound(player.getLocation(), typingSoundKey, soundVolume, soundPitch);
                        } catch (Exception e) { }
                    }
                }
            }

            String currentTitle = builtLines[currentLineIndex].toString();
            try {
                currentBar.setTitle(currentTitle);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error setting BossBar title: '" + currentTitle + "'", e);
            }
            currentBar.setProgress(Math.min(1.0, (double) charIndexInLine / Math.max(1, targetLength)));

            if (charIndexInLine >= targetLength) {
                if (!builtLines[currentLineIndex].toString().equals(targetLine)) {
                    builtLines[currentLineIndex] = new StringBuilder(targetLine);
                    currentBar.setTitle(targetLine);
                }
                currentBar.setProgress(1.0);

                currentLineIndex++;
                charIndexInLine = 0;
                if (currentLineIndex >= pageTranslatedLines.size()) {
                    moveToNextPageOrEnd(player, dialogue, pageIndex, holdTimeTicks);
                    if (!isCancelled()) cancel();
                }
            }
        }

        private void forceCancelCleanup(boolean callStopDialogueIfOnline) {
            if (!isCancelled()) {
                try { cancel(); } catch (IllegalStateException ignored) {}
            }
            CurrentDialogueState state = activeDialogues.get(playerUUID);
            if (!callStopDialogueIfOnline) {
                MovementListener.removeRestriction(playerUUID);
                cleanupBars(playerUUID, this.pageBars);
                if (state != null) {
                    if(state.currentBukkitTask != null && state.currentBukkitTask.getTaskId() == getTaskId()){
                        if (state.backgroundBar != null) {
                            state.backgroundBar.setVisible(false);
                            state.backgroundBar.removeAll();
                        }
                        activeDialogues.remove(playerUUID);
                    }
                }
            } else {
                MovementListener.removeRestriction(playerUUID);
            }
        }

        @Override public synchronized void cancel() throws IllegalStateException {
            if(isCancelled()) return;
            super.cancel();
        }
    }
}