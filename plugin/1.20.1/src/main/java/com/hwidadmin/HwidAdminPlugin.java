package com.hwidadmin;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * HWIDAdmin — Bukkit/Spigot/Paper plugin.
 *
 * <p>Listens on a plugin-messaging channel for HWID payloads sent by the
 * companion Forge client mod and kicks any player whose HWID is not in the
 * configured whitelist.</p>
 */
public final class HwidAdminPlugin extends JavaPlugin {

    private HwidChecker checker;
    private HwidMessageListener messageListener;
    private String registeredChannel;

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
        getLogger().info("HWIDAdmin disabled.");
    }

    public HwidChecker getChecker() {
        return checker;
    }
}
