package gg.kite.module;

import com.google.inject.AbstractModule;
import gg.kite.Main;
import gg.kite.PluginInitializer;
import gg.kite.service.*;
import gg.kite.storage.DatabaseService;
import gg.kite.ui.IslandGuiHandler;
import org.bukkit.plugin.java.JavaPlugin;

public class PluginModule extends AbstractModule {
    private final Main plugin;

    public PluginModule(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        bind(JavaPlugin.class).toInstance(plugin);
        bind(DatabaseService.class).asEagerSingleton();
        bind(SchematicService.class).asEagerSingleton();
        bind(EconomyService.class).asEagerSingleton();
        bind(IslandService.class).asEagerSingleton();
        bind(MinionProtectionService.class).asEagerSingleton();
        bind(TeamService.class).asEagerSingleton();
        bind(UpgradeService.class).asEagerSingleton();
        bind(IslandGuiHandler.class).asEagerSingleton();
        bind(PluginInitializer.class).asEagerSingleton();
    }
}