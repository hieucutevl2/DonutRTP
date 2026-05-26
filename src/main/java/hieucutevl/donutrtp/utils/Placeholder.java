package hieucutevl.donutrtp.utils;

import hieucutevl.donutrtp.DonutRTP;
import hieucutevl.donutrtp.cmd.CMD_RTP;
import hieucutevl.donutrtp.cmd.services.CooldownManager;
import org.jetbrains.annotations.NotNull;
import hieucutevl.donutrtp.utils.config.ConfigManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class Placeholder extends PlaceholderExpansion {
   private final ConfigManager configManager;

   public Placeholder(ConfigManager configManager) {
      this.configManager = configManager;
   }

   @NotNull
   public String getIdentifier() {
      return "donutrtp";
   }

   @NotNull
   public String getAuthor() {
      return DonutRTP.getAuthor();
   }

   @NotNull
   public String getVersion() {
      return DonutRTP.getInstance().getVersion();
   }

   public boolean persist() {
      return true;
   }

   public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
      if (params.equalsIgnoreCase("player_is_cooldown")) {
         Player player = offlinePlayer.getPlayer();
         if (player == null) {
            return "false";
         } else {
            CooldownManager cm = DonutRTP.getInstance().getCooldownManager();
            int cooldownTime = this.configManager.getConfig().getInt("cooldown.time", 0);
            return String.valueOf(cm.getRemainingSeconds(player, cooldownTime) > 0L);
         }
      } else if (params.equalsIgnoreCase("player_is_movetimer")) {
         return String.valueOf(CMD_RTP.getInCountdownSet().contains(offlinePlayer.getUniqueId()));
      } else if (params.equalsIgnoreCase("player_cooldown_time")) {
         Player player = offlinePlayer.getPlayer();
         if (player == null) {
            return "0";
         } else {
            CooldownManager cm = DonutRTP.getInstance().getCooldownManager();
            int cooldownTime = this.configManager.getConfig().getInt("cooldown.time", 0);
            long remaining = cm.getRemainingSeconds(player, cooldownTime);
            return String.valueOf(remaining);
         }
      } else if (params.startsWith("player_cooldown_")) {
         String worldName = params.substring("player_cooldown_".length());
         Player player = offlinePlayer.getPlayer();
         if (player == null) {
            return "0";
         } else {
            CooldownManager cm = DonutRTP.getInstance().getCooldownManager();
            int cooldownTime = this.configManager.getConfig().getInt("cooldown.time", 0);
            long remaining = cm.getRemainingSeconds(player, cooldownTime, worldName);
            return String.valueOf(remaining);
         }
      } else if (params.startsWith("world_area_")) {
         String worldName = params.substring("world_area_".length());
         return String.valueOf(this.configManager.getConfig().getInt("area." + worldName, 1000));
      } else if (params.startsWith("world_cooldown_time_")) {
         return String.valueOf(this.configManager.getConfig().getInt("cooldown.time", 0));
      } else if (params.startsWith("world_movetimer_time_")) {
         return String.valueOf(this.configManager.getConfig().getInt("movetimer.time", 0));
      } else {
         return null;
      }
   }
}
