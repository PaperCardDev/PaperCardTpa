package cn.paper_card.paper_card_tpa;

import cn.paper_card.player_coins.api.NotEnoughCoinsException;
import cn.paper_card.player_coins.api.PlayerCoinsApi;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;


class UseCoins {

    private final @NotNull PaperCardTpa plugin;

    UseCoins(@NotNull PaperCardTpa plugin) {
        this.plugin = plugin;
    }

    void sendNotEnough(@NotNull Player player, long need, long have) {
        final TextComponent.Builder text = Component.text();
        plugin.appendPrefix(text);
        text.appendSpace();
        text.append(Component.text("你没有足够的硬币来进行传送噢，你只有").color(NamedTextColor.YELLOW));
        text.append(Component.text(have).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        text.append(Component.text("枚硬币，一次传送需要").color(NamedTextColor.YELLOW));
        text.append(Component.text(need).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        text.append(Component.text("枚硬币").color(NamedTextColor.YELLOW));

        player.sendMessage(text.build());
    }

    boolean checkCoins(@NotNull Player player) throws Exception {
        final PlayerCoinsApi api = plugin.getPlayerCoinsApi();

        final long coins = api.queryCoins(player.getUniqueId());

        final long needCoins = plugin.getConfigManager().getNeedCoins();

        if (needCoins > coins) {
            this.sendNotEnough(player, needCoins, coins);
            return false;
        }

        final TextComponent.Builder text = Component.text();

        plugin.appendPrefix(text);
        text.appendSpace();
        text.append(Component.text("此次传送将消耗").color(NamedTextColor.GREEN));
        text.append(Component.text(needCoins).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        text.append(Component.text("枚硬币，你现在还有").color(NamedTextColor.GREEN));
        text.append(Component.text(coins).color(NamedTextColor.GOLD));
        text.append(Component.text("枚硬币").color(NamedTextColor.GREEN));

        player.sendMessage(text.build());

        return true;
    }

    boolean consumeCoins(@NotNull Player player) throws Exception {
        final PlayerCoinsApi api = plugin.getPlayerCoinsApi();

        final long needCoins = plugin.getConfigManager().getNeedCoins();

        try {
            api.consumeCoins(player.getUniqueId(), needCoins);
        } catch (NotEnoughCoinsException e) {
            this.sendNotEnough(player, needCoins, e.getLeftCoins());
            return false;
        }

        final TextComponent.Builder text = Component.text();
        plugin.appendPrefix(text);
        text.appendSpace();
        text.append(Component.text("已消耗").color(NamedTextColor.GREEN));
        text.append(Component.text(needCoins).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        text.append(Component.text("枚硬币来进行传送~").color(NamedTextColor.GREEN));

        player.sendMessage(text.build());

        return true;
    }
}
