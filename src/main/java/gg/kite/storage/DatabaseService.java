package gg.kite.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import gg.kite.model.Invite;
import gg.kite.model.Island;
import gg.kite.model.IslandType;
import gg.kite.util.MessageUtil;
import jakarta.inject.Inject;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class DatabaseService {
    private final JavaPlugin plugin;
    private final MessageUtil messageUtil;
    private final HikariDataSource dataSource;

    @Inject
    public DatabaseService(@NotNull JavaPlugin plugin, MessageUtil messageUtil) {
        this.plugin = plugin;
        this.messageUtil = messageUtil;
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + new File(plugin.getDataFolder(),
                plugin.getConfig().getString("database.file", "database.db")).getAbsolutePath());
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(10);
        this.dataSource = new HikariDataSource(config);
    }

    public void initialize() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS islands (
                    owner TEXT PRIMARY KEY,
                    type TEXT NOT NULL,
                    world TEXT NOT NULL,
                    x REAL NOT NULL,
                    y REAL NOT NULL,
                    z REAL NOT NULL,
                    border_size INTEGER NOT NULL,
                    upgrades TEXT NOT NULL,
                    members TEXT NOT NULL
                )
                """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS invites (
                    inviter TEXT NOT NULL,
                    invitee TEXT PRIMARY KEY,
                    expiration INTEGER NOT NULL
                )
                """);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
        }
    }

    public void shutdown() {
        dataSource.close();
    }

    public void saveIsland(@NotNull Island island) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("""
                 INSERT OR REPLACE INTO islands (owner, type, world, x, y, z, border_size, upgrades, members)
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                 """)) {
            stmt.setString(1, island.owner().toString());
            stmt.setString(2, island.type().name());
            stmt.setString(3, island.center().getWorld().getName());
            stmt.setDouble(4, island.center().getX());
            stmt.setDouble(5, island.center().getY());
            stmt.setDouble(6, island.center().getZ());
            stmt.setInt(7, island.borderSize());
            stmt.setString(8, serializeUpgrades(island.upgrades()));
            stmt.setString(9, serializeMembers(island.members()));
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save island: " + e.getMessage());
        }
    }

    public void deleteIsland(@NotNull Island island) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM islands WHERE owner = ?")) {
            stmt.setString(1, island.owner().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to delete island: " + e.getMessage());
        }
    }

    public List<Island> getAllIslands() {
        List<Island> islands = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM islands")) {
            while (rs.next()) {
                UUID owner = UUID.fromString(rs.getString("owner"));
                IslandType type = IslandType.valueOf(rs.getString("type"));
                World world = plugin.getServer().getWorld(rs.getString("world"));
                if (world == null) {
                    plugin.getLogger().warning("World not found for island owned by " + owner);
                    continue;
                }
                Location center = new Location(world, rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"));
                int borderSize = rs.getInt("border_size");
                Map<String, Integer> upgrades = deserializeUpgrades(rs.getString("upgrades"));
                Set<UUID> members = deserializeMembers(rs.getString("members"));
                islands.add(new Island(owner, type, center, borderSize, upgrades, members));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load islands: " + e.getMessage());
        }
        return islands;
    }

    public void saveInvite(@NotNull Invite invite) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("""
                 INSERT OR REPLACE INTO invites (inviter, invitee, expiration)
                 VALUES (?, ?, ?)
                 """)) {
            stmt.setString(1, invite.inviter().toString());
            stmt.setString(2, invite.invitee().toString());
            stmt.setLong(3, invite.expiration().getEpochSecond());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save invite: " + e.getMessage());
        }
    }

    public void deleteInvite(@NotNull Invite invite) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM invites WHERE invitee = ?")) {
            stmt.setString(1, invite.invitee().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to delete invite: " + e.getMessage());
        }
    }

    public List<Invite> getAllInvites() {
        List<Invite> invites = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM invites")) {
            while (rs.next()) {
                UUID inviter = UUID.fromString(rs.getString("inviter"));
                UUID invitee = UUID.fromString(rs.getString("invitee"));
                Instant expiration = Instant.ofEpochSecond(rs.getLong("expiration"));
                invites.add(new Invite(inviter, invitee, expiration));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load invites: " + e.getMessage());
        }
        return invites;
    }

    private String serializeUpgrades(@NotNull Map<String, Integer> upgrades) {
        return upgrades.entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(Collectors.joining(";"));
    }

    private @NotNull Map<String, Integer> deserializeUpgrades(String serialized) {
        Map<String, Integer> upgrades = new HashMap<>();
        if (serialized != null && !serialized.isEmpty()) {
            for (String entry : serialized.split(";")) {
                String[] parts = entry.split(":");
                if (parts.length == 2) {
                    try {
                        upgrades.put(parts[0], Integer.parseInt(parts[1]));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return upgrades;
    }

    private String serializeMembers(@NotNull Set<UUID> members) {
        return members.stream()
                .map(UUID::toString)
                .collect(Collectors.joining(";"));
    }

    private @NotNull Set<UUID> deserializeMembers(String serialized) {
        Set<UUID> members = new HashSet<>();
        if (serialized != null && !serialized.isEmpty()) {
            for (String uuid : serialized.split(";")) {
                try {
                    members.add(UUID.fromString(uuid));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return members;
    }
}