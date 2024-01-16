package cn.paper_card.paper_card_tpa;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.UUID;

public final class PaperCardTpa extends JavaPlugin {

    private final @NotNull HashMap<UUID, Long> lastTp; // 玩家上次传送的时间
    private final @NotNull RequestContainer requestContainer;

    private final @NotNull TextComponent prefix;

    private final @NotNull ConfigManager configManager;

    public PaperCardTpa() {
        this.lastTp = new HashMap<>();
        this.requestContainer = new RequestContainer();
        this.prefix = Component.text()
                .append(Component.text("[").color(NamedTextColor.LIGHT_PURPLE))
                .append(Component.text("TPA传送").color(NamedTextColor.GOLD))
                .append(Component.text("]").color(NamedTextColor.LIGHT_PURPLE))
                .build();

        this.configManager = new ConfigManager(this);
    }

    @Nullable Long getPlayerLastTp(@NotNull Player player) {
        synchronized (this.lastTp) {
            return this.lastTp.get(player.getUniqueId());
        }
    }

    void setPlayerLastTp(@NotNull Player player, long time) {
        synchronized (this.lastTp) {
            this.lastTp.put(player.getUniqueId(), time);
        }
    }

    // 获取玩家距离传送冷却的生于时间，负数表示没有传送冷却
    long getPlayerTpCd(@NotNull Player player) {

        final Long playerLastTp = this.getPlayerLastTp(player);

        if (playerLastTp == null) return -1;

        final long currentTimeMillis = System.currentTimeMillis();

        final long cdEnd = playerLastTp + this.configManager.getCoolDown();

        return cdEnd - currentTimeMillis;
    }

    @NotNull RequestContainer getRequestContainer() {
        return this.requestContainer;
    }

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

    static @NotNull String formatTime(long ms) {
        ms /= 1000;
        ms += 1;
        final long minutes = ms / 60;

        ms -= minutes * 60;

        if (minutes == 0) {
            return "%d秒".formatted(ms);
        } else {
            return "%d分%d秒".formatted(minutes, ms);
        }
    }

    @Nullable Player getOnlinePlayerByName(@NotNull String name) {
        for (Player onlinePlayer : this.getServer().getOnlinePlayers()) {
            if (onlinePlayer.getName().equals(name)) return onlinePlayer;
        }
        return null;
    }

    @Override
    public void onEnable() {
        final PluginCommand tpaCmd = this.getCommand("tpa");
        final TpaCommand tpaCommand = new TpaCommand(this);
        assert tpaCmd != null;
        tpaCmd.setExecutor(tpaCommand);
        tpaCmd.setTabCompleter(tpaCommand);

        final PluginCommand tpaAcceptCmd = this.getCommand("tpaaccept");
        final TpaAcceptCommand tpaAcceptCommand = new TpaAcceptCommand(this);
        assert tpaAcceptCmd != null;
        tpaAcceptCmd.setExecutor(tpaAcceptCommand);
        tpaAcceptCmd.setTabCompleter(tpaAcceptCommand);

        final PluginCommand tpaDenyCmd = this.getCommand("tpadeny");
        final TpaDenyCommand tpaDenyCommand = new TpaDenyCommand(this);
        assert tpaDenyCmd != null;
        tpaDenyCmd.setExecutor(tpaDenyCommand);
        tpaDenyCmd.setTabCompleter(tpaDenyCommand);

        final PluginCommand tpaCancelCmd = this.getCommand("tpacancel");
        final TpaCancelCommand tpaCancelCommand = new TpaCancelCommand(this);
        assert tpaCancelCmd != null;
        tpaCancelCmd.setExecutor(tpaCancelCommand);
        tpaCancelCmd.setTabCompleter(tpaCancelCommand);

    }

    @Override
    public void onDisable() {
        this.saveConfig();
    }

    @NotNull ConfigManager getConfigManager() {
        return this.configManager;
    }

    void sendInfo(@NotNull CommandSender sender, @NotNull TextComponent message) {
        sender.sendMessage(Component.text()
                .append(this.prefix)
                .appendSpace()
                .append(message)
                .build());
    }

    void sendError(@NotNull CommandSender sender, @NotNull String error) {
        sender.sendMessage(Component.text()
                .append(this.prefix)
                .appendSpace()
                .append(Component.text(error).color(NamedTextColor.RED))
                .build());
    }

    void sendWaring(@NotNull CommandSender sender, @NotNull String warning) {
        sender.sendMessage(Component.text()
                .append(this.prefix)
                .appendSpace()
                .append(Component.text(warning).color(NamedTextColor.YELLOW))
                .build());
    }

}
