package gg.kite.command;

import gg.kite.model.IslandType;
import gg.kite.model.UpgradeType;
import gg.kite.service.IslandService;
import gg.kite.service.TeamService;
import gg.kite.ui.IslandGuiHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class IslandCommand implements CommandExecutor {
    private final IslandService islandService;
    private final IslandGuiHandler guiHandler;
    private final TeamService teamService;

    public IslandCommand(IslandService islandService, IslandGuiHandler guiHandler,
                         TeamService teamService) {
        this.islandService = islandService;
        this.guiHandler = guiHandler;
        this.teamService = teamService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command is for players only!", NamedTextColor.RED));
            return true;
        }

        if (!sender.hasPermission("beachislands.player")) {
            sender.sendMessage(Component.text("You do not have permission!", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Component.text("Use: /island <create|delete|upgrade|invite|accept|reject>", NamedTextColor.BLUE));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                if (args.length == 2 && sender.hasPermission("beachislands.admin")) {
                    try {
                        IslandType type = IslandType.valueOf(args[1].toUpperCase());
                        islandService.createIsland(player, type);
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(Component.text("Invalid island type!", NamedTextColor.RED));
                    }
                } else {
                    islandService.getIsland(player).ifPresentOrElse(
                            island -> player.sendMessage(Component.text("You already own an island!", NamedTextColor.RED)),
                            () -> guiHandler.openCreateGui(player));
                }
                break;
            case "delete":
                islandService.getIsland(player).ifPresentOrElse(
                        island -> guiHandler.openDeleteConfirmGui(player),
                        () -> player.sendMessage(Component.text("You do not own an island!", NamedTextColor.RED)));
                break;
            case "confirm_delete":
                islandService.getIsland(player).ifPresentOrElse(
                        island -> islandService.deleteIsland(player),
                        () -> player.sendMessage(Component.text("No island to delete!", NamedTextColor.RED)));
                break;
            case "upgrade":
                islandService.getIsland(player).ifPresentOrElse(
                        island -> {
                            if (args.length == 2) {
                                try {
                                    UpgradeType type = UpgradeType.valueOf(args[1].toUpperCase());
                                    if (islandService.upgradeIsland(player, type)) {
                                        player.sendMessage(Component.text("Upgrade applied!", NamedTextColor.GREEN));
                                    } else {
                                        player.sendMessage(Component.text("Cannot apply upgrade!", NamedTextColor.RED));
                                    }
                                } catch (IllegalArgumentException e) {
                                    player.sendMessage(Component.text("Invalid upgrade type!", NamedTextColor.RED));
                                }
                            } else {
                                guiHandler.openUpgradeGui(player);
                            }
                        },
                        () -> player.sendMessage(Component.text("You do not own an island!", NamedTextColor.RED)));
                break;
            case "invite":
                if (args.length == 2) {
                    Player target = player.getServer().getPlayer(args[1]);
                    if (target != null) {
                        teamService.invitePlayer(player, target);
                    } else {
                        player.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
                    }
                } else {
                    player.sendMessage(Component.text("Use: /island invite <player>", NamedTextColor.BLUE));
                }
                break;
            case "accept":
                if (args.length == 2) {
                    Player inviter = player.getServer().getPlayer(args[1]);
                    if (inviter != null) {
                        teamService.acceptInvite(player, inviter.getUniqueId());
                    } else {
                        player.sendMessage(Component.text("Inviter not found!", NamedTextColor.RED));
                    }
                } else {
                    player.sendMessage(Component.text("Use: /island accept <player>", NamedTextColor.BLUE));
                }
                break;
            case "reject":
                if (args.length == 2) {
                    Player inviter = player.getServer().getPlayer(args[1]);
                    if (inviter != null) {
                        teamService.rejectInvite(player, inviter.getUniqueId());
                    } else {
                        player.sendMessage(Component.text("Inviter not found!", NamedTextColor.RED));
                    }
                } else {
                    player.sendMessage(Component.text("Use: /island reject <player>", NamedTextColor.BLUE));
                }
                break;
            default:
                player.sendMessage(Component.text("Invalid subcommand!", NamedTextColor.RED));
        }

        return true;
    }
}