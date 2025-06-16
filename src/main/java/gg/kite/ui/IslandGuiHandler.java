package gg.kite.ui;

import gg.kite.model.IslandType;
import gg.kite.model.UpgradeType;
import gg.kite.service.IslandService;
import jakarta.inject.Inject;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.jetbrains.annotations.NotNull;

public class IslandGuiHandler implements Listener {
    private final IslandService islandService;

    @Inject
    public IslandGuiHandler(IslandService islandService) {
        this.islandService = islandService;
    }

    public void openCreateGui(Player player) {
        Book.Builder book = Book.builder();
        book.title(Component.text("Choose Island Type"));
        Component page = Component.text("Select an island type:\n\n");
        for (IslandType type : IslandType.values()) {
            page = page.append(Component.text(type.getDisplayName(), NamedTextColor.GREEN)
                            .clickEvent(ClickEvent.runCommand("/island create " + type.name().toLowerCase())))
                    .append(Component.text("\n"));
        }
        book.addPage(page);
        player.openBook(book.build());
    }

    public void openDeleteConfirmGui(@NotNull Player player) {
        Book book = Book.builder()
                .title(Component.text("Confirm Deletion"))
                .addPage(Component.text("Are you sure you want to delete your island?\n\n")
                        .append(Component.text("[Yes]", NamedTextColor.RED)
                                .clickEvent(ClickEvent.runCommand("/island confirm_delete")))
                        .append(Component.text(" [No]", NamedTextColor.GREEN)
                                .clickEvent(ClickEvent.runCommand("/island close"))))
                .build();
        player.openBook(book);
    }

    public void openUpgradeGui(Player player) {
        Book.Builder book = Book.builder();
        book.title(Component.text("Island Upgrades"));
        Component page = Component.text("Available upgrades:\n\n");
        for (UpgradeType upgrade : UpgradeType.values()) {
            page = page.append(Component.text(upgrade.getDisplayName(), NamedTextColor.BLUE)
                            .clickEvent(ClickEvent.runCommand("/island upgrade " + upgrade.name().toLowerCase())))
                    .append(Component.text("\n"));
        }
        book.addPage(page);
        player.openBook(book.build());
    }

    @EventHandler
    public void onCommand(@NotNull PlayerCommandPreprocessEvent event) {
        if (event.getMessage().equalsIgnoreCase("/island close")) {
            event.getPlayer().closeInventory();
            event.setCancelled(true);
        }
    }
}