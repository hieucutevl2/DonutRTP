package hieucutevl.donutrtp.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Translates legacy &-color-codes (including &#RRGGBB hex) into Adventure Components.
 * Config files keep using & syntax so admins don't need to learn MiniMessage.
 */
public class ColorTranslator {
   // Matches &#RRGGBB and converts to §x§R§R§G§G§B§B legacy hex format
   private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
   private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

   /** Returns a Component from a string that may contain & color codes and &#RRGGBB hex. */
   public static Component translateColor(String text) {
      if (text == null) return Component.empty();
      // Convert &#RRGGBB → §x§R§R§G§G§B§B so legacyAmpersand serializer handles it
      Matcher m = HEX_PATTERN.matcher(text);
      StringBuilder sb = new StringBuilder();
      while (m.find()) {
         String hex = m.group(1);
         StringBuilder legacy = new StringBuilder("§x");
         for (char c : hex.toCharArray()) legacy.append('§').append(c);
         m.appendReplacement(sb, legacy.toString());
      }
      m.appendTail(sb);
      return LEGACY.deserialize(sb.toString());
   }

   /** Convenience: plain string → Component (no color processing). */
   public static Component plain(String text) {
      return text == null ? Component.empty() : Component.text(text);
   }
}
