package gg.kite.service;

import gg.kite.model.Island;
import gg.kite.model.UpgradeType;
import gg.kite.util.MessageUtil;
import jakarta.inject.Inject;
import me.waterarchery.litminions.api.LitMinionsAPI;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class UpgradeService implements Listener {
    private final JavaPlugin plugin;
    private final IslandService islandService;
    private final MessageUtil messageUtil;
    private final Random random;

    @Inject
    public UpgradeService(JavaPlugin plugin, IslandService islandService, MessageUtil messageUtil) {
        this.plugin = plugin;
        this.islandService = islandService;
        this.messageUtil = messageUtil;
        this.random = new Random();
    }

    @EventHandler
    public void onBlockPlace(@NotNull BlockPlaceEvent event) {
        restrictBlockAction(event.getPlayer(), event.getBlock(), event);
    }

    @EventHandler
    public void onBlockBreak(@NotNull BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (restrictBlockAction(player, event.getBlock(), event)) {
            Island island = islandService.getIslandAt(event.getBlock().getLocation()).orElse(null);
            if (island != null && isOre(event.getBlock().getType())) {
                int oreLevel = island.upgrades().getOrDefault(UpgradeType.ORE_BOOSTER.toString(), 0);
                double multiplier = plugin.getConfig().getDouble("upgrades.ore-booster-multiplier", 1.5);
                if (oreLevel > 0 && random.nextDouble() < (multiplier - 1) * oreLevel) {
                    ItemStack drop = new ItemStack(event.getBlock().getType());
                    event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), drop);
                }
            }
        }
    }

    @EventHandler
    public void onBlockGrow(@NotNull BlockGrowEvent event) {
        Island island = islandService.getIslandAt(event.getBlock().getLocation()).orElse(null);
        if (island != null) {
            int cropLevel = island.upgrades().getOrDefault(UpgradeType.CROPS_BOOSTER.toString(), 0);
            double multiplier = plugin.getConfig().getDouble("upgrades.crops-booster-multiplier", 1.5);
            if (cropLevel > 0 && random.nextDouble() < (multiplier - 1) * cropLevel) {
                event.getBlock().applyBoneMeal(BlockFace.UP);
            }
        }
    }

    public int getMaxMinions(Player player) {
        return islandService.getIsland(player).map(island -> {
            int minionLevel = island.upgrades().getOrDefault(UpgradeType.MINION_LIMIT.toString(), 0);
            return LitMinionsAPI.getMaxPlaceableMinion(player) + minionLevel * UpgradeType.MINION_LIMIT.getIncrement();
        }).orElse(LitMinionsAPI.getMaxPlaceableMinion(player));
    }

    @Contract(pure = true)
    private boolean isOre(@NotNull Material material) {
        return switch (material) {
            case COAL_ORE, IRON_ORE, GOLD_ORE, DIAMOND_ORE, EMERALD_ORE, LAPIS_ORE, REDSTONE_ORE -> true;
            default -> false;
        };
    }

    private boolean restrictBlockAction(@NotNull Player player, @NotNull Block block, @NotNull BlockPlaceEvent event) {
        Island island = islandService.getIslandAt(block.getLocation()).orElse(null);
        if (island == null || (!island.owner().equals(player.getUniqueId()) && !island.members().contains(player.getUniqueId()))) {
            if (island != null) {
                int borderLevel = island.upgrades().getOrDefault(UpgradeType.BORDER.toString(), 0);
                int borderSize = 75 + borderLevel * UpgradeType.BORDER.getIncrement();
                if (!island.isWithinBorder(block.getLocation())) {
                    event.setCancelled(true);
                    player.sendMessage(messageUtil.getMessage("border-restricted"));
                    return false;
                }
            }
            return true;
        }
        return true;
    }

    private boolean restrictBlockAction(@NotNull Player player, @NotNull Block block, @NotNull BlockBreakEvent event) {
        Island island = islandService.getIslandAt(block.getLocation()).orElse(null);
        if (island == null || (!island.owner().equals(player.getUniqueId()) && !island.members().contains(player.getUniqueId()))) {
            if (island != null) {
                int borderLevel = island.upgrades().getOrDefault(UpgradeType.BORDER.toString(), 0);
                int borderSize = 75 + borderLevel * UpgradeType.BORDER.getIncrement();
                if (!island.isWithinBorder(block.getLocation())) {
                    event.setCancelled(true);
                    player.sendMessage(messageUtil.getMessage("border-restricted"));
                    return false;
                }
            }
            return true;
        }
        return true;
    }
}