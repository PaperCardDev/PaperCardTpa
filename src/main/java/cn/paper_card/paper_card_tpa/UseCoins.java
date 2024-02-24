package cn.paper_card.paper_card_tpa;

import cn.paper_card.player_coins.api.PlayerCoinsApi;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;


class UseCoins {

    private final @NotNull PaperCardTpa plugin;

    UseCoins(@NotNull PaperCardTpa plugin) {
        this.plugin = plugin;
    }

    boolean checkCoins(@NotNull Player player, long needCoins) throws Exception {
        final PlayerCoinsApi api = plugin.getPlayerCoinsApi();
        final long coins = api.queryCoins(player.getUniqueId());
        return coins >= needCoins;
    }

    void consume(@NotNull Player player, long needCoins, @NotNull String target) throws Exception {
        final PlayerCoinsApi api = plugin.getPlayerCoinsApi();
        api.consumeCoins(player.getUniqueId(), needCoins, "TPA传送：%s -> %s".formatted(
                player.getName(), target
        ));
    }
}
