package com.hwidadmin;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * HWIDAdmin — client-side auth helper.
 *
 * <p>On join this mod sends the player's machine HWID to the HWIDAdmin server
 * plugin. The plugin compares it against its own whitelist and kicks the player
 * when the HWID does not match. The correct HWID is NEVER present in this mod.</p>
 */
@Mod(HwidadminMod.MODID)
public class HwidadminMod {
    public static final String MODID = "hwidadmin";
    private static final Logger LOGGER = LogUtils.getLogger();

    public HwidadminMod() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();

        modBus.addListener(this::commonSetup);

        // Client-only wiring: HWID sender and the in-game /hwid command.
        modBus.addListener(com.hwidadmin.client.ClientSetup::init);

        MinecraftForge.EVENT_BUS.register(this);

        // Client config: print HWID to chat on join, and ping interval.
        modBus.registerConfig(ModConfig.Type.CLIENT, CommonConfig.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Shared channel registration.
        Net.init();
    }

    public static Logger log() {
        return LOGGER;
    }
}
