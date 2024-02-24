package cn.paper_card.paper_card_tpa;

import org.jetbrains.annotations.NotNull;

class ConfigManager {

    private final @NotNull PaperCardTpa plugin;

    private final static String PATH_NEED_COINS = "need-coins";

    private final static String PATH_NEED_ENDER_PEALS = "need-ender-peals";

    private final static String PATH_COOL_DOWN = "cool-down";

    ConfigManager(@NotNull PaperCardTpa plugin) {
        this.plugin = plugin;
    }


    long getCoolDown() {
        return this.plugin.getConfig().getLong(PATH_COOL_DOWN, 60 * 1000L);
    }

    void setCoolDown(long v) {
        this.plugin.getConfig().set(PATH_COOL_DOWN, v);
    }

    long getNeedCoins() {
        return this.plugin.getConfig().getLong(PATH_NEED_COINS, 1);
    }

    void setNeedCoins(long v) {
        this.plugin.getConfig().set(PATH_NEED_COINS, v);
    }

    int getNeedEnderPeals() {
        return this.plugin.getConfig().getInt(PATH_NEED_ENDER_PEALS, 4);
    }

    void setNeedEnderPeals(int v) {
        this.plugin.getConfig().set(PATH_NEED_ENDER_PEALS, v);
    }

    void setDefaults() {
        this.setCoolDown(this.getCoolDown());
        this.setNeedCoins(this.getNeedCoins());
        this.setNeedEnderPeals(this.getNeedEnderPeals());
    }

    void save() {
        this.plugin.saveConfig();
    }

    void reload() {
        this.plugin.reloadConfig();
    }

}
