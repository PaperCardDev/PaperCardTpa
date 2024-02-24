package cn.paper_card.paper_card_tpa;

import cn.paper_card.player_coins.api.PlayerCoinsApi;
import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.UUID;

public final class PaperCardTpa extends JavaPlugin {

    private final @NotNull HashMap<UUID, Long> lastTp; // 玩家上次传送的时间
    private final @NotNull RequestContainer requestContainer;

    private final @NotNull ConfigManager configManager;

    private final @NotNull TaskScheduler taskScheduler;

    private final @NotNull UseCoins useCoins;

    private final @NotNull UseEnderPeal useEnderPeal;

    private PlayerCoinsApi playerCoinsApi = null;

    public PaperCardTpa() {
        this.lastTp = new HashMap<>();
        this.requestContainer = new RequestContainer();

        this.configManager = new ConfigManager(this);
        this.taskScheduler = UniversalScheduler.getScheduler(this);

        this.useCoins = new UseCoins(this);
        this.useEnderPeal = new UseEnderPeal(this);
    }

    void appendPrefix(@NotNull TextComponent.Builder text) {
        text.append(Component.text("[").color(NamedTextColor.LIGHT_PURPLE));
        text.append(Component.text("TPA传送").color(NamedTextColor.GOLD));
        text.append(Component.text("]").color(NamedTextColor.LIGHT_PURPLE));
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
        this.playerCoinsApi = this.getServer().getServicesManager().load(PlayerCoinsApi.class);

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

        new MainCommand(this);

        this.configManager.setDefaults();
        this.configManager.save();
    }

    @Override
    public void onDisable() {
        this.taskScheduler.cancelTasks(this);
        this.configManager.save();
        this.playerCoinsApi = null;
    }

    @NotNull PlayerCoinsApi getPlayerCoinsApi() {
        return this.playerCoinsApi;
    }

    @NotNull ConfigManager getConfigManager() {
        return this.configManager;
    }

    @NotNull TaskScheduler getTaskScheduler() {
        return this.taskScheduler;
    }

    @NotNull UseCoins getUseCoins() {
        return this.useCoins;
    }

    @NotNull UseEnderPeal getUseEnderPeal() {
        return this.useEnderPeal;
    }

    @NotNull Permission addPermission(@NotNull String name) {
        final Permission p = new Permission(name);
        this.getServer().getPluginManager().addPermission(p);
        return p;
    }

    @NotNull TextComponent coinsNumber(long c) {
        return Component.text(c).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD);
    }

    void sendInfo(@NotNull CommandSender sender, @NotNull TextComponent message) {
        final TextComponent.Builder text = Component.text();
        this.appendPrefix(text);

        sender.sendMessage(text
                .appendSpace()
                .append(message)
                .build());
    }

    void sendInfo(@NotNull CommandSender sender, @NotNull String info) {
        final TextComponent.Builder text = Component.text();
        this.appendPrefix(text);

        sender.sendMessage(text
                .appendSpace()
                .append(Component.text(info).color(NamedTextColor.GREEN))
                .build());
    }

    void sendError(@NotNull CommandSender sender, @NotNull String error) {
        final TextComponent.Builder text = Component.text();
        this.appendPrefix(text);

        sender.sendMessage(text
                .appendSpace()
                .append(Component.text(error).color(NamedTextColor.RED))
                .build());
    }

    void sendException(@NotNull CommandSender sender, @NotNull Throwable e) {
        final TextComponent.Builder text = Component.text();
        this.appendPrefix(text);
        text.appendSpace();
        text.append(Component.text("==== 异常信息 ====").color(NamedTextColor.DARK_RED));

        for (Throwable t = e; t != null; t = t.getCause()) {
            text.appendNewline();
            text.append(Component.text(t.toString()).color(NamedTextColor.RED));
        }

        sender.sendMessage(text.build());
    }

    void sendWaring(@NotNull CommandSender sender, @NotNull String warning) {
        final TextComponent.Builder text = Component.text();
        this.appendPrefix(text);

        sender.sendMessage(text
                .appendSpace()
                .append(Component.text(warning).color(NamedTextColor.YELLOW))
                .build());
    }

}
