package cn.paper_card.paper_card_tpa;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

record TpRequest(
        @NotNull Player sender, // 传送请求发起者

        @NotNull Player receiver, // 传送请求的接收

        boolean tpToReceiver, // 传送方向

        long createTime, // 创建时间
        long needCoins, // 将要花费的Coins
        int needEnderPearls // 将要花费的末影珍珠
) {
}
