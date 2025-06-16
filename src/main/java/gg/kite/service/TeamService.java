package gg.kite.service;

import com.google.inject.Inject;
import gg.kite.model.Invite;
import gg.kite.storage.DatabaseService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TeamService {
    private final JavaPlugin plugin;
    private final IslandService islandService;
    private final DatabaseService databaseService;
    private final ConcurrentHashMap<UUID, Invite> pendingInvites;

    @Inject
    public TeamService(JavaPlugin plugin, IslandService islandService, DatabaseService databaseService) {
        this.plugin = plugin;
        this.islandService = islandService;
        this.databaseService = databaseService;
        this.pendingInvites = new ConcurrentHashMap<>();
        loadPendingInvites();
    }

    private void loadPendingInvites() {
        databaseService.getAllInvites().forEach(invite -> pendingInvites.put(invite.invitee(), invite));
    }

    public void invitePlayer(Player owner, Player target) {
        islandService.getIsland(owner).ifPresentOrElse(island -> {
            if (island.members().size() >= 5) {
                owner.sendMessage(Component.text("Your island has reached the maximum number of members!", NamedTextColor.RED));
                return;
            }
            if (pendingInvites.containsKey(target.getUniqueId())) {
                owner.sendMessage(Component.text("This player already has a pending invite!", NamedTextColor.RED));
                return;
            }
            long expirationSeconds = plugin.getConfig().getLong("invite.expiration-seconds", 300);
            Invite invite = new Invite(owner.getUniqueId(), target.getUniqueId(),
                    Instant.now().plusSeconds(expirationSeconds));
            pendingInvites.put(target.getUniqueId(), invite);
            databaseService.saveInvite(invite);
            target.sendMessage(Component.text(owner.getName() + " has invited you to their island!\n")
                    .append(Component.text("[Accept]", NamedTextColor.GREEN)
                            .clickEvent(ClickEvent.runCommand("/island accept " + owner.getName())))
                    .append(Component.text(" [Reject]", NamedTextColor.RED)
                            .clickEvent(ClickEvent.runCommand("/island reject " + owner.getName()))));
            owner.sendMessage(Component.text("Invite sent to " + target.getName() + "!", NamedTextColor.GREEN));
        }, () -> owner.sendMessage(Component.text("You do not own an island!", NamedTextColor.RED)));
    }

    public void acceptInvite(@NotNull Player player, UUID inviter) {
        Invite invite = pendingInvites.get(player.getUniqueId());
        if (invite == null || !invite.inviter().equals(inviter) || invite.isExpired()) {
            player.sendMessage(Component.text("No valid invite found!", NamedTextColor.RED));
            return;
        }
        islandService.getIslandByOwner(inviter).ifPresentOrElse(island -> {
            island.members().add(player.getUniqueId());
            databaseService.saveIsland(island);
            pendingInvites.remove(player.getUniqueId());
            databaseService.deleteInvite(invite);
            player.sendMessage(Component.text("You have joined the island!", NamedTextColor.GREEN));
            Player owner = plugin.getServer().getPlayer(inviter);
            if (owner != null) {
                owner.sendMessage(Component.text(player.getName() + " has joined your island!", NamedTextColor.GREEN));
            }
        }, () -> {
            pendingInvites.remove(player.getUniqueId());
            databaseService.deleteInvite(invite);
            player.sendMessage(Component.text("Island no longer exists!", NamedTextColor.RED));
        });
    }

    public void rejectInvite(@NotNull Player player, UUID inviter) {
        Invite invite = pendingInvites.get(player.getUniqueId());
        if (invite == null || !invite.inviter().equals(inviter)) {
            player.sendMessage(Component.text("No valid invite found!", NamedTextColor.RED));
            return;
        }
        pendingInvites.remove(player.getUniqueId());
        databaseService.deleteInvite(invite);
        player.sendMessage(Component.text("Invite rejected!", NamedTextColor.GREEN));
        Player owner = plugin.getServer().getPlayer(inviter);
        if (owner != null) {
            owner.sendMessage(Component.text(player.getName() + " rejected your invite!", NamedTextColor.RED));
        }
    }

    public void startInviteCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                pendingInvites.entrySet().removeIf(entry -> {
                    if (entry.getValue().isExpired()) {
                        databaseService.deleteInvite(entry.getValue());
                        return true;
                    }
                    return false;
                });
            }
        }.runTaskTimer(plugin, 0L, 20L * 60); // Run every minute
    }
}