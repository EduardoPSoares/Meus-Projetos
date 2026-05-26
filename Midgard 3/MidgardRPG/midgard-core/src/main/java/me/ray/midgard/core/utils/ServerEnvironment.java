package me.ray.midgard.core.utils;

import org.bukkit.Bukkit;

public class ServerEnvironment {

    private static final boolean IS_FOLIA;
    private static final boolean IS_PAPER;
    private static final String SERVER_VERSION;

    static {
        // Detect Folia
        boolean folia = false;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException e) {
            folia = false;
        }
        IS_FOLIA = folia;

        // Detect Paper
        boolean paper = false;
        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            paper = true;
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("io.papermc.paper.configuration.GlobalConfiguration");
                paper = true;
            } catch (ClassNotFoundException ex) {
                paper = false;
            }
        }
        IS_PAPER = paper || folia; // Folia is based on Paper

        // Detect Version
        SERVER_VERSION = Bukkit.getVersion();
    }

    public static boolean isFolia() {
        return IS_FOLIA;
    }

    public static boolean isPaper() {
        return IS_PAPER;
    }

    public static String getServerVersion() {
        return SERVER_VERSION;
    }
    
    public static String getJavaVersion() {
        return System.getProperty("java.version");
    }
    
    public static String getOS() {
        return System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ")";
    }
}
