package cn.paper_card.paper_card_tpa;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

record TpRequest(@NotNull Player srcPlayer, @NotNull Player destPlayer, long createTime
        , long needCoins, int needEnderPearls) {
}
