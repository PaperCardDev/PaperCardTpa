package cn.paper_card.paper_card_tpa;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
class UseEnderPeal {
    // 检查玩家的背包是否有足够的末影珍珠来进行传送，有返回true
    boolean checkEnderPearl(@NotNull Player player, int need) {

        if (need <= 0) return true;

        final PlayerInventory inventory = player.getInventory();

        int c = 0;

        int i = 4 * 9;
        while (--i >= 0) {
            final ItemStack item = inventory.getItem(i);

            // 空格子
            if (item == null) continue;

            // 不是末影珍珠
            final Material type = item.getType();
            if (!type.equals(Material.ENDER_PEARL)) continue;

            final int amount = item.getAmount();

            c += amount;

            if (c >= need) return true;
        }

        return false;
    }

    // 消耗玩家的末影珍珠，消耗成功返回true
    boolean consumeEnderPearl(@NotNull Player player, int need) {

        if (need <= 0) return true;

        final PlayerInventory inventory = player.getInventory();

        int i = 4 * 9;

        int c = need; // 表示还差几个末影珍珠

        while (--i >= 0) { // 遍历背包
            final ItemStack item = inventory.getItem(i);

            // 空格子
            if (item == null) continue;

            // 不是末影珍珠
            final Material type = item.getType();
            if (!type.equals(Material.ENDER_PEARL)) continue;

            final int amount = item.getAmount();

            int amountNew = amount - c;

            if (amountNew < 0) {
                inventory.setItem(i, null);
                c -= amount;
            } else {
                item.setAmount(amountNew);
                inventory.setItem(i, item);
                return true;
            }
        }

        // 如果没有足够的末影珍珠，归还移出的末影珍珠到背包
        final int back = need - c;
        i = 4 * 9;
        while (--i >= 0) {
            final ItemStack item = inventory.getItem(i);
            if (item != null) continue;

            final ItemStack itemStack = new ItemStack(Material.ENDER_PEARL);
            itemStack.setAmount(back);
            inventory.setItem(i, itemStack);
            break;
        }

        return false;
    }

}
