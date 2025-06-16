package gg.kite.service;

import com.google.inject.Inject;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import gg.kite.model.Island;
import gg.kite.model.IslandType;
import gg.kite.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;

public class SchematicService {
    private final JavaPlugin plugin;
    private final File schematicsFolder;
    private final MessageUtil messageUtil;

    @Inject
    public SchematicService(@NotNull JavaPlugin plugin, MessageUtil messageUtil) {
        this.plugin = plugin;
        this.messageUtil = messageUtil;
        this.schematicsFolder = new File(plugin.getDataFolder(), plugin.getConfig().getString("schematics.directory", "schematics"));
    }

    public void initialize() {
        if (!schematicsFolder.exists()) {
            if (!schematicsFolder.mkdirs()) {
                plugin.getLogger().warning("Failed to create schematics directory: " + schematicsFolder.getPath());
            }
        }
    }

    public boolean loadSchematic(IslandType type, @NotNull Location location) {
        if (location.getWorld() == null) {
            plugin.getLogger().severe("Cannot load schematic: World is null for location " + location);
            return false;
        }
        File schematicFile = new File(schematicsFolder, type.getSchematicFile());
        if (!schematicFile.exists()) {
            plugin.getLogger().warning("Schematic file not found: " + schematicFile.getPath());
            return false;
        }

        try (FileInputStream fis = new FileInputStream(schematicFile)) {
            Clipboard clipboard = Objects.requireNonNull(ClipboardFormats.findByFile(schematicFile))
                    .getReader(fis)
                    .read();
            ClipboardHolder holder = new ClipboardHolder(clipboard);
            Operation operation = holder.createPaste(WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(location.getWorld())))
                    .to(BukkitAdapter.asBlockVector(location))
                    .ignoreAirBlocks(true)
                    .build();
            Operations.complete(operation);
            return true;
        } catch (IOException | WorldEditException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load schematic: " + type.getSchematicFile(), e);
            return false;
        }
    }

    public boolean clearIsland(@NotNull Island island) {
        if (island.center().getWorld() == null) {
            plugin.getLogger().severe("Cannot clear island: World is null for island owned by " + island.owner());
            return false;
        }
        try {
            BlockVector3 min = BukkitAdapter.asBlockVector(island.center()).subtract(island.borderSize() / 2, 64, island.borderSize() / 2);
            BlockVector3 max = min.add(island.borderSize(), 256, island.borderSize());
            CuboidRegion region = new CuboidRegion(BukkitAdapter.adapt(island.center().getWorld()), min, max);
            try (com.sk89q.worldedit.EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(island.center().getWorld()))) {
                editSession.setBlocks(region, (Pattern) BlockTypes.AIR);
            }
            return true;
        } catch (WorldEditException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to clear island for owner: " + island.owner(), e);
            return false;
        }
    }

    public MessageUtil getMessageUtil() {
        return messageUtil;
    }
}