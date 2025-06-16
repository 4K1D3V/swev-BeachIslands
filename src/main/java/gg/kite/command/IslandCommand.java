package gg.kite.command;

import gg.kite.model.Island;
import gg.kite.model.IslandType;
import gg.kite.model.UpgradeType;
import gg.kite.service.IslandService;
import gg.kite.service.TeamService;
import gg.kite.ui.IslandGuiHandler;
import gg.kite.util.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class IslandCommand implements CommandExecutor {
    private final IslandService islandService;
    private final IslandGuiHandler guiHandler;
    private final TeamService teamService;
    private final MessageUtil messageUtil;

    public IslandCommand(IslandService islandService, IslandGuiHandler guiHandler,
                         TeamService teamService, MessageUtil messageUtil) {
        this.islandService = islandService;
        this.guiHandler = guiHandler;
        this.teamService = teamService;
        this.messageUtil = messageUtil;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageUtil.getMessage("player-only"));
            return true;
        }

        if (!sender.hasPermission("beachislands.player")) {
            sender.sendMessage(messageUtil.getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(messageUtil.getMessage("invalid-subcommand"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                if (args.length == 2 && sender.hasPermission("beachislands.admin")) {
                    try {
                        IslandType type = IslandType.valueOf(args[1].toUpperCase());
                        if (islandService.createIsland(player, type)) {
                            player.sendMessage(messageUtil.getMessage("island-created"));
                        }
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(messageUtil.getMessage("invalid-island-type"));
                    }
                } else {
                    islandService.getIsland(player).ifPresentOrElse(
                            island -> player.sendMessage(messageUtil.getMessage("island-exists")),
                            () -> guiHandler.openCreateGui(player));
                }
                break;
            case "delete":
                islandService.getIsland(player).ifPresentOrElse(
                        island -> guiHandler.openDeleteConfirmGui(player),
                        () -> player.sendMessage(messageUtil.getMessage("island-not-found")));
                break;
            case "confirm_delete":
                islandService.getIsland(player).ifPresentOrElse(
                        island -> {
                            if (islandService.deleteIsland(player)) {
                                player.sendMessage(messageUtil.getMessage("island-deleted"));
                            }
                        },
                        () -> player.sendMessage(messageUtil.getMessage("no-island-to-delete")));
                break;
            case "upgrade":
                islandService.getIsland(player).ifPresentOrElse(
                        island -> {
                            if (args.length == 2) {
                                try {
                                    UpgradeType type = UpgradeType.valueOf(args[1].toUpperCase());
                                    if (islandService.upgradeIsland(player, type)) {
                                        player.sendMessage(messageUtil.getMessage("upgrade-applied"));
                                    } else {
                                        player.sendMessage(messageUtil.getMessage("upgrade-failed"));
                                    }
                                } catch (IllegalArgumentException e) {
                                    player.sendMessage(messageUtil.getMessage("invalid-upgrade-type"));
                                }
                            } else {
                                guiHandler.openUpgradeGui(player);
                            }
                        },
                        () -> player.sendMessage(messageUtil.getMessage("island-not-found")));
                break;
            case "invite":
                if (args.length == 2) {
                    Player target = player.getServer().getPlayer(args[1]);
                    if (target != null) {
                        teamService.invitePlayer(player, target);
                    } else {
                        player.sendMessage(messageUtil.getMessage("player-not-found"));
                    }
                } else {
                    player.sendMessage(messageUtil.getMessage("invalid-usage-invite"));
                }
                break;
            case "accept":
                if (args.length == 2) {
                    Player inviter = player.getServer().getPlayer(args[1]);
                    if (inviter != null) {
                        teamService.acceptInvite(player, inviter.getUniqueId());
                    } else {
                        player.sendMessage(messageUtil.getMessage("player-not-found"));
                    }
                } else {
                    player.sendMessage(messageUtil.getMessage("invalid-usage-accept"));
                }
                break;
            case "reject":
                if (args.length == 2) {
                    Player inviter = player.getServer().getPlayer(args[1]);
                    if (inviter != null) {
                        teamService.rejectInvite(player, inviter.getUniqueId());
                    } else {
                        player.sendMessage(messageUtil.getMessage("player-not-found"));
                    }
                } else {
                    player.sendMessage(messageUtil.getMessage("invalid-usage-reject"));
                }
                break;
            case "home":
                islandService.getIsland(player).ifPresentOrElse(
                        island -> {
                            player.teleport(island.center());
                            player.sendMessage(messageUtil.getMessage("home-teleported"));
                        },
                        () -> player.sendMessage(messageUtil.getMessage("island-not-found")));
                break;
            case "kick":
                if (args.length == 2) {
                    Player target = player.getServer().getPlayer(args[1]);
                    if (target != null) {
                        islandService.getIsland(player).ifPresentOrElse(
                                island -> {
                                    if (!island.owner().equals(player.getUniqueId())) {
                                        player.sendMessage(messageUtil.getMessage("kick-not-owner"));
                                    } else {
                                        teamService.kickMember(player, target);
                                    }
                                },
                                () -> player.sendMessage(messageUtil.getMessage("island-not-found")));
                    } else {
                        player.sendMessage(messageUtil.getMessage("player-not-found"));
                    }
                } else {
                    player.sendMessage(messageUtil.getMessage("invalid-usage-kick"));
                }
                break;
            case "leave":
                teamService.leaveIsland(player);
                break;
            case "info":
                islandService.getIsland(player).ifPresentOrElse(
                        island -> displayIslandInfo(player, island),
                        () -> player.sendMessage(messageUtil.getMessage("island-not-found")));
                break;
            case "admin":
                if (!sender.hasPermission("beachislands.admin")) {
                    sender.sendMessage(messageUtil.getMessage("no-permission"));
                    return true;
                }
                if (args.length < 3) {
                    player.sendMessage(Component.text("Use: /island admin <delete|tp|reset> <player>", NamedTextColor.RED));
                    return true;
                }
                Player target = player.getServer().getPlayer(args[2]);
                if (target == null) {
                    player.sendMessage(messageUtil.getMessage("player-not-found"));
                    return true;
                }
                switch (args[1].toLowerCase()) {
                    case "delete":
                        islandService.getIslandByOwner(target.getUniqueId()).ifPresentOrElse(
                                island -> {
                                    if (islandService.deleteIsland(target)) {
                                        player.sendMessage(messageUtil.getMessage("admin-deleted", Map.of("player", target.getName())));
                                    }
                                },
                                () -> player.sendMessage(messageUtil.getMessage("admin-no-island", Map.of("player", target.getName()))));
                        break;
                    case "tp":
                        islandService.getIslandByOwner(target.getUniqueId()).ifPresentOrElse(
                                island -> {
                                    player.teleport(island.center());
                                    player.sendMessage(messageUtil.getMessage("admin-tp", Map.of("player", target.getName())));
                                },
                                () -> player.sendMessage(messageUtil.getMessage("admin-no-island", Map.of("player", target.getName()))));
                        break;
                    case "reset":
                        islandService.getIslandByOwner(target.getUniqueId()).ifPresentOrElse(
                                island -> {
                                    try {
                                        IslandType type = IslandType.valueOf(island.type().name());
                                        if (islandService.resetIsland(target, type)) {
                                            player.sendMessage(messageUtil.getMessage("admin-reset", Map.of("player", target.getName())));
                                        }
                                    } catch (IllegalArgumentException e) {
                                        player.sendMessage(messageUtil.getMessage("invalid-island-type"));
                                    }
                                },
                                () -> player.sendMessage(messageUtil.getMessage("admin-no-island", Map.of("player", target.getName()))));
                        break;
                    default:
                        player.sendMessage(Component.text("Invalid admin subcommand!", NamedTextColor.RED));
                }
                break;
            default:
                player.sendMessage(messageUtil.getMessage("invalid-subcommand"));
        }

        return true;
    }

    private void displayIslandInfo(@NotNull Player player, @NotNull Island island) {
        player.sendMessage(messageUtil.getMessage("info-header"));
        player.sendMessage(messageUtil.getMessage("info-owner", Map.of("owner", Objects.requireNonNull(player.getServer().getOfflinePlayer(island.owner()).getName()))));
        player.sendMessage(messageUtil.getMessage("info-type", Map.of("type", island.type().getDisplayName())));
        String members = island.members().stream()
                .map(uuid -> player.getServer().getOfflinePlayer(uuid).getName())
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));
        player.sendMessage(messageUtil.getMessage("info-members", Map.of("members", members.isEmpty() ? "None" : members)));
        String upgrades = island.upgrades().entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining(", "));
        player.sendMessage(messageUtil.getMessage("info-upgrades", Map.of("upgrades", upgrades.isEmpty() ? "None" : upgrades)));
    }
}