package wardarsoplya.nopork.ppr;

import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;

/** Applies the punishment whenever a player finishes eating pork. */
final class PorkConsumeListener implements Listener {

    static final String EXEMPT_PERMISSION = "nopork.exempt";

    private final NoPorkPlugin plugin;

    PorkConsumeListener(NoPorkPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        PorkSettings settings = plugin.settings();
        Material eaten = event.getItem().getType();
        Player player = event.getPlayer();

        if (!settings.foods().contains(eaten)
                || player.hasPermission(EXEMPT_PERMISSION)
                || !settings.worlds().allows(player.getWorld())) {
            return;
        }

        // The event fires before the item is actually consumed. Punishing on the
        // next tick lets vanilla finish eating first, so our effect durations are
        // not clipped and a teleport cannot land mid-consume.
        player.getScheduler().run(plugin, task -> punish(player, eaten, settings), null);
    }

    private void punish(Player player, Material eaten, PorkSettings settings) {
        if (!player.isOnline()) {
            return;
        }

        for (PotionEffect effect : settings.effects()) {
            if (settings.overrideExistingEffects()) {
                // Otherwise a longer or stronger effect already running would
                // swallow this one and the punishment would go unnoticed.
                player.removePotionEffect(effect.getType());
            }
            player.addPotionEffect(effect);
        }

        Sound sound = settings.sound();
        if (sound != null) {
            player.playSound(player.getLocation(), sound, SoundCategory.PLAYERS,
                    settings.soundVolume(), settings.soundPitch());
        }

        TagResolver base = TagResolver.resolver(
                Placeholder.unparsed("player", player.getName()),
                Placeholder.unparsed("item", eaten.key().asString()));

        Component punished = render(settings.messages().punished(), base);
        if (punished != null) {
            player.sendActionBar(punished);
        }

        if (settings.teleportEnabled()
                && ThreadLocalRandom.current().nextInt(settings.teleportChance()) == 0) {
            teleportToRoof(player, settings, base);
        }
    }

    private void teleportToRoof(Player player, PorkSettings settings, TagResolver base) {
        Location target = NetherRoof.resolve(plugin.getServer(), player.getLocation(), settings);
        if (target == null) {
            Component noNether = render(settings.messages().noNether(), base);
            if (noNether != null) {
                player.sendMessage(noNether);
            }
            plugin.getLogger().warning("No nether world found; cannot teleport " + player.getName() + ".");
            return;
        }

        player.teleportAsync(target, PlayerTeleportEvent.TeleportCause.PLUGIN).thenAccept(success -> {
            if (!success) {
                return;
            }
            TagResolver resolvers = TagResolver.resolver(base, positionOf(target));
            Component teleported = render(settings.messages().teleported(), resolvers);
            if (teleported != null) {
                player.sendMessage(teleported);
            }
            Component log = render(settings.messages().logTeleport(), resolvers);
            if (log != null) {
                plugin.getComponentLogger().info(log);
            }
        });
    }

    private static TagResolver positionOf(Location location) {
        return TagResolver.resolver(
                Placeholder.unparsed("x", String.valueOf(location.getBlockX())),
                Placeholder.unparsed("y", String.valueOf(location.getBlockY())),
                Placeholder.unparsed("z", String.valueOf(location.getBlockZ())),
                Placeholder.unparsed("world", location.getWorld().getName()));
    }

    /** Renders a configured MiniMessage string, or {@code null} if it is disabled. */
    static Component render(String raw, TagResolver resolvers) {
        return raw == null ? null : MiniMessage.miniMessage().deserialize(raw, resolvers);
    }
}
