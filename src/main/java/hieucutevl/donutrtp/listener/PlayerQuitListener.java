package hieucutevl.donutrtp.listener;

import hieucutevl.donutrtp.cmd.CMD_RTP;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * Cleanup player state khi quit để tránh UUID bị mắc kẹt trong
 * searchingPlayers / inCountdown / countdownGeneration / lastCommandTick.
 */
public class PlayerQuitListener implements Listener {

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        CMD_RTP.cleanupPlayer(uuid);
    }
}
