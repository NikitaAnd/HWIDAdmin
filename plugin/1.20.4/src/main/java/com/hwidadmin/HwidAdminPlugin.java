package com.hwidadmin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HWIDAdmin — Bukkit/Spigot/Paper plugin.
 *
 * <p>Listens on a plugin-messaging channel for HWID payloads sent by the
 * companion Forge client mod and kicks any player whose HWID is not in the
 * configured whitelist. Protected players (OP / holders of configured
 * permissions) that do not send a valid HWID within the configured timeout
 * are kicked automatically.</p>
 */
public final class HwidAdminPlugin extends JavaPlugin {

    private HwidChecker checker;
    private HwidMessageListener messageListener;
    private String registeredChannel;

    /** Pending kick task IDs keyed by player UUID — cancelled when a valid HWID arrives. */
    private final Map<UUID, Integer> pendingKicks = new ConcurrentHashMap<>();

    /** UUIDs of players that already passed the HWID check this session. */
    private final Set<UUID> verifiedPlayers = ConcurrentHashMap.newKeySet();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.checker = new HwidChecker(this);

        // Register the plugin-messaging channel.
        registerMessaging();

        // Reload / status command.
        HwidAdminCommand command = new HwidAdminCommand(this, checker);
        var cmd = getCommand("hwidadmin");
        if (cmd != null) {
            cmd.setExecutor(command);
            cmd.setTabCompleter(command);
        } else {
            getLogger().warning("Could not register /hwidadmin: command not found in plugin.yml");
        }

        // Join / quit listener — enforces HWID timeout for protected players.
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this, checker), this);

        getLogger().info("HWIDAdmin enabled. Allowed HWIDs: " + checker.getAllowedHwids().size());
    }

    /**
     * (Re)registers the incoming/outgoing plugin-messaging channel. Safe to
     * call again after the channel name changed in config.yml.
     */
    public void registerMessaging() {
        String channel = checker.getChannelName();

        // Unregister the previous registration (using the OLD channel name).
        if (registeredChannel != null) {
            getServer().getMessenger().unregisterIncomingPluginChannel(this, registeredChannel);
            getServer().getMessenger().unregisterOutgoingPluginChannel(this, registeredChannel);
        }

        messageListener = new HwidMessageListener(this, checker);
        getLogger().info("Registering incoming plugin channel: " + channel);
        getServer().getMessenger().registerIncomingPluginChannel(this, channel, messageListener);
        getServer().getMessenger().registerOutgoingPluginChannel(this, channel);
        registeredChannel = channel;
    }

    @Override
    public void onDisable() {
        if (registeredChannel != null) {
            getServer().getMessenger().unregisterIncomingPluginChannel(this, registeredChannel);
            getServer().getMessenger().unregisterOutgoingPluginChannel(this, registeredChannel);
        }
        // Cancel all pending kicks.
        for (int taskId : pendingKicks.values()) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        pendingKicks.clear();
        verifiedPlayers.clear();
        getLogger().info("HWIDAdmin disabled.");
    }

    // ---- Pending-kick management (called from PlayerJoinListener / HwidMessageListener) ----

    /**
     * Schedules a kick for the given player. If a valid HWID is not received
     * before the timeout expires, the player is kicked.
     */
    public void schedulePendingKick(Player player) {
        long delay = checker.getHwidTimeoutTicks();
        int taskId = Bukkit.getScheduler().runTaskLater(this, () -> {
            pendingKicks.remove(player.getUniqueId());
            if (player.isOnline() && !verifiedPlayers.contains(player.getUniqueId())) {
                String reason = org.bukkit.ChatColor.translateAlternateColorCodes('&',
                        checker.getNoHwidKickMessage());
                player.kickPlayer(reason);
                getLogger().warning("Kicked " + player.getName()
                        + " — no valid HWID received within " + delay + " ticks ("
                        + (delay / 20) + "s).");
            }
        }, delay).getTaskId();

        // Cancel any previous pending kick for this player (e.g. rapid rejoin).
        Integer prev = pendingKicks.put(player.getUniqueId(), taskId);
        if (prev != null) {
            Bukkit.getScheduler().cancelTask(prev);
        }
    }

    /**
     * Cancels a pending kick and marks the player as verified. Called when a
     * valid HWID is received from the client mod.
     */
    public void verifyPlayer(UUID uuid) {
        verifiedPlayers.add(uuid);
        Integer taskId = pendingKicks.remove(uuid);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    /** Removes the player from tracking on disconnect. */
    public void removePlayer(UUID uuid) {
        verifiedPlayers.remove(uuid);
        Integer taskId = pendingKicks.remove(uuid);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    /** Whether this player has already passed the HWID check this session. */
    public boolean isVerified(UUID uuid) {
        return verifiedPlayers.contains(uuid);
    }

    public HwidChecker getChecker() {
        return checker;
    }
}
