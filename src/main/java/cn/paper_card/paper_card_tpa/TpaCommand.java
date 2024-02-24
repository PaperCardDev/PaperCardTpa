package cn.paper_card.paper_card_tpa;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
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
                    .append(Component.text("传送冷却，在").color(NamedTextColor.YELLOW))
                    .append(Component.text(PaperCardTpa.formatTime(playerTpCd)).color(NamedTextColor.RED))
                    .append(Component.text("后才能再次传送噢").color(NamedTextColor.YELLOW))
                    .build()
            );
            return true;
        }

        plugin.getTaskScheduler().runTaskAsynchronously(() -> {
            // 检查是否重复请求
            final TpRequest reqOld = plugin.getRequestContainer().removeBySrcPlayer(player);
            if (reqOld != null) {
                final long currentTimeMillis = System.currentTimeMillis();

                // 请求已经过期
                if (currentTimeMillis > reqOld.createTime() + 2 * 60 * 1000L) {
                    plugin.getLogger().info("已自动取消超时的传送请求");
                } else {
                    final Player destPlayer = reqOld.destPlayer();
                    plugin.sendInfo(commandSender, Component.text()
                            .append(Component.text("您已向 ").color(NamedTextColor.YELLOW))
                            .append(destPlayer.displayName())
                            .append(Component.text(" 发起了传送请求，不能再发起新的传送请求，请先取消原请求再重新发起噢").color(NamedTextColor.YELLOW))
                            .appendSpace()
                            .append(Component.text("[点击取消]")
                                    .color(NamedTextColor.GRAY).decorate(TextDecoration.UNDERLINED)
                                    .clickEvent(ClickEvent.runCommand("/tpacancel"))
                                    .hoverEvent(HoverEvent.showText(Component.text("点击执行/tpacancel")))
                            )
                            .build());

                    plugin.getRequestContainer().add(reqOld); // 放回
                    return;
                }
            }

            // 检查末影珍珠
            final int needEnderPeals = this.plugin.getConfigManager().getNeedEnderPeals();

            final TpRequest reqNew;

            if (this.plugin.getUseEnderPeal().checkEnderPearl(player, needEnderPeals)) {
                reqNew = new TpRequest(
                        player,
                        targetPlayer,
                        System.currentTimeMillis(),
                        0,
                        needEnderPeals
                );
            } else {
                // 没有末影珍珠，使用硬币
                final long needCoins = this.plugin.getConfigManager().getNeedCoins();

                final boolean ok;

                try {
                    ok = this.plugin.getUseCoins().checkCoins(player, needCoins);
                } catch (Exception e) {
                    plugin.getSLF4JLogger().error("", e);
                    plugin.sendException(commandSender, e);
                    return;
                }

                if (ok) {
                    reqNew = new TpRequest(
                            player,
                            targetPlayer,
                            System.currentTimeMillis(),
                            needCoins,
                            0
                    );

                } else {
                    final TextComponent.Builder text = Component.text();
                    plugin.appendPrefix(text);
                    text.appendSpace();
                    text.append(Component.text("没有足够的"));
                    text.append(Component.translatable(Material.ENDER_PEARL.translationKey()));
                    text.append(Component.text("（需要%d）".formatted(needEnderPeals)));
                    text.append(Component.text("或"));
                    text.append(Component.text(plugin.getPlayerCoinsApi().getCoinsName()));
                    text.append(Component.text("（需要%d）".formatted(needCoins)));
                    text.append(Component.text("来发起传送请求，请将"));
                    text.append(Component.translatable(Material.ENDER_PEARL.translationKey()));
                    text.append(Component.text("放在主手上"));
                    commandSender.sendMessage(text.build().color(NamedTextColor.YELLOW));
                    return;
                }
            }


            // 添加传送请求
            final boolean added = plugin.getRequestContainer().add(reqNew);

            if (!added) {
                plugin.sendError(commandSender, "当前传送系统繁忙，请稍后重试");
                return;
            }

            // 告知代价
            {
                final TextComponent.Builder text = Component.text();
                plugin.appendPrefix(text);
                text.append(Component.text(" 此次传送将花费"));

                if (reqNew.needEnderPearls() > 0) {
                    text.append(plugin.coinsNumber(reqNew.needEnderPearls()));
                    text.append(Component.translatable(Material.ENDER_PEARL.translationKey()));
                }

                if (reqNew.needCoins() > 0) {
                    text.append(plugin.coinsNumber(reqNew.needCoins()));
                    text.append(Component.text(plugin.getPlayerCoinsApi().getCoinsName()));
                }

                if (reqNew.needCoins() > 0 && reqNew.needEnderPearls() <= 0) {
                    text.append(Component.text("，如需使用"));
                    text.append(Component.translatable(Material.ENDER_PEARL.translationKey()));
                    text.append(Component.text("，请将其放在主手"));
                }


                commandSender.sendMessage(text.build().color(NamedTextColor.GREEN));
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
