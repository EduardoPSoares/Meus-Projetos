package me.ray.midgard.core.integration;

import me.ray.midgard.core.economy.EconomyProvider;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.UUID;
import me.ray.midgard.core.utils.ConsoleUtils;

public class VaultIntegration implements EconomyProvider {

    private Economy economy;

    public VaultIntegration() {
        setupEconomy();
    }

    private void setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            ConsoleUtils.log("    <gray>Economy:<red>Vault Not Found</red>");
            return;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            ConsoleUtils.log("    <gray>Economy:<red>Provider Missing</red>");
            return;
        }
        economy = rsp.getProvider();
        ConsoleUtils.log("    <gray>Economy:<white>Vault Hooked</white>");
    }

    @Override
    public double getBalance(UUID player, String currency) {
        if (economy == null) { return 0.0; }
        // Vault only supports one currency usually, so we ignore currency param or check if it matches default
        return economy.getBalance(Bukkit.getOfflinePlayer(player));
    }

    @Override
    public void setBalance(UUID player, String currency, double amount) {
        if (economy == null) { return; }
        double current = getBalance(player, currency);
        if (current < amount) {
            deposit(player, currency, amount - current);
        } else {
            withdraw(player, currency, current - amount);
        }
    }

    @Override
    public void deposit(UUID player, String currency, double amount) {
        if (economy == null) { return; }
        economy.depositPlayer(Bukkit.getOfflinePlayer(player), amount);
    }

    @Override
    public void withdraw(UUID player, String currency, double amount) {
        if (economy == null) { return; }
        economy.withdrawPlayer(Bukkit.getOfflinePlayer(player), amount);
    }

    @Override
    public boolean has(UUID player, String currency, double amount) {
        if (economy == null) { return false; }
        return economy.has(Bukkit.getOfflinePlayer(player), amount);
    }

    @Override
    public String format(String currency, double amount) {
        return economy.format(amount);
    }
}
