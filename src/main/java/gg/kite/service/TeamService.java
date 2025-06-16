package gg.kite.service;

import com.google.inject.Inject;
import gg.kite.model.Invite;
import gg.kite.storage.DatabaseService;
import gg.kite.util.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TeamService {
    private final JavaPlugin plugin;
    private final IslandService islandService;
    private final DatabaseService databaseService;
    private final MessageUtil messageUtil;
    private final ConcurrentHashMap<UUID, Invite> pendingInvites;

    @Inject
    public TeamService(JavaPlugin plugin, IslandService islandService, DatabaseService databaseService,
                       MessageUtil messageUtil) {
        this.plugin = plugin;
        this.islandService = islandService;
        this.databaseService = databaseService;
        this.messageUtil = messageUtil;
        this.pendingInvites = new ConcurrentHashMap<>();
        loadPendingInvites();
    }

    private void loadPendingInvites() {
        databaseService.getAllInvites().forEach(invite -> pendingInvites.put(invite.invitee(), invite));
    }

    public void invitePlayer(Player owner, Player target) {
        islandService.getIsland(owner).ifPresentOrElse(island -> {
            if (island.members().size() >= 5) {
                owner.sendMessage(messageUtil.getMessage("invite-island-full"));
                return;
            }
            if (pendingInvites.containsKey(target.getUniqueId())) {
                owner.sendMessage(messageUtil.getMessage("invite-pending"));
                return;
            }
            long expirationSeconds = plugin.getConfig().getLong("invite.expiration-seconds", 300);
            Invite invite = new Invite(owner.getUniqueId(), target.getUniqueId(),
                    Instant.now().plusSeconds(expirationSeconds));
            pendingInvites.put(target.getUniqueId(), invite);
            databaseService.saveInvite(invite);
            target.sendMessage(messageUtil.getMessage("invite-received", Map.of("player", owner.getName()))
                    .append(Component.text(" "))
                    .append(Component.text("[Accept]", NamedTextColor.GREEN)
                            .clickEvent(ClickEvent.runCommand("/island accept " + owner.getName())))
                    .append(Component.text(" "))
                    .append(Component.text("[Reject]", NamedTextColor.RED)
                            .clickEvent(ClickEvent.runCommand("/island reject " + owner.getName()))));
            owner.sendMessage(messageUtil.getMessage("invite-sent", Map.of("player", target.getName())));
        }, () -> owner.sendMessage(messageUtil.getMessage("island-not-found")));
    }

    public void acceptInvite(@NotNull Player player, UUID inviter) {
        Invite invite = pendingInvites.get(player.getUniqueId());
        if (invite == null || !invite.inviter().equals(inviter) || invite.isExpired()) {
            player.sendMessage(messageUtil.getMessage("invite-no-valid"));
            return;
        }
        islandService.getIslandByOwner(inviter).ifPresentOrElse(island -> {
            island.members().add(player.getUniqueId());
            databaseService.saveIsland(island);
            pendingInvites.remove(player.getUniqueId());
            databaseService.deleteInvite(invite);
            player.sendMessage(messageUtil.getMessage("invite-accepted"));
            Player owner = plugin.getServer().getPlayer(inviter);
            if (owner != null) {
                owner.sendMessage(messageUtil.getMessage("invite-owner-notified-accepted", Map.of("player", player.getName())));
            }
        }, () -> {
            pendingInvites.remove(player.getUniqueId());
            databaseService.deleteInvite(invite);
            player.sendMessage(messageUtil.getMessage("invite-island-gone"));
        });
    }

    public void rejectInvite(@NotNull Player player, UUID inviter) {
        Invite invite = pendingInvites.get(player.getUniqueId());
        if (invite == null || !invite.inviter().equals(inviter)) {
            player.sendMessage(messageUtil.getMessage("invite-no-valid"));
            return;
        }
        pendingInvites.remove(player.getUniqueId());
        databaseService.deleteInvite(invite);
        player.sendMessage(messageUtil.getMessage("invite-rejected"));
        Player owner = plugin.getServer().getPlayer(inviter);
        if (owner != null) {
            owner.sendMessage(messageUtil.getMessage("invite-owner-notified-rejected", Map.of("player", player.getName())));
        }
    }

    public boolean kickMember(Player owner, Player target) {
        return islandService.getIsland(owner).map(island -> {
            if (!island.members().contains(target.getUniqueId())) {
                owner.sendMessage(messageUtil.getMessage("kick-not-member", Map.of("player", target.getName())));
                return false;
            }
            island.members().remove(target.getUniqueId());
            databaseService.saveIsland(island);
            owner.sendMessage(messageUtil.getMessage("kick-success", Map.of("player", target.getName())));
            target.sendMessage(messageUtil.getMessage("kick-success", Map.of("player", owner.getName())));
            return true;
        }).orElseGet(() -> {
            owner.sendMessage(messageUtil.getMessage("island-not-found"));
            return false;
        });
    }

    public boolean leaveIsland(@NotNull Player player) {
        return islandService.getIslandAt(player.getLocation()).map(island -> {
            if (island.owner().equals(player.getUniqueId())) {
                player.sendMessage(messageUtil.getMessage("leave-owner"));
                return false;
            }
            if (!island.members().contains(player.getUniqueId())) {
                player.sendMessage(messageUtil.getMessage("leave-not-member"));
                return false;
            }
            island.members().remove(player.getUniqueId());
            databaseService.saveIsland(island);
            player.sendMessage(messageUtil.getMessage("leave-success"));
            Player owner = plugin.getServer().getPlayer(island.owner());
            if (owner != null) {
                owner.sendMessage(messageUtil.getMessage("leave-success", Map.of("player", player.getName())));
            }
            return true;
        }).orElseGet(() -> {
            player.sendMessage(messageUtil.getMessage("leave-not-member"));
            return false;
        });
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