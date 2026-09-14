package wardarsoplya.nopork.ppr;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

/** {@code /nopork reload}. */
final class NoPorkCommand implements CommandExecutor, TabCompleter {

    private final NoPorkPlugin plugin;

    NoPorkCommand(NoPorkPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1 || !args[0].equalsIgnoreCase("reload")) {
            return false;
        }
        plugin.reloadSettings();
        Component message = PorkConsumeListener.render(
                plugin.settings().messages().reloaded(), TagResolver.empty());
        sender.sendMessage(message == null ? Component.text("NoPork configuration reloaded.") : message);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return args.length == 1 ? List.of("reload") : List.of();
    }
}
