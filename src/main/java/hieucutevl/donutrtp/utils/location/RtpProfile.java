package hieucutevl.donutrtp.utils.location;

import java.util.EnumSet;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;

public record RtpProfile(World world, int minRadius, int maxRadius, int centerX, int centerZ, int maxTries, RtpStrategy strategy) {

   public static final Set<Material> BLOCK_BLACKLIST = EnumSet.of(Material.LAVA, Material.WATER, Material.AIR);

   public RtpProfile(World world, int minRadius, int maxRadius, int centerX, int centerZ, int maxTries, RtpStrategy strategy) {
      if (strategy == RtpStrategy.AUTO) {
         strategy = world.getEnvironment() == Environment.NETHER ? RtpStrategy.BOTTOM_UP : RtpStrategy.HIGHEST_BLOCK;
      }
      this.world = world;
      this.minRadius = minRadius;
      this.maxRadius = maxRadius;
      this.centerX = centerX;
      this.centerZ = centerZ;
      this.maxTries = maxTries;
      this.strategy = strategy;
   }

   public static class Builder {
      private final World world;
      private final int minRadius;
      private final int maxRadius;
      private final int centerX;
      private final int centerZ;
      private final int maxTries;
      private RtpStrategy strategy = RtpStrategy.AUTO;

      public Builder(World world, int minRadius, int maxRadius, int centerX, int centerZ, int maxTries) {
         this.world = world;
         this.minRadius = minRadius;
         this.maxRadius = maxRadius;
         this.centerX = centerX;
         this.centerZ = centerZ;
         this.maxTries = maxTries;
      }

      public Builder setStrategy(RtpStrategy strategy) {
         this.strategy = strategy;
         return this;
      }

      public RtpProfile build() {
         return new RtpProfile(this.world, this.minRadius, this.maxRadius, this.centerX, this.centerZ, this.maxTries, this.strategy);
      }
   }
}
