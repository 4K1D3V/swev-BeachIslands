package gg.kite.service;

import com.google.inject.Inject;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.session.ClipboardHolder;
import gg.kite.model.IslandType;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;

public class SchematicService {
    private final JavaPlugin plugin;
    private final File schematicsFolder;

    @Inject
    public SchematicService(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        this.schematicsFolder = new File(plugin.getDataFolder(), plugin.getConfig().getString("schematics.directory", "schematics"));
    }

    public void initialize() {
        if (!schematicsFolder.exists()) {
            schematicsFolder.mkdirs();
        }
    }

    public boolean loadSchematic(@NotNull IslandType type, Location location) {
        File schematicFile = new File(schematicsFolder, type.getSchematicFile());
        if (!schematicFile.exists()) {
            plugin.getLogger().warning("Schematic file not found: " + schematicFile.getPath());
            return false;
        }

        try (FileInputStream fis = new FileInputStream(schematicFile)) {
            Clipboard clipboard = ClipboardFormats.findByFile(schematicFile)
                    .getReader(fis)
                    .read();
            ClipboardHolder holder = new ClipboardHolder(clipboard);
            Operation operation = holder.createPaste(WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(location.getWorld())))
                    .to(BukkitAdapter.asBlockVector(location))
                    .ignoreAirBlocks(true)
                    .build();
            Operations.complete(operation);
            return true;
        } catch (IOException | com.sk89q.worldedit.WorldEditException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load schematic: " + type.getSchematicFile(), e);
            return false;
        }
    }
}