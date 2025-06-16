package gg.kite.service;

import com.google.inject.Inject;
import gg.kite.util.MessageUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class EconomyService {
    private final JavaPlugin plugin;
    private final MessageUtil messageUtil;
    private Economy economy;

    @Inject
    public EconomyService(JavaPlugin plugin, MessageUtil messageUtil) {
        this.plugin = plugin;
        this.messageUtil = messageUtil;
    }

    public void initialize() {
        if (!plugin.getConfig().getBoolean("economy.enabled", true)) {
            plugin.getLogger().info("Economy integration disabled in config.");
            return;
        }
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            economy = rsp.getProvider();
            plugin.getLogger().info("Vault economy hooked successfully.");
        } else {
            plugin.getLogger().warning("No Vault economy provider found!");
        }
    }

    public boolean hasEnough(Player player, double amount) {
        if (economy == null) return false;
        return economy.has(player, amount);
    }

    public boolean withdraw(Player player, double amount) {
        if (economy == null) {
            player.sendMessage(messageUtil.getMessage("transaction-failed"));
            return false;
        }
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }
}