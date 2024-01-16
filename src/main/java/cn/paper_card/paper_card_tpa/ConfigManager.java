package cn.paper_card.paper_card_tpa;

import org.jetbrains.annotations.NotNull;

class ConfigManager {

    private final @NotNull PaperCardTpa plugin;

    ConfigManager(@NotNull PaperCardTpa plugin) {
        this.plugin = plugin;
    }

    int getNeedEnderPearl() {
        return this.plugin.getConfig().getInt("need-ender-pearl", 4);
    }

    long getCoolDown() {
        return this.plugin.getConfig().getLong("cool-down", 5 * 60 * 1000L);
    }
}
