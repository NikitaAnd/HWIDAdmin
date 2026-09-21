package com.hwidadmin.client;

import com.hwidadmin.CommonConfig;
import com.hwidadmin.HwidadminMod;
import com.hwidadmin.HwidMessage;
import com.hwidadmin.Net;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client event handlers: sends the HWID on join and periodically while in
 * game, and registers the in-game {@code /hwid} command.
 */
@Mod.EventBusSubscriber(modid = HwidadminMod.MODID, value = Dist.CLIENT)
public final class HwidSender {
    private static final int SHOW_TICKS = 3 * 20;   // announce HWID ~3s after join
    private static final int PING_TICKS = 20;       // try every second until success
    private static boolean announcedThisJoin = false;
    private static boolean sentThisJoin = false;
    private static int tickCounter = 0;
    public static boolean currentKicked = false;

    private HwidSender() {
    }

    @SubscribeEvent
    public static void onPlayerLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        announcedThisJoin = false;
        sentThisJoin = false;
        currentKicked = false;
        tickCounter = 0;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) {
            // mc.getConnection() is null in singleplayer: only multiplayer
            // servers produce a ClientPacketListener, so we never ping locally.
            return;
        }

        tickCounter++;

        if (CommonConfig.SHOW_HWID_ON_JOIN.get() && !announcedThisJoin && tickCounter >= SHOW_TICKS) {
            mc.player.displayClientMessage(
                    Component.literal("[HWIDAdmin] Твой HWID: " + HwidCollector.hash()).withStyle(ChatFormatting.AQUA),
                    false);
            announcedThisJoin = true;
        }

        int interval = CommonConfig.PING_INTERVAL.get() * 20;
        if (Net.channel() != null && (!sentThisJoin || tickCounter % interval == 0)) {
            sendHwid();
        }
    }

    private static void sendHwid() {
        try {
            Net.channel().sendToServer(new HwidMessage(HwidCollector.hash()));
            sentThisJoin = true;
        } catch (Exception e) {
            HwidadminMod.log().warn("HWIDAdmin: failed to send HWID packet", e);
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
                com.mojang.brigadier.builder.LiteralArgumentBuilder
                        .<net.minecraft.commands.CommandSourceStack>literal("hwid")
                        .executes(ctx -> {
                            com.hwidadmin.client.HwidSender.reportHwid(ctx.getSource());
                            return 1;
                        })
        );
    }

    private static void reportHwid(net.minecraft.commands.CommandSourceStack source) {
        String raw = HwidCollector.raw();
        String hash = HwidCollector.hash();
        source.sendSuccess(() -> Component.literal("— HWIDAdmin —").withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(() -> Component.literal("Провайдер: " + HwidCollector.providerName()).withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("HWID hash: " + hash).withStyle(ChatFormatting.AQUA), false);
        source.sendSuccess(() -> Component.literal("(raw: " + raw + ")").withStyle(ChatFormatting.DARK_GRAY), false);
        HwidadminMod.log().info("HWIDAdmin: provider={}, raw={}, hash={}", HwidCollector.providerName(), raw, hash);
    }
}
