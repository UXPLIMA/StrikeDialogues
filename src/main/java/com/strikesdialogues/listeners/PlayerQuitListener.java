package com.strikesdialogues.listeners;

import com.strikesdialogues.dialogue.DialogueManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final DialogueManager dialogueManager;
    public PlayerQuitListener(DialogueManager dialogueManager) {
        this.dialogueManager = dialogueManager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        dialogueManager.stopDialogue(player);
    }
}