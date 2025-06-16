package gg.kite;

import gg.kite.command.IslandCommand;
import gg.kite.service.EconomyService;
import gg.kite.service.IslandService;
import gg.kite.service.MinionProtectionService;
import gg.kite.service.SchematicService;
import gg.kite.service.TeamService;
import gg.kite.storage.DatabaseService;
import gg.kite.ui.IslandGuiHandler;
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
    private final IslandGuiHandler guiHandler;

    @Inject
    public PluginInitializer(JavaPlugin plugin, DatabaseService databaseService,
                             SchematicService schematicService, EconomyService economyService,
                             IslandService islandService, MinionProtectionService minionProtectionService,
                             TeamService teamService, IslandGuiHandler guiHandler) {
        this.plugin = plugin;
        this.databaseService = databaseService;
        this.schematicService = schematicService;
        this.economyService = economyService;
        this.islandService = islandService;
        this.minionProtectionService = minionProtectionService;
        this.teamService = teamService;
        this.guiHandler = guiHandler;
    }

    public void initialize() {
        databaseService.initialize();
        schematicService.initialize();
        economyService.initialize();
        PluginCommand islandCommand = plugin.getCommand("island");
        if (islandCommand != null) {
            islandCommand.setExecutor(new IslandCommand(islandService, guiHandler, teamService));
        }
        plugin.getServer().getPluginManager().registerEvents(guiHandler, plugin);
        plugin.getServer().getPluginManager().registerEvents(minionProtectionService, plugin);
        teamService.startInviteCleanupTask();
    }

    public void shutdown() {
        islandService.saveAllIslands();
        databaseService.shutdown();
    }
}