package com.hwidadmin;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Enforces the HWID check at join time.
 *
 * <p>When a <em>protected</em> player (OP or holder of a configured permission)
 * joins, a countdown starts. If the client mod does not deliver a valid HWID
 * before the countdown expires, the player is kicked. Players that are not
 * protected are completely ignored.</p>
 */
public final class PlayerJoinListener implements Listener {

    private final HwidAdminPlugin plugin;
    private final HwidChecker checker;

    public PlayerJoinListener(HwidAdminPlugin plugin, HwidChecker checker) {
        this.plugin = plugin;
        this.checker = checker;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Only enforce for protected players (OP / required-permissions).
        if (!checker.isProtected(player)) {
            return;
        }

        // If the player is already verified (shouldn't happen on fresh join,
        // but guards against rapid rejoin edge cases), skip.
        if (plugin.isVerified(player.getUniqueId())) {
            return;
        }

        // Schedule a kick if no valid HWID arrives within the timeout.
        plugin.schedulePendingKick(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.removePlayer(event.getPlayer().getUniqueId());
    }
}
