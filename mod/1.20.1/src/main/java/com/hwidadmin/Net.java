package com.hwidadmin;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Simple network channel shared by the mod (client -> server) and the Bukkit
 * plugin (which reads the raw plugin message). The channel id must stay
 * in-sync with the plugin's config.
 */
public final class Net {
    public static final String CHANNEL_NAME = "hwidadmin:main";
    public static final String PROTOCOL = "1";
    private static SimpleChannel channel;

    private Net() {
    }

    public static void init() {
        channel = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(CHANNEL_NAME),
                () -> PROTOCOL,
                // ACCEPT the connection even when the remote side does not
                // register this channel: the Bukkit server is vanilla and does
                // not participate in the Forge mod-list handshake at all.
                NetworkRegistry.acceptMissingOr(PROTOCOL),
                NetworkRegistry.acceptMissingOr(PROTOCOL)
        );

        channel.messageBuilder(HwidMessage.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(HwidMessage::encode)
                .decoder(HwidMessage::decode)
                .consumerMainThread(HwidMessage::handle)
                .add();

        HwidadminMod.log().debug("HWIDAdmin channel `{}` (protocol {}) registered.", CHANNEL_NAME, PROTOCOL);
    }

    public static SimpleChannel channel() {
        return channel;
    }

    /** Sends the HWID message from the client to the server. */
    public static void sendToServer(HwidMessage msg) {
        channel.sendToServer(msg);
    }
}
