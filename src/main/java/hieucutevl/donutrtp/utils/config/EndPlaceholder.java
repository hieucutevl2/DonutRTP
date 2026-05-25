package hieucutevl.donutrtp.utils.config;

import hieucutevl.donutrtp.utils.ColorTranslator;
import hieucutevl.donutrtp.utils.location.SafeLocation;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;

public class EndPlaceholder {
   private final SafeLocation safeLocation;

   public EndPlaceholder(SafeLocation safeLocation) {
      this.safeLocation = safeLocation;
   }

   /** Replace placeholders in a raw lang string then parse to Component. */
   public Component handle(Component raw) {
      // Serialize → replace → deserialize is the safe way to handle placeholder replacement
      // But since our strings come from Lang.get() already parsed, we work on the raw string level.
      // Receive the raw string from lang file and inject here.
      throw new UnsupportedOperationException("Use handle(String) then pass through Lang.of()");
   }

   /** Replace placeholders in a raw & color-coded string, return Component. */
   public Component handle(String raw) {
      Location loc = this.safeLocation.location();
      String s = raw
            .replace("%tries%",    String.valueOf(this.safeLocation.tries()))
            .replace("%x%",        String.valueOf(loc.getBlockX()))
            .replace("%y%",        String.valueOf(loc.getBlockY()))
            .replace("%z%",        String.valueOf(loc.getBlockZ()))
            .replace("%world%",    loc.getWorld().getName())
            .replace("%position%", loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
      return ColorTranslator.translateColor(s);
   }
}
