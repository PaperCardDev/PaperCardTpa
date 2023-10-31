package cn.paper_card.paper_card_tpa;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

class TpaDenyCommand implements CommandExecutor, TabCompleter {

    private final @NotNull PaperCardTpa plugin;

    TpaDenyCommand(@NotNull PaperCardTpa plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (!(commandSender instanceof final Player destPlayer)) {
            plugin.sendError(commandSender, "该命令只能由玩家来执行！");
            return true;
        }

        final String argSrcPlayer = strings.length > 0 ? strings[0] : null;

        if (argSrcPlayer == null) {
            plugin.sendWaring(commandSender, "你必须要指定拒绝谁的传送请求噢");
            return true;
        }

        final Player srcPlayer = plugin.getOnlinePlayerByName(argSrcPlayer);

        if (srcPlayer == null) {
            plugin.sendInfo(commandSender, Component.text()
                    .append(Component.text("找不到该在线玩家: ").color(NamedTextColor.YELLOW))
                    .append(Component.text(argSrcPlayer).color(NamedTextColor.RED))
                    .build());
            return true;
        }

        final TpRequest request = plugin.getRequestContainer().remove(srcPlayer, destPlayer);

        if (request == null) {
            plugin.sendInfo(commandSender, Component.text()
                    .append(srcPlayer.displayName())
                    .append(Component.text(" 没有向你发起传送请求噢").color(NamedTextColor.YELLOW))
                    .build());

            return true;
        }

        plugin.sendInfo(commandSender, Component.text()
                .append(Component.text("你已拒绝 ").color(NamedTextColor.GREEN))
                .append(srcPlayer.displayName())
                .append(Component.text(" 的传送请求").color(NamedTextColor.GREEN))
                .build());

        // 通知对方
        plugin.sendInfo(srcPlayer, Component.text()
                .append(destPlayer.displayName())
                .append(Component.text(" 已拒绝你的传送请求 :(").color(NamedTextColor.RED))
                .append(Component.text())
                .build());

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        return null;
    }
}
