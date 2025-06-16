package gg.kite.model;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public record Island(UUID owner, IslandType type, Location center, int borderSize,
                     Map<String, Integer> upgrades, Set<UUID> members) {
    public Island {
        Objects.requireNonNull(owner, "Owner cannot be null");
        Objects.requireNonNull(type, "Type cannot be null");
        Objects.requireNonNull(center, "Center cannot be null");
        borderSize = Math.max(75, borderSize);
        upgrades = new HashMap<>(upgrades);
        members = new HashSet<>(members);
    }

    @Contract("_, _, _ -> new")
    public static @NotNull Island createNew(@NotNull Player player, IslandType type, Location center) {
        return new Island(player.getUniqueId(), type, center, 75,
                new HashMap<>(), new HashSet<>());
    }

    public boolean isWithinBorder(@NotNull Location location) {
        double halfSize = borderSize / 2.0;
        return location.getWorld().equals(center.getWorld()) &&
                Math.abs(location.getX() - center.getX()) <= halfSize &&
                Math.abs(location.getZ() - center.getZ()) <= halfSize;
    }
}