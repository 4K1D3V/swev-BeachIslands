package gg.kite.service;

import com.google.inject.Inject;
import gg.kite.model.Island;
import gg.kite.model.IslandType;
import gg.kite.model.UpgradeType;
import gg.kite.storage.DatabaseService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class IslandService {
    private final JavaPlugin plugin;
    private final DatabaseService databaseService;
    private final SchematicService schematicService;
    private final EconomyService economyService;
    private final Map<UUID, Island> islandsByOwner;
    private final World oceanWorld;

    @Inject
    public IslandService(@NotNull JavaPlugin plugin, DatabaseService databaseService,
                         SchematicService schematicService, EconomyService economyService) {
        this.plugin = plugin;
        this.databaseService = databaseService;
        this.schematicService = schematicService;
        this.economyService = economyService;
        this.islandsByOwner = new ConcurrentHashMap<>();
        this.oceanWorld = plugin.getServer().createWorld(new WorldCreator(
                plugin.getConfig().getString("world.ocean-world-name", "ocean")));
        loadIslands();
    }

    private void loadIslands() {
        databaseService.getAllIslands().forEach(island -> islandsByOwner.put(island.owner(), island));
    }

    public Optional<Island> getIsland(@NotNull Player player) {
        return Optional.ofNullable(islandsByOwner.get(player.getUniqueId()));
    }

    public Optional<Island> getIslandAt(Location location) {
        return islandsByOwner.values().stream()
                .filter(island -> island.isWithinBorder(location))
                .findFirst();
    }

    public Optional<Island> getIslandByOwner(UUID owner) {
        return Optional.ofNullable(islandsByOwner.get(owner));
    }

    public void createIsland(Player player, IslandType type) {
        Location center = findNextIslandLocation();
        Island island = Island.createNew(player, type, center);
        islandsByOwner.put(player.getUniqueId(), island);
        if (schematicService.loadSchematic(type, center)) {
            generateOceanBorder(center);
            player.teleport(center);
            databaseService.saveIsland(island);
            player.sendMessage(Component.text("Island created!", NamedTextColor.GREEN));
        } else {
            islandsByOwner.remove(player.getUniqueId());
            player.sendMessage(Component.text("Failed to create island!", NamedTextColor.RED));
        }
    }

    public void deleteIsland(@NotNull Player player) {
        Island island = islandsByOwner.remove(player.getUniqueId());
        if (island != null) {
            databaseService.deleteIsland(island);
            island.members().forEach(member -> {
                Player memberPlayer = plugin.getServer().getPlayer(member);
                if (memberPlayer != null && island.isWithinBorder(memberPlayer.getLocation())) {
                    memberPlayer.teleport(memberPlayer.getWorld().getSpawnLocation());
                }
            });
            player.sendMessage(Component.text("Island deleted!", NamedTextColor.GREEN));
        }
    }

    public boolean upgradeIsland(@NotNull Player player, UpgradeType upgradeType) {
        Island island = islandsByOwner.get(player.getUniqueId());
        if (island == null) {
            return false;
        }
        int currentLevel = island.upgrades().getOrDefault(upgradeType.toString(), 0);
        if (currentLevel >= upgradeType.getMaxLevel()) {
            return false;
        }
        double cost = upgradeType.getCost(currentLevel);
        if (!economyService.hasEnough(player, cost)) {
            player.sendMessage(Component.text("You don't have enough money! Need: " + cost, NamedTextColor.RED));
            return false;
        }
        if (!economyService.withdraw(player, cost)) {
            player.sendMessage(Component.text("Transaction failed!", NamedTextColor.RED));
            return false;
        }
        int newLevel = currentLevel + upgradeType.getIncrement();
        island.upgrades().put(upgradeType.toString(), newLevel);
        databaseService.saveIsland(island);
        player.sendMessage(Component.text("Upgrade applied!", NamedTextColor.GREEN));
        return true;
    }

    private void generateOceanBorder(Location center) {
        // Placeholder for generating ocean around island
    }

    @Contract(" -> new")
    private @NotNull Location findNextIslandLocation() {
        int radius = (int) Math.sqrt(islandsByOwner.size()) * 200;
        double angle = Math.toRadians(islandsByOwner.size() * 30);
        int x = (int) (radius * Math.cos(angle));
        int z = (int) (radius * Math.sin(angle));
        return new Location(oceanWorld, x, 64, z);
    }

    public void saveAllIslands() {
        islandsByOwner.values().forEach(databaseService::saveIsland);
    }
}