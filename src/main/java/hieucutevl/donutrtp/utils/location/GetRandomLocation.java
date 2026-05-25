package hieucutevl.donutrtp.utils.location;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import hieucutevl.donutrtp.DonutRTP;
import hieucutevl.donutrtp.utils.random.GetRandomPoint;
import hieucutevl.donutrtp.utils.random.RandomPoint;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World.Environment;

public class GetRandomLocation {
   public static CompletableFuture<SafeLocation> getRandomSafeLocationAsync(RtpProfile profile, Logger log) {
      return attemptFindLocation(profile, log, 1);
   }

   private static CompletableFuture<SafeLocation> attemptFindLocation(RtpProfile profile, Logger log, int attempt) {
      if (attempt > profile.maxTries()) {
         log.severe("Failed to find a safe location after " + (attempt - 1) + " tries.");
         return CompletableFuture.failedFuture(new SafeLocationNotFoundException(attempt - 1));
      }

      // Shape hardcoded to SQUARE for simplicity
      RandomPoint point = GetRandomPoint.getRandomPointOnSquare(profile.minRadius(), profile.maxRadius(), profile.centerX(), profile.centerZ());

      int chunkX = point.x() >> 4;
      int chunkZ = point.y() >> 4;

      CompletableFuture<ChunkSnapshot> chunkFuture;
      if (DonutRTP.getInstance().isPaper()) {
         chunkFuture = profile.world().getChunkAtAsync(chunkX, chunkZ)
               .thenApply((chunk) -> chunk.getChunkSnapshot(true, true, false));
      } else {
         chunkFuture = CompletableFuture.completedFuture(
               profile.world().getChunkAt(chunkX, chunkZ).getChunkSnapshot(true, true, false));
      }

      return chunkFuture.thenCompose((snapshot) -> {
         Location safeLoc = findSafeYInChunk(profile, snapshot, point.x(), point.y(), log);
         if (safeLoc == null) {
            return attemptFindLocation(profile, log, attempt + 1);
         }


         return CompletableFuture.completedFuture(new SafeLocation(safeLoc, attempt));
      });
   }

   private static Location findSafeYInChunk(RtpProfile profile, ChunkSnapshot snapshot, int worldX, int worldZ, Logger log) {
      RtpStrategy strategy = profile.strategy();
      int cx = worldX & 15;
      int cz = worldZ & 15;

      if (strategy != RtpStrategy.BOTTOM_UP && strategy != RtpStrategy.TOP_DOWN) {
         int highestY = snapshot.getHighestBlockYAt(cx, cz);
         if (highestY < profile.world().getMinHeight() || highestY >= profile.world().getMaxHeight()) {
            return null;
         }
         Material blockMat = snapshot.getBlockData(cx, highestY, cz).getMaterial();
         if (RtpProfile.BLOCK_BLACKLIST.contains(blockMat)) {
            return null;
         }
         if (highestY + 1 >= profile.world().getMaxHeight()) {
            return null;
         }
         return new Location(profile.world(), worldX + 0.5, highestY + 1, worldZ + 0.5);
      }

      int worldMin = profile.world().getMinHeight();
      int worldMax = profile.world().getEnvironment() == Environment.NETHER
            ? profile.world().getLogicalHeight()
            : profile.world().getMaxHeight();
      int scanMin = worldMin;
      int scanMax = worldMax - 3;

      if (strategy == RtpStrategy.BOTTOM_UP) {
         for (int y = scanMin; y <= scanMax; y++) {
            if (isSafeAt(snapshot, cx, y, cz, profile)) {
               return new Location(profile.world(), worldX + 0.5, y + 1, worldZ + 0.5);
            }
         }
      } else {
         for (int y = scanMax; y >= scanMin; y--) {
            if (isSafeAt(snapshot, cx, y, cz, profile)) {
               return new Location(profile.world(), worldX + 0.5, y + 1, worldZ + 0.5);
            }
         }
      }
      return null;
   }

   private static boolean isSafeAt(ChunkSnapshot snapshot, int cx, int y, int cz, RtpProfile profile) {
      Material ground = snapshot.getBlockData(cx, y, cz).getMaterial();
      if (!ground.isSolid() || RtpProfile.BLOCK_BLACKLIST.contains(ground)) {
         return false;
      }
      return snapshot.getBlockData(cx, y + 1, cz).getMaterial().isAir()
            && snapshot.getBlockData(cx, y + 2, cz).getMaterial().isAir();
   }
}
