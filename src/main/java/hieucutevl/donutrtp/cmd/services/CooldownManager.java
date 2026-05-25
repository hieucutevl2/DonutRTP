package hieucutevl.donutrtp.cmd.services;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import hieucutevl.donutrtp.utils.config.ConfigManager;
import org.bukkit.entity.Player;

public class CooldownManager {
   private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

   public CooldownManager(ConfigManager configManager) {
   }

   private long calculateRemaining(UUID uuid, int cooldownSeconds) {
      Long lastUsed = this.cooldowns.get(uuid);
      if (lastUsed == null) {
         return 0L;
      } else {
         long remainingMs = lastUsed + (long)cooldownSeconds * 1000L - System.currentTimeMillis();
         if (remainingMs <= 0L) {
            this.cooldowns.remove(uuid);
            return 0L;
         } else {
            return TimeUnit.MILLISECONDS.toSeconds(remainingMs);
         }
      }
   }

   public long getRemainingSeconds(Player player, int cooldownSeconds) {
      return this.calculateRemaining(player.getUniqueId(), cooldownSeconds);
   }

   public long getRemainingSeconds(Player player, int cooldownSeconds, String world) {
      return this.getRemainingSeconds(player, cooldownSeconds);
   }

   public void setCooldown(Player player) {
      this.cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
   }

   public void setCooldown(Player player, String worldName) {
      this.setCooldown(player);
   }

   public void cleanUp() {
      long now = System.currentTimeMillis();
      long maxLifetime = 7200000L;
      this.cooldowns.entrySet().removeIf((entry) -> now - entry.getValue() > maxLifetime);
   }

   public void clearCooldowns() {
      this.cooldowns.clear();
   }
}
