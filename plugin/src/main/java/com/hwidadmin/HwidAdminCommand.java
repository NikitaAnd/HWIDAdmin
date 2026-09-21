package com.hwidadmin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;
import java.util.Locale;

/**
 * Admin command:
 *   /hwidadmin reload  — re-reads config.yml
 *   /hwidadmin status  — shows channel, whitelist size, kick-on-unlisted
 */
public final class HwidAdminCommand implements CommandExecutor, TabCompleter {

    private final HwidAdminPlugin plugin;
    private final HwidChecker checker;

    public HwidAdminCommand(HwidAdminPlugin plugin, HwidChecker checker) {
        this.plugin = plugin;
        this.checker = checker;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                if (!sender.hasPermission("hwidadmin.reload")) {
                    sender.sendMessage(ChatColor.RED + "Нет прав (hwidadmin.reload).");
                    return true;
                }
                checker.reload();
                // Re-register in case the channel changed in config.
                plugin.registerMessaging();
                sender.sendMessage(ChatColor.GREEN + "[HWIDAdmin] Конфиг перечитан. HWID в списке: " + checker.getAllowedHwids().size());
            }
            case "status" -> {
                if (!sender.hasPermission("hwidadmin.status")) {
                    sender.sendMessage(ChatColor.RED + "Нет прав (hwidadmin.status).");
                    return true;
                }
                sender.sendMessage(ChatColor.GOLD + "— HWIDAdmin статус —");
                sender.sendMessage(ChatColor.GRAY + "Канал: " + ChatColor.WHITE + checker.getChannelName());
                sender.sendMessage(ChatColor.GRAY + "Whitelist HWID: " + ChatColor.WHITE + checker.getAllowedHwids().size());
                sender.sendMessage(ChatColor.GRAY + "Кик вне списка: " + (checker.isKickUnlisted() ? ChatColor.RED + "да" : ChatColor.GREEN + "нет"));
            }
            default -> sendUsage(sender);
        }
        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "— HWIDAdmin —");
        sender.sendMessage(ChatColor.GRAY + "/hwidadmin reload" + ChatColor.WHITE + " — перечитать config.yml");
        sender.sendMessage(ChatColor.GRAY + "/hwidadmin status" + ChatColor.WHITE + " — показать настройки");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("reload", "status").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        return List.of();
    }
}
