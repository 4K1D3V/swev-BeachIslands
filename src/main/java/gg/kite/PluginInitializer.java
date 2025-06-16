package gg.kite;

import gg.kite.command.IslandCommand;
import gg.kite.service.*;
import gg.kite.storage.DatabaseService;
import gg.kite.ui.IslandGuiHandler;
import gg.kite.util.MessageUtil;
import jakarta.inject.Inject;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class PluginInitializer {
    private final JavaPlugin plugin;
    private final DatabaseService databaseService;
    private final SchematicService schematicService;
    private final EconomyService economyService;
    private final IslandService islandService;
    private final MinionProtectionService minionProtectionService;
    private final TeamService teamService;
    private final UpgradeService upgradeService;
    private final IslandGuiHandler guiHandler;
    private final MessageUtil messageUtil;

    @Inject
    public PluginInitializer(JavaPlugin plugin, DatabaseService databaseService,
                             SchematicService schematicService, EconomyService economyService,
                             IslandService islandService, MinionProtectionService minionProtectionService,
                             TeamService teamService, UpgradeService upgradeService,
                             IslandGuiHandler guiHandler) {
        this.plugin = plugin;
        this.databaseService = databaseService;
        this.schematicService = schematicService;
        this.economyService = economyService;
        this.islandService = islandService;
        this.minionProtectionService = minionProtectionService;
        this.teamService = teamService;
        this.upgradeService = upgradeService;
        this.guiHandler = guiHandler;
        this.messageUtil = new MessageUtil(plugin);
    }

    public void initialize() {
        databaseService.initialize();
        schematicService.initialize();
        economyService.initialize();
        PluginCommand islandCommand = plugin.getCommand("island");
        if (islandCommand != null) {
            islandCommand.setExecutor(new IslandCommand(islandService, guiHandler, teamService, messageUtil));
        }
        plugin.getServer().getPluginManager().registerEvents(guiHandler, plugin);
        plugin.getServer().getPluginManager().registerEvents(minionProtectionService, plugin);
        plugin.getServer().getPluginManager().registerEvents(upgradeService, plugin);
        teamService.startInviteCleanupTask();
    }

    public void shutdown() {
        islandService.saveAllIslands();
        databaseService.shutdown();
    }
}