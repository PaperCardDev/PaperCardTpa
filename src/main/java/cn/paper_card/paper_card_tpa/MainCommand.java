package cn.paper_card.paper_card_tpa;

import cn.paper_card.mc_command.TheMcCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

class MainCommand extends TheMcCommand.HasSub {

    private final @NotNull Permission permission;

    private final @NotNull PaperCardTpa plugin;

    public MainCommand(@NotNull PaperCardTpa plugin) {
        super("paper-card-tpa");
        this.plugin = plugin;
        this.permission = Objects.requireNonNull(plugin.getServer().getPluginManager().getPermission("paper-card-tpa.command"));

        final PluginCommand c = plugin.getCommand(this.getLabel());
        assert c != null;
        c.setExecutor(this);
        c.setTabCompleter(this);

        this.addSubCommand(new Reload());
    }

    @Override
    protected boolean canNotExecute(@NotNull CommandSender commandSender) {
        return !commandSender.hasPermission(this.permission);
    }

    class Reload extends TheMcCommand {

        private final @NotNull Permission permission;

        Reload() {
            super("reload");
            this.permission = plugin.addPermission(MainCommand.this.permission.getName() + "." + this.getLabel());
        }

        @Override
        protected boolean canNotExecute(@NotNull CommandSender commandSender) {
            return !commandSender.hasPermission(this.permission);
        }

        @Override
        public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {

            plugin.getConfigManager().reload();
            plugin.sendInfo(commandSender);
            return true;
        }

        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
            return null;
        }
    }
}
