package cn.paper_card.paper_card_tpa;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;

class TpaCommand implements CommandExecutor, TabCompleter {

    private final @NotNull PaperCardTpa plugin;


    TpaCommand(@NotNull PaperCardTpa plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (!(commandSender instanceof final Player sender)) {
            plugin.sendError(commandSender, "该命令只能由玩家来执行！");
            return true;
        }

        final String argTargetPlayer = strings.length > 0 ? strings[0] : null;

        if (argTargetPlayer == null) {
            plugin.sendWaring(commandSender, "你必须指定要传送到谁那里噢，小笨蛋");
            return true;
        }

        if (strings.length != 1) {
            plugin.sendError(commandSender, "参数错误，只需要1个参数，而你提供了%d个参数".formatted(strings.length));
            return true;
        }

        final Player receiver = plugin.getOnlinePlayerByName(argTargetPlayer);

        if (receiver == null) {
            plugin.sendInfo(commandSender, Component.text()
                    .append(Component.text("找不到该在线玩家：").color(NamedTextColor.YELLOW))
                    .append(Component.text(argTargetPlayer).color(NamedTextColor.RED))
                    .build()
            );
            return true;
        }

        // 旁观者
        if (sender.getGameMode() == GameMode.SPECTATOR) {
            sender.teleportAsync(receiver.getLocation());
            return true;
        }

        // 检查是否启用
        if (!plugin.getConfigManager().isEnable()) {
            plugin.sendWaring(commandSender, "服务器暂时禁用了TPA传送功能");
            return true;
        }

        // 检查传送冷却
        final long playerTpCd = plugin.getPlayerTpCd(sender);
        if (playerTpCd > 0) {
            plugin.sendInfo(commandSender, Component.text()
                    .append(Component.text("传送冷却，你在").color(NamedTextColor.YELLOW))
                    .append(Component.text(PaperCardTpa.formatTime(playerTpCd)).color(NamedTextColor.RED))
                    .append(Component.text("后才能再次传送噢").color(NamedTextColor.YELLOW))
                    .build()
            );
            return true;
        }

        plugin.getTaskScheduler().runTaskAsynchronously(() -> {
            // 检查是否重复请求
            final TpRequest reqOld = plugin.getRequestContainer().removeBySender(sender);

            if (reqOld != null) {
                final long currentTimeMillis = System.currentTimeMillis();

                // 请求已经过期
                if (currentTimeMillis > reqOld.createTime() + 2 * 60 * 1000L) {
                    plugin.getLogger().info("已自动取消超时的传送请求");
                } else {
                    Util.sendRep(plugin, sender, reqOld.receiver());
                    plugin.getRequestContainer().add(reqOld); // 放回
                    return;
                }
            }

            // 检查代价
            final TpRequest reqNew = Util.checkCost(plugin, sender, receiver, true);

            if (reqNew == null) return;

            // 添加传送请求
            final boolean added = plugin.getRequestContainer().add(reqNew);

            if (!added) {
                plugin.sendError(commandSender, "当前传送系统繁忙，请稍后重试");
                return;
            }

            // 告知代价
            Util.sendCost(plugin, reqNew);

            // 通知
            Util.sendNotify(plugin, reqNew);
        });

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (strings.length == 1) {
            final String arg = strings[0];
            final LinkedList<String> list = new LinkedList<>();
            for (final Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                final String name = onlinePlayer.getName();
                if (name.startsWith(arg)) list.add(name);
            }
            return list;
        }
        return null;
    }
}
