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

class TpaCancelCommand implements CommandExecutor, TabCompleter {

    private final @NotNull PaperCardTpa plugin;

    TpaCancelCommand(@NotNull PaperCardTpa plugin) {
        this.plugin = plugin;
    }


    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {

        if (!(commandSender instanceof final Player srcPlayer)) {
            plugin.sendError(commandSender, "该命令只能由玩家来执行！");
            return true;
        }

        final TpRequest request = plugin.getRequestContainer().removeBySrcPlayer(srcPlayer);

        if (request == null) {
            plugin.sendWaring(commandSender, "你没有发起任何传送请求噢");
            return true;
        }

        final Player destPlayer = request.destPlayer();

        plugin.sendInfo(srcPlayer, Component.text()
                .append(Component.text("已取消传送到 ").color(NamedTextColor.GREEN))
                .append(destPlayer.displayName())
                .append(Component.text(" 的请求").color(NamedTextColor.GREEN))
                .build());

        plugin.sendInfo(destPlayer, Component.text()
                .append(srcPlayer.displayName())
                .append(Component.text(" 已取消传送请求").color(NamedTextColor.YELLOW))
                .build());

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        return null;
    }
}
