package com.hwidadmin;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Client-only mod configuration, generated into
 * {@code config/hwidadmin-client.toml}.
 */
public final class CommonConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue PING_INTERVAL;
    public static final ForgeConfigSpec.BooleanValue SHOW_HWID_ON_JOIN;

    private CommonConfig() {
    }

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("hwidadmin");

        SHOW_HWID_ON_JOIN = builder
                .comment("If true, the player's own HWID is printed to the in-game chat once upon joining a server. Use this to read the HWID that you must add to the plugin's config.")
                .define("showHwidOnJoin", true);

        PING_INTERVAL = builder
                .comment("How often (in seconds) this mod resends the HWID to the server while connected. Resends keep the whitelist check active even if a login packet was lost.")
                .defineInRange("pingInterval", 10, 5, 60);

        builder.pop();
        SPEC = builder.build();
    }
}
