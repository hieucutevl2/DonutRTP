package hieucutevl.donutrtp.utils.location;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class GetSafeRTP {
   public static CompletableFuture<SafeLocation> getSafeRtpLocationFromConfigAsync(SafeLocationRequest r, Plugin plugin) {
      try {
         return getSafeRtpLocationFromConfig(r, plugin.getLogger());
      } catch (Exception e) {
         CompletableFuture<SafeLocation> failed = new CompletableFuture();
         failed.completeExceptionally(e);
         return failed;
      }
   }

   public static CompletableFuture<SafeLocation> getSafeRtpLocationAsync(RtpProfile p, Plugin plugin) {
      CompletableFuture<SafeLocation> future = new CompletableFuture();
      Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
         Logger log = plugin.getLogger();

         try {
            CompletableFuture<SafeLocation> innerFuture = GetRandomLocation.getRandomSafeLocationAsync(p, log);
            innerFuture.whenComplete((safeLocation, ex) -> {
               if (ex != null) {
                  log.severe("An error occurred while finding a safe RTP location: " + ex.getMessage());
                  future.completeExceptionally(ex);
               } else {
                  future.complete(safeLocation);
               }

            });
         } catch (Exception e) {
            log.severe("An error occurred while finding a safe RTP location: " + e.getMessage());
            future.completeExceptionally(e);
         }

      });
      return future;
   }

   public static CompletableFuture<SafeLocation> getSafeRtpLocationFromConfig(SafeLocationRequest r, Logger log) {
      return GetRandomLocation.getRandomSafeLocationAsync((new RtpProfile.Builder(r.getTargetWorld(), 0, r.getArea(), r.getOriginX(), r.getOriginZ(), r.getMaxTries())).setStrategy(RtpStrategy.AUTO).build(), log);
   }
}
