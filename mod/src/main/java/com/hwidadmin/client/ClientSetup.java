package com.hwidadmin.client;

import com.hwidadmin.HwidadminMod;

/**
 * Client-only initialization, invoked from the mod constructor via the
 * {@code FMLClientSetupEvent} listener registration.
 *
 * <p>The actual client event handlers ({@link HwidSender}) are registered
 * statically via {@code @Mod.EventBusSubscriber}.</p>
 */
public final class ClientSetup {
    private ClientSetup() {
    }

    public static void init(final net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
        // Triggers class loading of the client-only HWID provider.
        HwidadminMod.log().debug("HWIDAdmin client setup: HWID provider = {}", HwidCollector.providerName());
    }
}
