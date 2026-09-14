package wardarsoplya.nopork.ppr;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Punishes players for eating pork, raw or cooked.
 *
 * <p>Everything happens on the server: no resource pack, mod or client-side
 * companion is involved, so vanilla clients are punished just the same.
 */
public final class NoPorkPlugin extends JavaPlugin {

    private volatile PorkSettings settings;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.settings = PorkSettings.load(getConfig(), getLogger());
        getServer().getPluginManager().registerEvents(new PorkConsumeListener(this), this);

        var command = getCommand("nopork");
        if (command != null) {
            command.setExecutor(new NoPorkCommand(this));
        }
    }

    /** The current configuration. Replaced wholesale on reload, never mutated. */
    public PorkSettings settings() {
        return settings;
    }

    /** Re-reads config.yml and swaps in the new settings. */
    public void reloadSettings() {
        reloadConfig();
        this.settings = PorkSettings.load(getConfig(), getLogger());
    }
}
