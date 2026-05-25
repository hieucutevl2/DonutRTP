package hieucutevl.donutrtp.utils.location;

import java.util.logging.Logger;
import dev.dejvokep.boostedyaml.YamlDocument;
import hieucutevl.donutrtp.utils.config.SoundProfile;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class SafeLocationRequest {
   private final Player player;
   private final World targetWorld;
   private final int area;
   private final int maxTries;
   private final int originX;
   private final int originZ;
   private final int cooldown;
   private final int movetimer;
   private final boolean isCooldown;
   private final boolean isMovetimer;
   private final SoundProfile soundProfile;

   public SafeLocationRequest(Player player, World targetWorld, YamlDocument config, SoundProfile soundProfile, Logger log) {
      this.player = player;
      this.targetWorld = targetWorld;

      String worldName = targetWorld.getName();
      this.area = config.getInt("area." + worldName, 1000);
      if (this.area <= 0) {
         log.warning("Area for world '" + worldName + "' is " + this.area + "! Is this on purpose?");
      }

      this.maxTries = config.getInt("max-tries", 25);
      this.originX = 0;
      this.originZ = 0;

      this.cooldown = config.getInt("cooldown.time");
      this.movetimer = config.getInt("movetimer.time");
      this.isCooldown = config.getBoolean("cooldown.enabled");
      this.isMovetimer = config.getBoolean("movetimer.enabled");
      this.soundProfile = soundProfile;
   }

   public Player getPlayer() { return this.player; }
   public SoundProfile getSoundProfile() { return this.soundProfile; }
   public int getCooldownTime() { return this.cooldown; }
   public int getMovetimerTime() { return this.movetimer; }
   public boolean isCooldown() { return this.isCooldown; }
   public boolean isMovetimer() { return this.isMovetimer; }
   public World getTargetWorld() { return this.targetWorld; }
   public int getArea() { return this.area; }
   public int getMaxTries() { return this.maxTries; }
   public int getOriginX() { return this.originX; }
   public int getOriginZ() { return this.originZ; }
}
