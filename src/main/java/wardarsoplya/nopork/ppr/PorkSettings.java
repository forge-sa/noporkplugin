package wardarsoplya.nopork.ppr;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * An immutable snapshot of config.yml. Loading never throws: anything the
 * server cannot resolve is logged and skipped so a typo in one entry does not
 * take the whole plugin down.
 */
public record PorkSettings(
        Set<Material> foods,
        List<PotionEffect> effects,
        boolean overrideExistingEffects,
        boolean teleportEnabled,
        int teleportChance,
        int teleportYOffset,
        boolean scaleCoordinates,
        WorldFilter worlds,
        Sound sound,
        float soundVolume,
        float soundPitch,
        Messages messages) {

    /** Which worlds pork is punished in. */
    public record WorldFilter(Mode mode, Set<String> names) {

        public enum Mode { ALL, WHITELIST, BLACKLIST }

        public boolean allows(World world) {
            return switch (mode) {
                case ALL -> true;
                case WHITELIST -> names.contains(world.getName());
                case BLACKLIST -> !names.contains(world.getName());
            };
        }
    }

    /** Raw MiniMessage strings; an empty entry is stored as {@code null}. */
    public record Messages(
            String punished,
            String teleported,
            String logTeleport,
            String noNether,
            String reloaded) {
    }

    public static PorkSettings load(ConfigurationSection config, Logger logger) {
        return new PorkSettings(
                loadFoods(config, logger),
                loadEffects(config.getConfigurationSection("effects"), logger),
                config.getBoolean("override-existing-effects", true),
                config.getBoolean("teleport.enabled", true),
                Math.max(1, config.getInt("teleport.chance-denominator", 20)),
                config.getInt("teleport.y-offset", 0),
                config.getBoolean("teleport.scale-coordinates", true),
                loadWorldFilter(config, logger),
                loadSound(config.getString("sound.id"), logger),
                (float) config.getDouble("sound.volume", 1.0D),
                (float) config.getDouble("sound.pitch", 0.7D),
                new Messages(
                        trimmedOrNull(config.getString("messages.punished")),
                        trimmedOrNull(config.getString("messages.teleported")),
                        trimmedOrNull(config.getString("messages.log-teleport")),
                        trimmedOrNull(config.getString("messages.no-nether")),
                        trimmedOrNull(config.getString("messages.reloaded"))));
    }

    private static Set<Material> loadFoods(ConfigurationSection config, Logger logger) {
        Set<Material> foods = EnumSet.noneOf(Material.class);
        for (String id : config.getStringList("foods")) {
            Material material = Material.matchMaterial(id);
            if (material == null) {
                logger.warning("Unknown item in foods: " + id + " (ignored)");
                continue;
            }
            foods.add(material);
        }
        if (foods.isEmpty()) {
            logger.warning("No valid items configured under foods; falling back to porkchop and cooked_porkchop.");
            foods.add(Material.PORKCHOP);
            foods.add(Material.COOKED_PORKCHOP);
        }
        return Set.copyOf(foods);
    }

    private static List<PotionEffect> loadEffects(ConfigurationSection section, Logger logger) {
        List<PotionEffect> effects = new ArrayList<>();
        if (section == null) {
            return List.of();
        }
        var registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.MOB_EFFECT);
        for (String id : section.getKeys(false)) {
            NamespacedKey key = NamespacedKey.fromString(id.toLowerCase(Locale.ROOT));
            PotionEffectType type = key == null ? null : registry.get(key);
            if (type == null) {
                logger.warning("Unknown potion effect: " + id + " (ignored)");
                continue;
            }
            int seconds = section.getInt(id + ".duration-seconds", 20);
            int amplifier = section.getInt(id + ".amplifier", 0);
            if (seconds <= 0) {
                logger.warning("Effect " + id + " has a duration of " + seconds + "s (ignored)");
                continue;
            }
            effects.add(new PotionEffect(type, seconds * 20, Math.max(0, amplifier), false, true, true));
        }
        return List.copyOf(effects);
    }

    private static WorldFilter loadWorldFilter(ConfigurationSection config, Logger logger) {
        String raw = config.getString("worlds.mode", "all");
        WorldFilter.Mode mode;
        try {
            mode = WorldFilter.Mode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            logger.warning("Unknown worlds.mode: " + raw + " (using 'all')");
            mode = WorldFilter.Mode.ALL;
        }
        return new WorldFilter(mode, Set.copyOf(new HashSet<>(config.getStringList("worlds.list"))));
    }

    private static Sound loadSound(String id, Logger logger) {
        String trimmed = trimmedOrNull(id);
        if (trimmed == null) {
            return null;
        }
        NamespacedKey key = NamespacedKey.fromString(trimmed.toLowerCase(Locale.ROOT));
        Sound sound = key == null
                ? null
                : RegistryAccess.registryAccess().getRegistry(RegistryKey.SOUND_EVENT).get(key);
        if (sound == null) {
            logger.warning("Unknown sound: " + trimmed + " (no sound will play)");
        }
        return sound;
    }

    private static String trimmedOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
