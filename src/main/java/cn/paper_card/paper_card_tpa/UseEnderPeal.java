package cn.paper_card.paper_card_tpa;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

class UseEnderPeal {

    private final @NotNull PaperCardTpa plugin;

    UseEnderPeal(@NotNull PaperCardTpa plugin) {
        this.plugin = plugin;
    }

    // 检查玩家的手里是否有足够的末影珍珠来进行传送，有返回true
    boolean checkEnderPearl(@NotNull Player player, int need) {
        final PlayerInventory inventory = player.getInventory();

        final ItemStack item = inventory.getItemInMainHand();

        final Material type = item.getType();
        if (type != Material.ENDER_PEARL) {
            return false;
        }

        if (item.getAmount() >= need) {
            return true;
        }

        final TextComponent.Builder text = Component.text();
        this.plugin.appendPrefix(text);
        text.appendSpace();
        text.append(Component.text("手上没有足够的"));
        text.append(Component.translatable(Material.ENDER_PEARL.translationKey()));
        text.append(Component.text("，需要"));
        text.append(plugin.coinsNumber(need));
        text.append(Component.text("，拥有"));
        text.append(plugin.coinsNumber(item.getAmount()));

        player.sendMessage(text.build().color(NamedTextColor.YELLOW));
        return false;
    }

    boolean consume(@NotNull Player player, int need) {
        final PlayerInventory inventory = player.getInventory();

        final ItemStack item = inventory.getItemInMainHand();

        final Material type = item.getType();
        if (type != Material.ENDER_PEARL) {
            return false;
        }

        int amount = item.getAmount();

        if (amount < need) return false;

        amount -= need;
        item.setAmount(amount);
        return true;
    }
}
