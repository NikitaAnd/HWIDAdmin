package com.hwidadmin;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Reads and validates the plugin configuration and holds the HWID whitelist.
 *
 * <p>The plugin never overrides an existing config on disk: settings are read
 * straight from {@code plugins/HWIDAdmin/config.yml} on every reload.</p>
 */
public final class HwidChecker {

    public static final String DEFAULT_CHANNEL = "hwidadmin:main";

    private final JavaPlugin plugin;
    private volatile String channelName = DEFAULT_CHANNEL;
    private volatile Set<String> allowedHwids = Collections.emptySet();
    private volatile boolean kickUnlisted = true;
    private volatile String kickMessage = "&cДоступ запрещён: твой HWID не в белом списке.";
    private volatile boolean checkOp = true;
    private volatile Set<String> requiredPermissions = Collections.emptySet();

    public HwidChecker(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public synchronized void reload() {
        // Ensure a default file exists, but never overwrite user edits.
        if (!new java.io.File(plugin.getDataFolder(), "config.yml").exists()) {
            plugin.saveDefaultConfig();
        }
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();

        channelName = cfg.getString("channel", DEFAULT_CHANNEL);
        if (channelName == null || channelName.isBlank()) {
            channelName = DEFAULT_CHANNEL;
        }

        Set<String> set = new HashSet<>();
        for (String raw : cfg.getStringList("allowed-hwids")) {
            String h = normalizeHwid(raw);
            if (!h.isEmpty()) {
                set.add(h);
            }
        }
        // Legacy: some people kept a single "allowed-hwid" string.
        String single = cfg.getString("allowed-hwid", null);
        if (single != null && !single.isBlank()) {
            String h = normalizeHwid(single);
            if (!h.isEmpty()) {
                set.add(h);
            }
        }
        allowedHwids = Collections.unmodifiableSet(set);

        kickUnlisted = cfg.getBoolean("kick-unlisted", true);
        kickMessage = cfg.getString("kick-message", "&cДоступ запрещён: твой HWID не в белом списке.");

        checkOp = cfg.getBoolean("check-op", true);

        Set<String> perms = new HashSet<>();
        for (String raw : cfg.getStringList("required-permissions")) {
            if (raw != null && !raw.trim().isEmpty()) {
                perms.add(raw.trim());
            }
        }
        requiredPermissions = Collections.unmodifiableSet(perms);
    }

    public String getChannelName() {
        return channelName;
    }

    public Set<String> getAllowedHwids() {
        return allowedHwids;
    }

    public boolean isKickUnlisted() {
        return kickUnlisted;
    }

    public String getKickMessage() {
        return kickMessage;
    }

    public boolean isCheckOp() {
        return checkOp;
    }

    public Set<String> getRequiredPermissions() {
        return requiredPermissions;
    }

    /**
     * True when the player must pass the HWID check: either a server OP, or
     * holding at least one of the permission nodes from {@code required-permissions}.
     * Everyone else is ignored by the HWID check.
     */
    public boolean isProtected(Player player) {
        if (checkOp && player.isOp()) {
            return true;
        }
        for (String perm : requiredPermissions) {
            if (player.hasPermission(perm)) {
                return true;
            }
        }
        return false;
    }

    /** True when the given (already normalised) HWID is in the whitelist. */
    public boolean isAllowed(String normalizedHwid) {
        return allowedHwids.contains(normalizedHwid);
    }

    /** Trims/lowercases a HWID and validates the hex format softly. */
    public static String normalizeHwid(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim().toLowerCase(Locale.ROOT);
        if (s.matches("^\\{[0-9a-f-]+}$")) {
            s = s.replace("{", "").replace("}", "").replace("-", "");
        }
        return s;
    }
}
