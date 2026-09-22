package com.hwidadmin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Receives HWID payloads from the client mod.
 *
 * <p>Expected layout (matches the mod's {@code HwidMessage}):
 * <ul>
 *   <li>byte 0    : magic {@code 0xA1}</li>
 *   <li>bytes 1-3 : ASCII tag {@code mod}</li>
 *   <li>byte 4    : protocol version</li>
 *   <li>bytes 5-8 : 4-byte big-endian payload length</li>
 *   <li>rest      : UTF-8 HWID</li>
 * </ul>
 * </p>
 */
public final class HwidMessageListener implements PluginMessageListener {

    private static final byte MAGIC = (byte) 0xA1;
    private static final byte[] TAG = new byte[]{'m', 'o', 'd'};

    private final HwidAdminPlugin plugin;
    private final HwidChecker checker;

    public HwidMessageListener(HwidAdminPlugin plugin, HwidChecker checker) {
        this.plugin = plugin;
        this.checker = checker;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!checker.getChannelName().equals(channel)) {
            return;
        }

        // Only enforce the HWID check for protected players (OP or holders of
        // the configured permission nodes). Regular players are ignored.
        if (!checker.isProtected(player)) {
            return;
        }

        String hwid;
        try {
            hwid = parseHwid(message);
        } catch (Exception e) {
            plugin.getLogger().warning("Ignored malformed HWID packet from " + player.getName() + ": " + e.getMessage());
            return;
        }

        if (checker.isAllowed(hwid)) {
            plugin.getLogger().info("HWID OK " + player.getName() + " (hwid=" + hwid + ")");
            return;
        }

        plugin.getLogger().warning("Unwhitelisted HWID from " + player.getName() + " — kicked. HWID: " + hwid);

        if (checker.isKickUnlisted()) {
            final String reason = org.bukkit.ChatColor.translateAlternateColorCodes('&', checker.getKickMessage());
            Bukkit.getScheduler().runTask(plugin, () -> player.kickPlayer(reason));
        }
    }

    /**
     * Parses the payload strictly. Returns the normalised HWID or throws an
     * {@link IllegalArgumentException} on any inconsistency.
     */
    static String parseHwid(byte[] data) {
        if (data == null || data.length < 10) {
            throw new IllegalArgumentException("packet too short");
        }
        ByteBuffer buf = ByteBuffer.wrap(data);

        byte magic = buf.get();
        if (magic != MAGIC) {
            throw new IllegalArgumentException("bad magic 0x" + Integer.toHexString(magic & 0xFF));
        }

        byte[] tag = new byte[3];
        buf.get(tag);
        for (int i = 0; i < TAG.length; i++) {
            if (tag[i] != TAG[i]) {
                throw new IllegalArgumentException("bad tag");
            }
        }

        buf.get(); // version (unused for now)

        int len = buf.getInt();
        if (len < 1 || len > 512 || len > buf.remaining()) {
            throw new IllegalArgumentException("bad payload length " + len);
        }

        byte[] hwidBytes = new byte[len];
        buf.get(hwidBytes);
        String hwid = new String(hwidBytes, StandardCharsets.UTF_8);

        String normalized = HwidChecker.normalizeHwid(hwid);
        if (normalized.length() < 16) {
            throw new IllegalArgumentException("hwid too short");
        }
        return normalized;
    }
}
