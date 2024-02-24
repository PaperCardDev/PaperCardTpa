package cn.paper_card.paper_card_tpa;

import cn.paper_card.player_coins.api.NotEnoughCoinsException;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.TitlePart;
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

        // 消耗末影珍珠或硬币
        final int needEnderPearls = request.needEnderPearls();
        final long needCoins = request.needCoins();

        final TextComponent.Builder text = Component.text();
        plugin.appendPrefix(text);
        text.appendSpace();

        text.append(Component.text("已花费"));
        if (needEnderPearls > 0) {
            if (plugin.getUseEnderPeal().consume(request.srcPlayer(), needEnderPearls)) {
                text.append(plugin.coinsNumber(needEnderPearls));
                text.append(Component.translatable(Material.ENDER_PEARL.translationKey()));
            } else {
                plugin.sendWaring(commandSender, "对方没有足够的足够的末影珍珠来传送！");
                plugin.sendWaring(request.srcPlayer(), "请将%d末影珍珠放在主手！".formatted(needEnderPearls));
                return true;
            }
        }

        if (needCoins > 0) {
            try {
                plugin.getUseCoins().consume(request.srcPlayer(), needCoins, destPlayer.getName());
            } catch (NotEnoughCoinsException e) {
                plugin.sendWaring(commandSender, "对方没有足够的%s来进行传送！".formatted(
                        plugin.getPlayerCoinsApi().getCoinsName()
                ));
                return true;
            } catch (Exception e) {
                plugin.getSLF4JLogger().error("", e);
                plugin.sendException(commandSender, e);
                return true;
            }

            text.append(plugin.coinsNumber(needCoins));
            text.append(Component.text(plugin.getPlayerCoinsApi().getCoinsName()));
        }
        text.append(Component.text("来进行传送"));

        commandSender.sendMessage(text.build().color(NamedTextColor.GREEN));

        plugin.getTaskScheduler().runTaskAsynchronously(() -> {


            final Consumer<ScheduledTask> task = getScheduledTaskConsumer(destPlayer, srcPlayer);

            srcPlayer.getScheduler().runAtFixedRate(plugin, task, null, 20, 20);

            // 通知
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
        });

        return true;
    }

    @NotNull
    private Consumer<ScheduledTask> getScheduledTaskConsumer(Player destPlayer, Player srcPlayer) {
        final AtomicInteger integer = new AtomicInteger(60);

        return (t) -> {
            final int i = integer.get();
            if (i <= 0) {
                // 真正的传送
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
