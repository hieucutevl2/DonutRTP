package hieucutevl.donutrtp.gui;

import hieucutevl.donutrtp.DonutRTP;
import hieucutevl.donutrtp.cmd.CMD_RTP;
import hieucutevl.donutrtp.utils.ColorTranslator;
import dev.dejvokep.boostedyaml.YamlDocument;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class RtpGui implements CustomHolder {

   private static String cachedTitle = null;
   private final Inventory inv;

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
      player.openInventory(new RtpGui().getInventory());
   }

   public RtpGui() {
      YamlDocument lang = DonutRTP.getInstance().getConfigManager().getLang();

      Component worldName  = parse(lang.getString("gui.world.name",  "&a&lWorld"));
      Component netherName = parse(lang.getString("gui.nether.name", "&c&lNether"));
      Component endName    = parse(lang.getString("gui.end.name",    "&e&lThe End"));

      List<Component> worldLore  = parseList(lang.getStringList("gui.world.lore"));
      List<Component> netherLore = parseList(lang.getStringList("gui.nether.lore"));
      List<Component> endLore    = parseList(lang.getStringList("gui.end.lore"));

      this.inv = Bukkit.createInventory(this, 27,
            net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                  .legacySection().deserialize(getTitle()));

      this.inv.setItem(11, buildItem(Material.GRASS_BLOCK, worldName,  worldLore));
      this.inv.setItem(13, buildItem(Material.NETHERRACK,  netherName, netherLore));
      this.inv.setItem(15, buildItem(Material.END_STONE,   endName,    endLore));
   }

   @Override
   public Inventory getInventory() {
       return this.inv;
   }

   @Override
   public void onClick(InventoryClickEvent event) {
      if (!(event.getWhoClicked() instanceof Player player)) return;

      String configType = switch (event.getRawSlot()) {
         case 11 -> "world";
         case 13 -> "nether";
         case 15 -> "end";
         default -> null;
      };

      if (configType == null) return;
      
      YamlDocument config = DonutRTP.getInstance().getConfigManager().getConfig();
      boolean masterEnabled = config.getBoolean("multi-world.enabled", false);
      boolean typeEnabled = config.getBoolean("multi-world." + configType + ".enabled", false);

      if (masterEnabled && typeEnabled) {
          List<?> worldsList = config.getList("multi-world." + configType + ".worlds");
          if (worldsList != null && !worldsList.isEmpty()) {
              player.openInventory(new WorldPickerGui(configType, worldsList).getInventory());
              return;
          }
      }
      
      String fallbackWorld = switch (configType) {
         case "nether" -> "world_nether";
         case "end" -> "world_the_end";
         default -> "world";
      };

      player.closeInventory();
      CMD_RTP.triggerRtp(player, fallbackWorld);
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