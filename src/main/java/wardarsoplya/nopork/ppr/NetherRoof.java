package wardarsoplya.nopork.ppr;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.WorldBorder;

/** Works out where on the nether ceiling a given location maps to. */
final class NetherRoof {

    private NetherRoof() {
    }

    /**
     * Returns the spot on the nether ceiling matching {@code from}, or
     * {@code null} when the server has no nether world at all.
     */
    static Location resolve(Server server, Location from, PorkSettings settings) {
        World source = from.getWorld();
        World nether = findNether(server, source);
        if (nether == null) {
            return null;
        }

        double x = from.getX();
        double z = from.getZ();
        if (settings.scaleCoordinates() && nether != source) {
            // Same ratio a portal uses: overworld scale 1 over nether scale 8.
            double scale = source.getCoordinateScale() / nether.getCoordinateScale();
            x *= scale;
            z *= scale;
        }

        // The nether's world height is 256, but its *logical* height is 128 and
        // that is where the bedrock ceiling sits (solid bedrock at y=127), so
        // getLogicalHeight() is the first free block above it: the roof itself.
        // A custom nether with no ceiling falls back to its build limit.
        int roof = nether.hasCeiling() ? nether.getLogicalHeight() : nether.getMaxHeight();
        double y = roof + settings.teleportYOffset();
        Location target = new Location(nether, x, y, z, from.getYaw(), from.getPitch());
        clampInsideBorder(target);
        return target;
    }

    private static World findNether(Server server, World source) {
        if (source.getEnvironment() == World.Environment.NETHER) {
            return source;
        }
        World linked = server.getWorld(source.getName() + "_nether");
        if (linked != null && linked.getEnvironment() == World.Environment.NETHER) {
            return linked;
        }
        for (World world : server.getWorlds()) {
            if (world.getEnvironment() == World.Environment.NETHER) {
                return world;
            }
        }
        return null;
    }

    private static void clampInsideBorder(Location target) {
        WorldBorder border = target.getWorld().getWorldBorder();
        Location center = border.getCenter();
        double half = Math.max(0.0D, border.getSize() / 2.0D - 2.0D);
        target.setX(clamp(target.getX(), center.getX() - half, center.getX() + half));
        target.setZ(clamp(target.getZ(), center.getZ() - half, center.getZ() + half));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
