package cn.paper_card.paper_card_tpa;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

class ConfigManager {

    private final @NotNull PaperCardTpa plugin;

    ConfigManager(@NotNull PaperCardTpa plugin) {
        this.plugin = plugin;
    }


    long getCoolDown() {

        final String path = "cool-down";
        final long def = 60 * 1000;

        final FileConfiguration c = this.plugin.getConfig();

        if (!c.contains(path, true)) {
            c.set(path, def);
            c.setComments(path, Collections.singletonList("玩家传送冷却时间，以毫秒为单位，默认值：60x1000，一分钟"));
        }

        return c.getLong(path, def);
    }

    long getNeedCoins() {
        final String path = "need-coins";
        final long def = 1;

        final FileConfiguration c = this.plugin.getConfig();

        if (!c.contains(path, true)) {
            c.set(path, def);
            c.setComments(path, Collections.singletonList("玩家一次传送需要消耗的Coins（叫硬币或电池），默认值：1"));
        }

        return c.getLong(path, def);
    }

    int getNeedEnderPeals() {
        final String path = "need-ender-peals";
        final int def = 4;

        final FileConfiguration c = this.plugin.getConfig();

        if (!c.contains(path, true)) {
            c.set(path, def);
            c.setComments(path, Collections.singletonList("玩家一次传送需要消耗的末影珍珠，默认值：4"));
        }

        return c.getInt(path, def);
    }


    void getAll() {
        this.getCoolDown();
        this.getNeedCoins();
        this.getNeedEnderPeals();
    }

    void save() {
        this.plugin.saveConfig();
    }

    void reload() {
        this.plugin.reloadConfig();
    }
}
