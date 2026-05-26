package me.ray.midgard.core.economy;

import java.util.UUID;

public interface EconomyProvider {
    
    double getBalance(UUID player, String currency);
    
    void setBalance(UUID player, String currency, double amount);
    
    void deposit(UUID player, String currency, double amount);
    
    void withdraw(UUID player, String currency, double amount);
    
    boolean has(UUID player, String currency, double amount);
    
    String format(String currency, double amount);
}
