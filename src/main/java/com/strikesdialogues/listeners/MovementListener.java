package com.strikesdialogues.listeners;

import com.strikesdialogues.StrikesDialogues;
import com.strikesdialogues.config.ConfigManager;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class MovementListener implements Listener {

    private static final Set<UUID> restrictedPlayers = new HashSet<>();
    private static final Map<UUID, Float> lockedYaw = new HashMap<>();
    private static final Map<UUID, Float> lockedPitch = new HashMap<>();
    private static final float DEFAULT_WALK_SPEED = 0.2f;
    private static final float RESTRICTED_FLY_SPEED = 0f;
    private static final float DEFAULT_FLY_SPEED = 0.1f;

    private static ProtocolManager protocolManager = null;
    private static PacketAdapter cameraPacketAdapter = null;

    public static void initializeProtocolLib(StrikesDialogues plugin) {
        try {
            protocolManager = ProtocolLibrary.getProtocolManager();
            createCameraPacketListener(plugin);
            protocolManager.addPacketListener(cameraPacketAdapter);
            plugin.getLogger().info("ProtocolLib hook enabled for perspective locking.");
        } catch (Exception e) {
            plugin.getLogger().warning("ProtocolLib not found or failed to initialize. Perspective locking disabled.");
            protocolManager = null;
        }
    }

    public static void cleanupProtocolLib() {
        if (protocolManager != null && cameraPacketAdapter != null) {
            protocolManager.removePacketListener(cameraPacketAdapter);
            protocolManager = null;
            cameraPacketAdapter = null;
        }
    }

    private static ConfigManager getConfigManager() {
        StrikesDialogues plugin = StrikesDialogues.getInstance();
        return (plugin != null) ? plugin.getConfigManager() : null;
    }

    public static void restrictPlayer(Player player) {
        if (player == null) return;
        ConfigManager cm = getConfigManager();
        if (cm == null || !cm.isMovementRestrictionEnabled()) return;

        UUID uuid = player.getUniqueId();
        restrictedPlayers.add(uuid);

        player.setFlySpeed(RESTRICTED_FLY_SPEED);
        player.setAllowFlight(false);

        if (cm.isLockPerspectiveEnabled()) {
            if (protocolManager != null) {
                Location loc = player.getLocation();
                lockedYaw.put(uuid, loc.getYaw());
                lockedPitch.put(uuid, loc.getPitch());
                if (cm.isDebugLogging()) {
                    StrikesDialogues.getInstance().getLogger().info("[DEBUG MovementListener] Perspective lock activated for " + player.getName() + " at Yaw: " + loc.getYaw() + " Pitch: " + loc.getPitch());
                }
            } else if (cm.isDebugLogging()) {
                StrikesDialogues.getInstance().getLogger().warning("[DEBUG MovementListener] Perspective lock requested for " + player.getName() + " but ProtocolLib is not available.");
            }
        }

        if (cm.isDebugLogging()) {
            StrikesDialogues.getInstance().getLogger().info("[DEBUG MovementListener] Restricted " + player.getName() + " (No speed change)");
        }
    }

    public static void unrestrictPlayer(Player player) {
        if (player == null) return;
        ConfigManager cm = getConfigManager();

        UUID uuid = player.getUniqueId();
        boolean wasRestricted = restrictedPlayers.remove(uuid);

        boolean wasLocked = lockedYaw.containsKey(uuid);
        lockedYaw.remove(uuid);
        lockedPitch.remove(uuid);
        if (wasLocked && cm != null && cm.isDebugLogging()) {
            StrikesDialogues.getInstance().getLogger().info("[DEBUG MovementListener] Perspective lock removed for " + player.getName());
        }

        if (cm != null && cm.isDebugLogging()) {
            StrikesDialogues.getInstance().getLogger().info("[DEBUG MovementListener] unrestrictPlayer called for " + player.getName() + ". Was restricted? " + wasRestricted + ". Was locked? " + wasLocked);
        }

        if (wasRestricted && player.isOnline()) {
            try {
                player.setWalkSpeed(DEFAULT_WALK_SPEED);
                player.setFlySpeed(DEFAULT_FLY_SPEED);

                boolean allowFlight = player.getGameMode() == GameMode.CREATIVE ||
                        player.getGameMode() == GameMode.SPECTATOR ||
                        player.hasPermission("essentials.fly");
                player.setAllowFlight(allowFlight);

                if (cm != null && cm.isDebugLogging()) {
                    StrikesDialogues.getInstance().getLogger().info("[DEBUG MovementListener] Restored speed/flight for " + player.getName());
                }
            } catch (Exception e) {
                if (cm != null) cm.getPlugin().getLogger().log(Level.SEVERE, "Failed to reset speed/flight for " + player.getName(), e);
                player.setWalkSpeed(0.2f);
                player.setFlySpeed(0.1f);
            }
        } else if (!wasRestricted && cm != null && cm.isDebugLogging()) {
            StrikesDialogues.getInstance().getLogger().info("[DEBUG MovementListener] unrestrictPlayer called for " + player.getName() + ", but they weren't in the restricted set.");
        }
    }

    public static boolean isRestricted(Player player) {
        ConfigManager cm = getConfigManager();
        return player != null && (cm != null && cm.isMovementRestrictionEnabled()) && restrictedPlayers.contains(player.getUniqueId());
    }

    public static boolean isPerspectiveLocked(Player player) {
        ConfigManager cm = getConfigManager();
        return player != null && cm != null && cm.isLockPerspectiveEnabled() && lockedYaw.containsKey(player.getUniqueId());
    }

    public static void removeRestriction(UUID playerUUID) {
        boolean removedMove = restrictedPlayers.remove(playerUUID);
        boolean removedLock = lockedYaw.remove(playerUUID) != null;
        lockedPitch.remove(playerUUID);

        ConfigManager cm = getConfigManager();
        if (cm != null && cm.isDebugLogging() && (removedMove || removedLock)) {
            StrikesDialogues.getInstance().getLogger().info("[DEBUG MovementListener] Removed restriction data for UUID " + playerUUID + " (Move removed: "+removedMove+", Lock removed: "+removedLock+")");
        }
    }

    public static void clearAllRestrictions() {
        restrictedPlayers.clear();
        lockedYaw.clear();
        lockedPitch.clear();
        ConfigManager cm = getConfigManager();
        if (cm != null && cm.isDebugLogging()) {
            StrikesDialogues.getInstance().getLogger().info("[DEBUG MovementListener] Cleared all restriction data (onDisable).");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (isRestricted(player)) {
            Location from = event.getFrom();
            Location to = event.getTo();

            boolean rotationOnlyChange = from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ() &&
                    (from.getYaw() != to.getYaw() || from.getPitch() != to.getPitch());

            boolean blockPosChanged = from.getBlockX() != to.getBlockX() ||
                    from.getBlockY() != to.getBlockY() ||
                    from.getBlockZ() != to.getBlockZ();

            if (blockPosChanged && !rotationOnlyChange) {
                event.setCancelled(true);
            }
        }
    }

    private static void createCameraPacketListener(StrikesDialogues plugin) {
        if (cameraPacketAdapter != null) return;

        cameraPacketAdapter = new PacketAdapter(plugin, ListenerPriority.HIGHEST,
                PacketType.Play.Client.LOOK,
                PacketType.Play.Client.POSITION_LOOK
        ) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                if (player == null || !player.isOnline()) return;

                UUID uuid = player.getUniqueId();
                if (lockedYaw.containsKey(uuid)) {
                    PacketType type = event.getPacketType();

                    if (type == PacketType.Play.Client.POSITION_LOOK) {
                        event.setCancelled(true);
                        ConfigManager cm = getConfigManager();
                        if (cm != null && cm.isDebugLogging()) {
                            plugin.getLogger().info("[DEBUG MovementListener] Cancelled POSITION_LOOK packet for " + player.getName());
                        }
                    }
                    else if (type == PacketType.Play.Client.LOOK) {
                        event.setCancelled(true);

                        final float initialYaw = lockedYaw.getOrDefault(uuid, player.getLocation().getYaw());
                        final float initialPitch = lockedPitch.getOrDefault(uuid, player.getLocation().getPitch());

                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (player.isOnline() && lockedYaw.containsKey(uuid)) {
                                Location currentLoc = player.getLocation();
                                Location forcedLocation = new Location(player.getWorld(), currentLoc.getX(), currentLoc.getY(), currentLoc.getZ(), initialYaw, initialPitch);
                                player.teleport(forcedLocation, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);

                                ConfigManager cm = getConfigManager();
                                if (cm != null && cm.isDebugLogging()) {
                                    plugin.getLogger().info("[DEBUG MovementListener] Synced teleport for LOOK packet " + player.getName() + " to Yaw: " + initialYaw + " Pitch: " + initialPitch);
                                }
                            }
                        });
                    }
                }
            }
        };
    }
}