package hieucutevl.donutrtp.listener;
import net.kyori.adventure.text.Component;

import java.util.concurrent.CompletableFuture;
import hieucutevl.donutrtp.DonutRTP;
import dev.dejvokep.boostedyaml.YamlDocument;
import hieucutevl.donutrtp.utils.config.ConfigManager;
import hieucutevl.donutrtp.utils.config.Lang;
import hieucutevl.donutrtp.utils.location.GetSafeRTP;
import hieucutevl.donutrtp.utils.location.SafeLocation;
import hieucutevl.donutrtp.utils.location.SafeLocationRequest;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class OnDeath implements Listener {
   private final ConfigManager configManager;
   private final JavaPlugin plugin;

   public OnDeath(ConfigManager configManager, JavaPlugin plugin) {
      this.configManager = configManager;
      this.plugin = plugin;
   }

   @EventHandler
   public void onRespawn(PlayerRespawnEvent event) {
      YamlDocument config = this.configManager.getConfig();
      if (!config.getBoolean("on-death")) return;

      Player p = event.getPlayer();
      if (p.getBedSpawnLocation() != null) return;
      SafeLocationRequest request = new SafeLocationRequest(p, p.getWorld(), config, this.configManager.getSoundProfile(), this.plugin.getLogger());
      CompletableFuture<SafeLocation> future = GetSafeRTP.getSafeRtpLocationFromConfigAsync(request, this.plugin);
      future.thenAccept((safeLocation) -> {
         if (safeLocation == null) {
            p.sendMessage(Lang.get(this.configManager.getLang(), "error"));
         } else {
            if (DonutRTP.getInstance().isPaper()) {
               p.teleportAsync(safeLocation.location());
            } else {
               Bukkit.getScheduler().runTask(this.plugin, () -> p.teleport(safeLocation.location()));
            }
         }
      });
   }
}