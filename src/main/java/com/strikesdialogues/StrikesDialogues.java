package com.strikesdialogues;

import com.strikesdialogues.commands.DialogueCommand;
import com.strikesdialogues.commands.ReloadCommand;
import com.strikesdialogues.config.ConfigManager;
import com.strikesdialogues.dialogue.DialogueManager;
import com.strikesdialogues.listeners.MovementListener;
import com.strikesdialogues.listeners.PlayerQuitListener;
import com.strikesdialogues.util.ActionExecutor;
import com.strikesdialogues.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;


public final class StrikesDialogues extends JavaPlugin {

    private static StrikesDialogues instance;
    private ConfigManager configManager;
    private DialogueManager dialogueManager;


    @Override
    public void onEnable() {
        instance = this;
        this.configManager = new ConfigManager(this);
        this.dialogueManager = new DialogueManager(this);

        TextUtil.initialize(this);
        ActionExecutor.initialize(this);

        if (getServer().getPluginManager().isPluginEnabled("ProtocolLib")) {
            MovementListener.initializeProtocolLib(this);
        } else {
            if (configManager.isLockPerspectiveEnabled()) {
                getLogger().warning("ProtocolLib not found! Perspective locking is DISABLED.");
            }
        }

        DialogueCommand dialogueExecutor = new DialogueCommand(this, configManager, dialogueManager);
        Objects.requireNonNull(getCommand("startdialogue"), "Command 'startdialogue' not found in plugin.yml")
                .setExecutor(dialogueExecutor);
        Objects.requireNonNull(getCommand("startdialogue"), "Command 'startdialogue' not found in plugin.yml")
                .setTabCompleter(dialogueExecutor);

        ReloadCommand reloadExecutor = new ReloadCommand(this, configManager);
        Objects.requireNonNull(getCommand("dialoguereload"), "Command 'dialoguereload' not found in plugin.yml")
                .setExecutor(reloadExecutor);

        getServer().getPluginManager().registerEvents(new PlayerQuitListener(dialogueManager), this);
        getServer().getPluginManager().registerEvents(new MovementListener(), this);

        getLogger().info("Strikes-Dialogues has been enabled!");

        if (!getServer().getPluginManager().isPluginEnabled("PlaceholderAPI") && configManager.isPapiEnabled()) {
            getLogger().warning("PlaceholderAPI is enabled in config but the plugin was not found! Placeholders will not work.");
        }
    }

    @Override
    public void onDisable() {
        MovementListener.cleanupProtocolLib();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (MovementListener.isRestricted(player) || MovementListener.isPerspectiveLocked(player)) {
                if (dialogueManager != null) {
                    dialogueManager.stopDialogue(player, player.getUniqueId(), 0L);
                } else {
                    MovementListener.unrestrictPlayer(player);
                }
            }
        }
        MovementListener.clearAllRestrictions();

        getLogger().info("Strikes-Dialogues disabled.");
        instance = null;
        dialogueManager = null;
        configManager = null;
    }

    public static StrikesDialogues getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        if (this.configManager == null) {
            getLogger().severe("ConfigManager accessed after onDisable or before full onEnable!");
            if(instance != null) {
                this.configManager = new ConfigManager(instance);
                return this.configManager;
            }
            throw new IllegalStateException("ConfigManager is null and cannot be recovered.");
        }
        return configManager;
    }

    public DialogueManager getDialogueManager() {
        if (this.dialogueManager == null) {
            getLogger().severe("DialogueManager accessed after onDisable or before full onEnable!");
            if(instance != null && instance.configManager != null) {
                this.dialogueManager = new DialogueManager(instance);
                return this.dialogueManager;
            }
            throw new IllegalStateException("DialogueManager is null and cannot be recovered.");
        }
        return dialogueManager;
    }
}