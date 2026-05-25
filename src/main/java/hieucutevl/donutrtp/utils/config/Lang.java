package hieucutevl.donutrtp.utils.config;

import dev.dejvokep.boostedyaml.YamlDocument;
import hieucutevl.donutrtp.utils.ColorTranslator;
import net.kyori.adventure.text.Component;

public class Lang {
   /** Get a lang key, parse & color codes, return Component. */
   public static Component get(YamlDocument lang, String key) {
      return format(lang.getString(key));
   }

   /** Parse a raw & color-coded string into a Component. */
   public static Component of(String string) {
      return format(string);
   }

   private static Component format(String string) {
      if (string == null) return Component.empty();
      return ColorTranslator.translateColor(string.replace("\\n", "\n"));
   }
}
