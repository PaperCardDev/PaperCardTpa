package cn.paper_card.paper_card_tpa;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class Util {

    static @Nullable TpRequest checkCost(@NotNull PaperCardTpa plugin,
                                         @NotNull Player sender,
                                         @NotNull Player receiver,
                                         boolean tpToReceiver) {
        // 检查末影珍珠
        final int needEnderPeals = plugin.getConfigManager().getNeedEnderPeals();

        final TpRequest reqNew;

        final Boolean checkEnderPearl = plugin.getUseEnderPeal().checkEnderPearl(sender, needEnderPeals);

        if (checkEnderPearl == null) {
            // 不是末影珍珠，使用Coins
            final long needCoins = plugin.getConfigManager().getNeedCoins();

            final boolean ok;

            try {
                ok = plugin.getUseCoins().checkCoins(sender, needCoins);
            } catch (Exception e) {
                plugin.getSLF4JLogger().error("", e);
                plugin.sendException(sender, e);
                return null;
            }

            if (ok) {
                reqNew = new TpRequest(
                        sender,
                        receiver,
                        tpToReceiver,
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
                sender.sendMessage(text.build().color(NamedTextColor.YELLOW));
                return null;
            }
        } else {
            if (checkEnderPearl) {
                // 有足够的的末影珍珠
                reqNew = new TpRequest(
                        sender,
                        receiver,
                        tpToReceiver,
                        System.currentTimeMillis(),
                        0,
                        needEnderPeals
                );
            } else {
                // 没有足够的末影珍珠
                return null;
            }
        }

        return reqNew;
    }

    static void sendRep(@NotNull PaperCardTpa plugin, @NotNull Player sender, @NotNull Player receiver) {

        plugin.sendInfo(sender, Component.text()
                .append(Component.text("您已向 ").color(NamedTextColor.YELLOW))
                .append(receiver.displayName())
                .append(Component.text(" 发起了传送请求，不能再发起新的传送请求，请先取消原请求再重新发起噢").color(NamedTextColor.YELLOW))
                .appendSpace()
                .append(Component.text("[点击取消]")
                        .color(NamedTextColor.GRAY).decorate(TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.runCommand("/tpacancel"))
                        .hoverEvent(HoverEvent.showText(Component.text("点击执行/tpacancel")))
                )
                .build());
    }


    static void sendNotify(@NotNull PaperCardTpa plugin, @NotNull TpRequest request) {
        final Player sender = request.sender();
        final Player receiver = request.receiver();

        //
        final TextComponent build = Component.text()
                .append(Component.text("你向 ").color(NamedTextColor.GREEN))
                .append(receiver.displayName())
                .append(Component.text(" 发起了传送请求，请等待对方响应").color(NamedTextColor.GREEN))
                .appendSpace()
                .append(Component.text("[点击取消]")
                        .color(NamedTextColor.GRAY).decorate(TextDecoration.UNDERLINED)
                        .hoverEvent(HoverEvent.showText(Component.text("点击执行/tpacancel")))
                        .clickEvent(ClickEvent.runCommand("/tpacancel")
                        ))
                .build().color(NamedTextColor.GREEN);

        plugin.sendInfo(sender, build);

        // 通知被传送方
        if (request.tpToReceiver()) {
            receiver.sendActionBar(Component.text()
                    .append(sender.displayName())
                    .append(Component.text(" 想传送到你这儿~"))
                    .build().color(NamedTextColor.GREEN));
        } else {
            receiver.sendActionBar(Component.text()
                    .append(sender.displayName())
                    .append(Component.text(" 邀请你到他那儿~"))
                    .build().color(NamedTextColor.GREEN));
        }

        final String acceptCmd = "/tpaaccept " + sender.getName();
        final String rejectCmd = "/tpadeny " + sender.getName();

        final TextComponent.Builder builder = Component.text();

        if (request.tpToReceiver()) {
            builder.append(sender.displayName())
                    .append(Component.text(" 想传送到你这儿~"));
        } else {
            builder.append(sender.displayName())
                    .append(Component.text(" 邀请你到他那儿~"));
        }


        final TextComponent msg = builder.append(Component.newline())
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
                .build().color(NamedTextColor.GREEN);

        plugin.sendInfo(receiver, msg);
    }

    // 告知代价
    static void sendCost(@NotNull PaperCardTpa plugin, @NotNull TpRequest reqNew) {

        final TextComponent.Builder text = Component.text();
        plugin.appendPrefix(text);
        text.append(Component.text(" 此次传送将花费 "));

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

        reqNew.sender().sendMessage(text.build().color(NamedTextColor.GREEN));
    }
}
