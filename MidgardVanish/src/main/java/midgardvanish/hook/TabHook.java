package midgardvanish.hook;

import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.nametag.NameTagManager;
import org.bukkit.entity.Player;

public class TabHook {

    public static void pauseTeamHandling(Player player) {
        try {
            TabPlayer tabPlayer = TabAPI.getInstance().getPlayer(player.getUniqueId());
            NameTagManager manager = TabAPI.getInstance().getNameTagManager();
            if (tabPlayer != null && manager != null) {
                manager.pauseTeamHandling(tabPlayer);
            }
        } catch (Exception ignored) {
        }
    }

    public static void resumeTeamHandling(Player player) {
        try {
            TabPlayer tabPlayer = TabAPI.getInstance().getPlayer(player.getUniqueId());
            NameTagManager manager = TabAPI.getInstance().getNameTagManager();
            if (tabPlayer != null && manager != null) {
                manager.resumeTeamHandling(tabPlayer);
            }
        } catch (Exception ignored) {
        }
    }
}
