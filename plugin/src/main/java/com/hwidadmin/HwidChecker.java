package com.hwidadmin;

import org.bukkit.configuration.file.FileConfiguration;
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
