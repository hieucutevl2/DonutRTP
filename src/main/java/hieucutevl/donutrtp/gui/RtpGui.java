package hieucutevl.donutrtp.gui;

import hieucutevl.donutrtp.DonutRTP;
import hieucutevl.donutrtp.utils.ColorTranslator;
import dev.dejvokep.boostedyaml.YamlDocument;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class RtpGui {

   private static String cachedTitle = null;

   public static void invalidateCache() {
      cachedTitle = null;
   }

   public static String getTitle() {
      if (cachedTitle == null) cachedTitle = resolveTitle();
      return cachedTitle;
   }

   private static String resolveTitle() {
      YamlDocument lang = DonutRTP.getInstance().getConfigManager().getLang();
      String raw = lang.getString("gui.title", "Random Teleport");
      return raw == null ? "Random Teleport" : raw.replace("\\n", "\n");
   }

   public static void open(Player player) {
      cachedTitle = resolveTitle();
      YamlDocument lang = DonutRTP.getInstance().getConfigManager().getLang();

      Component worldName  = parse(lang.getString("gui.world.name",  "&a&lWorld"));
      Component netherName = parse(lang.getString("gui.nether.name", "&c&lNether"));
      Component endName    = parse(lang.getString("gui.end.name",    "&e&lThe End"));

      List<Component> worldLore  = parseList(lang.getStringList("gui.world.lore"));
      List<Component> netherLore = parseList(lang.getStringList("gui.nether.lore"));
      List<Component> endLore    = parseList(lang.getStringList("gui.end.lore"));

      Inventory inv = Bukkit.createInventory(null, 27,
            net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                  .legacySection().deserialize(cachedTitle));

      inv.setItem(11, buildItem(Material.GRASS_BLOCK, worldName,  worldLore));
      inv.setItem(13, buildItem(Material.NETHERRACK,  netherName, netherLore));
      inv.setItem(15, buildItem(Material.END_STONE,   endName,    endLore));
      player.openInventory(inv);
   }

   private static Component parse(String s) {
      return ColorTranslator.translateColor(s == null ? "" : s).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);
   }

   private static List<Component> parseList(List<String> list) {
      if (list == null) return List.of();
      return list.stream().map(RtpGui::parse).toList();
   }

   private static ItemStack buildItem(Material mat, Component name, List<Component> lore) {
      ItemStack item = new ItemStack(mat);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.displayName(name);
         if (!lore.isEmpty()) meta.lore(lore);
         item.setItemMeta(meta);
      }
      return item;
   }
}