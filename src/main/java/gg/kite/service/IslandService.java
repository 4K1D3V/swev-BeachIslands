package gg.kite.service;

import com.google.inject.Inject;
import gg.kite.model.Island;
import gg.kite.model.IslandType;
import gg.kite.model.UpgradeType;
import gg.kite.storage.DatabaseService;
import gg.kite.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
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
    private final MessageUtil messageUtil;
    private final Map<UUID, Island> islandsByOwner;
    private final World oceanWorld;

    @Inject
    public IslandService(@NotNull JavaPlugin plugin, DatabaseService databaseService,
                         SchematicService schematicService, EconomyService economyService,
                         MessageUtil messageUtil) {
        this.plugin = plugin;
        this.databaseService = databaseService;
        this.schematicService = schematicService;
        this.economyService = economyService;
        this.messageUtil = messageUtil;
        this.islandsByOwner = new ConcurrentHashMap<>();
        World world = plugin.getServer().getWorld(plugin.getConfig().getString("world.ocean-world-name", "ocean"));
        if (world == null) {
            world = plugin.getServer().createWorld(new WorldCreator(plugin.getConfig().getString("world.ocean-world-name", "ocean")));
        }
        this.oceanWorld = world;
        if (oceanWorld == null) {
            plugin.getLogger().severe("Failed to load or create ocean world!");
        }
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

    public boolean createIsland(Player player, IslandType type) {
        if (oceanWorld == null) {
            player.sendMessage(messageUtil.getMessage("error-world"));
            return false;
        }
        Location center = findNextIslandLocation();
        Island island = Island.createNew(player, type, center);
        islandsByOwner.put(player.getUniqueId(), island);
        if (schematicService.loadSchematic(type, center)) {
            generateOceanBorder(center);
            player.teleport(center);
            databaseService.saveIsland(island);
            player.sendMessage(messageUtil.getMessage("island-created"));
            return true;
        } else {
            islandsByOwner.remove(player.getUniqueId());
            player.sendMessage(messageUtil.getMessage("error-schematic", Map.of("file", type.getSchematicFile())));
            return false;
        }
    }

    public boolean deleteIsland(@NotNull Player player) {
        Island island = islandsByOwner.remove(player.getUniqueId());
        if (island != null) {
            if (!schematicService.clearIsland(island)) {
                plugin.getLogger().warning("Failed to clear island for owner: " + player.getUniqueId());
            }
            databaseService.deleteIsland(island);
            island.members().forEach(member -> {
                Player memberPlayer = plugin.getServer().getPlayer(member);
                if (memberPlayer != null && island.isWithinBorder(memberPlayer.getLocation())) {
                    memberPlayer.teleport(memberPlayer.getWorld().getSpawnLocation());
                }
            });
            player.sendMessage(messageUtil.getMessage("island-deleted"));
            return true;
        }
        return false;
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
            player.sendMessage(messageUtil.getMessage("not-enough-money", Map.of("cost", String.valueOf(cost))));
            return false;
        }
        if (!economyService.withdraw(player, cost)) {
            player.sendMessage(messageUtil.getMessage("transaction-failed"));
            return false;
        }
        int newLevel = currentLevel + upgradeType.getIncrement();
        island.upgrades().put(upgradeType.toString(), newLevel);
        databaseService.saveIsland(island);
        player.sendMessage(messageUtil.getMessage("upgrade-applied"));
        return true;
    }

    public boolean resetIsland(@NotNull Player player, IslandType type) {
        Island island = islandsByOwner.get(player.getUniqueId());
        if (island == null) {
            return false;
        }
        if (!schematicService.clearIsland(island)) {
            plugin.getLogger().warning("Failed to clear island for reset: " + player.getUniqueId());
            return false;
        }
        if (!schematicService.loadSchematic(type, island.center())) {
            player.sendMessage(messageUtil.getMessage("error-schematic", Map.of("file", type.getSchematicFile())));
            return false;
        }
        generateOceanBorder(island.center());
        island.upgrades().clear();
        island.members().clear();
        databaseService.saveIsland(island);
        player.teleport(island.center());
        return true;
    }

    private void generateOceanBorder(@NotNull Location center) {
        int borderWidth = plugin.getConfig().getInt("world.border-width", 10);
        int radius = 75 / 2 + borderWidth; // Default border size is 75
        World world = center.getWorld();
        if (world == null) return;
        int centerX = center.getBlockX();
        int centerZ = center.getBlockZ();
        int y = 64; // Sea level
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                double distance = Math.sqrt(Math.pow(x - centerX, 2) + Math.pow(z - centerZ, 2));
                if (distance > 75 / 2.0 && distance <= radius) {
                    Block block = world.getBlockAt(x, y, z);
                    block.setType(Material.WATER);
                    for (int below = y - 1; below >= y - 5; below--) {
                        world.getBlockAt(x, below, z).setType(Material.WATER);
                    }
                }
            }
        }
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