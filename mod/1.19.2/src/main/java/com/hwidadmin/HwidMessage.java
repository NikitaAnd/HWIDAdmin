package com.hwidadmin;

import net.minecraft.network.FriendlyByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import net.minecraftforge.network.NetworkEvent;

/**
 * Payload sent by the client to the server:
 * byte[0]    = magic (a)
 * byte[1..4] = ASCII tag "mod" — identifies the packet as coming from this mod
 * byte[5]    = version (currently 1)
 * byte[6..9] = 4-byte big-endian payload length L
 * byte[10,10+L) = UTF-8 encoded HWID
 *
 * <p>The Bukkit plugin reads the same layout from the raw payload.</p>
 */
public final class HwidMessage {
    public static final byte MAGIC = (byte) 0xA1;
    public static final byte[] TAG = new byte[]{'m', 'o', 'd'};
    public static final byte VERSION = 1;

    private final String hwid;

    public HwidMessage(String hwid) {
        this.hwid = hwid;
    }

    public String getHwid() {
        return hwid;
    }

    public void encode(FriendlyByteBuf buf) {
        byte[] bytes = hwid.getBytes(StandardCharsets.UTF_8);

        buf.writeByte(MAGIC);
        buf.writeBytes(TAG);
        buf.writeByte(VERSION);
        buf.writeInt(bytes.length);
        buf.writeBytes(bytes);
    }

    public static HwidMessage decode(FriendlyByteBuf buf) {
        byte magic = buf.readByte();
        if (magic != MAGIC) {
            throw new IllegalArgumentException("HWIDAdmin: bad magic 0x" + Integer.toHexString(magic & 0xFF));
        }
        buf.skipBytes(TAG.length);
        buf.readByte(); // version
        int len = buf.readInt();
        if (len < 0 || len > 512) {
            throw new IllegalArgumentException("HWIDAdmin: bad payload length " + len);
        }
        byte[] bytes = new byte[len];
        buf.readBytes(bytes);
        return new HwidMessage(new String(bytes, StandardCharsets.UTF_8));
    }

    public static void handle(HwidMessage msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        // The client does not expect any reply; this handler is a safe no-op.
    }
}
