package com.hwidadmin;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

/**
 * Simple network channel shared by the mod (client -> server) and the Bukkit
 * plugin (which reads the raw plugin message). The channel id must stay
 * in-sync with the plugin's config.
 *
 * <p>On Minecraft 1.20.4+ Forge replaced the old {@code NetworkRegistry}
 * factory with the {@code ChannelBuilder} API, which is used here.</p>
 */
public final class Net {
    public static final String CHANNEL_NAME = "hwidadmin:main";
    public static final int PROTOCOL = 1;

    // optional(): allow this mod to talk to a server that is missing the
    // channel (a vanilla Bukkit/Spigot/Paper server does not participate in
    // the Forge mod-list handshake at all).
    private static final SimpleChannel CHANNEL = ChannelBuilder
            .named(new ResourceLocation(CHANNEL_NAME))
            .networkProtocolVersion(PROTOCOL)
            .optional()
            .simpleChannel();

    private Net() {
    }

    public static void init() {
        CHANNEL.messageBuilder(HwidMessage.class)
                .encoder(HwidMessage::encode)
                .decoder(HwidMessage::decode)
                .consumerMainThread(HwidMessage::handle)
                .add();

        HwidadminMod.log().debug("HWIDAdmin channel `{}` (protocol {}) registered.", CHANNEL_NAME, PROTOCOL);
    }

    public static SimpleChannel channel() {
        return CHANNEL;
    }

    /** Sends the HWID message from the client to the server. */
    public static void sendToServer(HwidMessage msg) {
        CHANNEL.send(msg, PacketDistributor.SERVER.noArg());
    }
}
