package cn.paper_card.paper_card_tpa;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
        if (!(commandSender instanceof final Player player)) {
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

        final Player targetPlayer = plugin.getOnlinePlayerByName(argTargetPlayer);

        if (targetPlayer == null) {
            plugin.sendInfo(commandSender, Component.text()
                    .append(Component.text("找不到该在线玩家：").color(NamedTextColor.YELLOW))
                    .append(Component.text(argTargetPlayer).color(NamedTextColor.RED))
                    .build()
            );
            return true;
        }

        // 检查传送冷却
        final long playerTpCd = plugin.getPlayerTpCd(player);
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

            // 检查硬币是否足够
            try {
                if (!plugin.getUseCoins().checkCoins(player)) return;
            } catch (Exception e) {
                plugin.getSLF4JLogger().error("", e);
                plugin.sendException(commandSender, e);
                return;
            }

            // 检查是否重复请求
            final TpRequest request = plugin.getRequestContainer().removeBySrcPlayer(player);
            if (request != null) {
                final long currentTimeMillis = System.currentTimeMillis();

                // 请求已经过期
                if (currentTimeMillis > request.createTime() + 2 * 60 * 1000L) {
                    plugin.getLogger().info("已自动取消超时的传送请求");
                } else {
                    final Player destPlayer = request.destPlayer();
                    plugin.sendInfo(commandSender, Component.text()
                            .append(Component.text("你已经向 ").color(NamedTextColor.YELLOW))
                            .append(destPlayer.displayName())
                            .append(Component.text(" 发起了一个传送请求，不能再发起新的传送请求，请先取消原请求再重新发起噢").color(NamedTextColor.YELLOW))
                            .appendSpace()
                            .append(Component.text("[点击取消]")
                                    .color(NamedTextColor.GRAY).decorate(TextDecoration.UNDERLINED)
                                    .clickEvent(ClickEvent.runCommand("/tpacancel"))
                                    .hoverEvent(HoverEvent.showText(Component.text("点击执行/tpacancel")))
                            )
                            .build());

                    plugin.getRequestContainer().add(request); // 放回
                    return;
                }
            }

            // 创建传送请求
            final boolean added = plugin.getRequestContainer().add(new TpRequest(player, targetPlayer, System.currentTimeMillis()));

            if (!added) {
                plugin.sendError(commandSender, "当前传送系统繁忙，请稍后重试");
                return;
            }

            //
            final TextComponent build = Component.text()
                    .append(Component.text("你向 ").color(NamedTextColor.GREEN))
                    .append(targetPlayer.displayName())
                    .append(Component.text(" 发起了传送请求，请等待对方响应").color(NamedTextColor.GREEN))
                    .appendSpace()
                    .append(Component.text("[点击取消]")
                            .color(NamedTextColor.GRAY).decorate(TextDecoration.UNDERLINED)
                            .hoverEvent(HoverEvent.showText(Component.text("点击执行/tpacancel")))
                            .clickEvent(ClickEvent.runCommand("/tpacancel")
                            ))
                    .build();

            plugin.sendInfo(commandSender, build);

            // 通知被传送方
            targetPlayer.sendActionBar(Component.text()
                    .append(Component.text("来自 ").color(NamedTextColor.GREEN))
                    .append(player.displayName())
                    .append(Component.text(" 的传送请求").color(NamedTextColor.GREEN))
                    .build());

            final String acceptCmd = "/tpaaccept " + player.getName();
            final String rejectCmd = "/tpadeny " + player.getName();

            final TextComponent build1 = Component.text()
                    .append(Component.text("你收到了来自 ").color(NamedTextColor.GREEN))
                    .append(player.displayName())
                    .append(Component.text(" 的传送请求").color(NamedTextColor.GREEN))
                    .append(Component.newline())
                    .append(Component.text("[点击同意]")
                            .color(NamedTextColor.AQUA).decorate(TextDecoration.UNDERLINED)
                            .hoverEvent(HoverEvent.showText(Component.text("点击执行: %s".formatted(acceptCmd))))
                            .clickEvent(ClickEvent.runCommand(acceptCmd))
                    )
                    .appendSpace()
                    .append(Component.text("[点击拒绝]")
                            .color(NamedTextColor.RED).decorate(TextDecoration.UNDERLINED)
                            .hoverEvent(HoverEvent.showText(Component.text("点击执行: %s".formatted(rejectCmd))))
                            .clickEvent(ClickEvent.runCommand(rejectCmd))
                    )
                    .build();

            plugin.sendInfo(targetPlayer, build1);
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
