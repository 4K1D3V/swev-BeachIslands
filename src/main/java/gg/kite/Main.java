package gg.kite;

import com.google.inject.Guice;
import com.google.inject.Injector;
import gg.kite.module.PluginModule;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    private Injector injector;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        injector = Guice.createInjector(new PluginModule(this));
        injector.getInstance(PluginInitializer.class).initialize();
    }

    @Override
    public void onDisable() {
        injector.getInstance(PluginInitializer.class).shutdown();
    }
}