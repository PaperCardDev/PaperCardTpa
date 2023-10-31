package cn.paper_card.paper_card_tpa;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.TitlePart;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

class TpaAcceptCommand implements CommandExecutor, TabCompleter {

    private final @NotNull PaperCardTpa plugin;

    TpaAcceptCommand(@NotNull PaperCardTpa plugin) {
        this.plugin = plugin;
    }


    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (!(commandSender instanceof final Player destPlayer)) {
            plugin.sendError(commandSender, "该命令只能由玩家来执行！");
            return true;
        }

        final String argSrcPlayer = strings.length > 0 ? strings[0] : null;

        final Player srcPlayer;

        if (argSrcPlayer == null) {
            plugin.sendError(commandSender, "你必须指定要同意哪个玩家的传送请求噢~");
            return true;
        }

        srcPlayer = plugin.getOnlinePlayerByName(argSrcPlayer);

        if (srcPlayer == null) {
            plugin.sendInfo(commandSender, Component.text()
                    .append(Component.text("该玩家 ").color(NamedTextColor.YELLOW))
                    .append(Component.text(argSrcPlayer).color(NamedTextColor.RED))
                    .append(Component.text(" 不在线").color(NamedTextColor.YELLOW))
                    .build());
            return true;
        }

        // 查询传送请求
        final TpRequest request = plugin.getRequestContainer().remove(srcPlayer, destPlayer);

        if (request == null) {
            plugin.sendInfo(commandSender, Component.text()
                    .append(srcPlayer.displayName())
                    .append(Component.text(" 没有给你发送传送请求").color(NamedTextColor.YELLOW))
                    .build());
            return true;
        }

        // 检查传送冷却
        final long playerTpCd = plugin.getPlayerTpCd(srcPlayer);
        if (playerTpCd > 0) {
            final String s1 = PaperCardTpa.formatTime(playerTpCd);
            plugin.sendInfo(srcPlayer, Component.text()
                    .append(destPlayer.displayName())
                    .append(Component.text(" 已同意你的传送请求，但你传送冷却，").color(NamedTextColor.YELLOW))
                    .append(Component.text(s1).color(NamedTextColor.RED))
                    .append(Component.text("后才能再次传送").color(NamedTextColor.YELLOW))
                    .build());


            plugin.sendInfo(destPlayer, Component.text()
                    .append(srcPlayer.displayName())
                    .append(Component.text(" 处于传送冷却状态，").color(NamedTextColor.YELLOW))
                    .append(Component.text(s1).color(NamedTextColor.RED))
                    .append(Component.text("后才能再次传送").color(NamedTextColor.YELLOW))
                    .build());
            return true;
        }

        // 消耗末影珍珠
        if (!plugin.consumeEnderPearl(srcPlayer)) {
            plugin.sendError(srcPlayer, "末影珍珠不足，一次传送至少需要4颗");
            plugin.sendError(destPlayer, "对方没有足够的末影珍珠进行传送");
            return true;
        }


        final AtomicInteger integer = new AtomicInteger(60);
        final Consumer<ScheduledTask> task = (t) -> {
            final int i = integer.get();
            if (i <= 0) {
                srcPlayer.teleportAsync(destPlayer.getLocation());
                plugin.setPlayerLastTp(srcPlayer, System.currentTimeMillis());
                plugin.sendInfo(srcPlayer, Component.text("已传送").color(NamedTextColor.GREEN));
                plugin.sendInfo(destPlayer, Component.text("已传送").color(NamedTextColor.GREEN));
                t.cancel();
                return;
            }
            integer.set(i - 20);
            plugin.sendInfo(srcPlayer, Component.text()
                    .append(Component.text(i / 20).color(NamedTextColor.GOLD))
                    .append(Component.text("秒后传送...").color(NamedTextColor.GREEN))
                    .build());
        };

        // 真正的传送
        srcPlayer.getScheduler().runAtFixedRate(plugin, task, null, 20, 20);

        srcPlayer.sendActionBar(Component.text()
                .append(destPlayer.displayName())
                .appendSpace()
                .append(Component.text("已同意你的传送请求，即将传送...")).color(NamedTextColor.GREEN)
                .build());

        // 让被玩家不要动
        srcPlayer.sendTitlePart(TitlePart.TITLE, Component.text("准备传送").color(NamedTextColor.GOLD));
        srcPlayer.sendTitlePart(TitlePart.SUBTITLE, Component.text("不要动！").color(NamedTextColor.RED));

        destPlayer.sendActionBar(Component.text()
                .append(Component.text("准备将 ").color(NamedTextColor.GREEN))
                .append(srcPlayer.displayName())
                .append(Component.text(" 传送到你这儿~").color(NamedTextColor.GREEN))
                .build());

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (strings.length == 1) {
            final String arg = strings[0];
            final LinkedList<String> list = new LinkedList<>();

            for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                final String name = onlinePlayer.getName();
                if (name.startsWith(arg)) list.add(name);
            }

            return list;
        }

        return null;
    }
}
