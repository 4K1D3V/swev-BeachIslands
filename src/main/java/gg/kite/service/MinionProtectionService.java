package gg.kite.service;

import com.google.inject.Inject;
import gg.kite.model.Island;
import gg.kite.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class MinionProtectionService implements Listener {
    private final JavaPlugin plugin;
    private final IslandService islandService;
    private final MessageUtil messageUtil;

    @Inject
    public MinionProtectionService(JavaPlugin plugin, IslandService islandService, MessageUtil messageUtil) {
        this.plugin = plugin;
        this.islandService = islandService;
        this.messageUtil = messageUtil;
    }

    @EventHandler
    public void onMinionInteract(@NotNull PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Island island = islandService.getIslandAt(event.getRightClicked().getLocation()).orElse(null);
        if (island != null && !island.owner().equals(player.getUniqueId()) &&
                !island.members().contains(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(messageUtil.getMessage("minion-protected"));
        }
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }
}