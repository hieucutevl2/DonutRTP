package hieucutevl.donutrtp.listener;

import hieucutevl.donutrtp.gui.RtpGui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public class RTPGuiListener implements Listener {

   private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

   private boolean isRtpGui(InventoryClickEvent e) {
      Component invTitle = e.getView().title();
      Component expected = LEGACY.deserialize(RtpGui.getTitle());
      return invTitle.equals(expected);
   }

   @EventHandler
   public void onInventoryClick(InventoryClickEvent event) {
      if (!(event.getWhoClicked() instanceof Player player)) return;
      if (!isRtpGui(event)) return;

      event.setCancelled(true);

      Inventory inv = event.getClickedInventory();
      if (inv == null || !inv.equals(event.getView().getTopInventory())) return;

      String world = switch (event.getRawSlot()) {
         case 11 -> "world";
         case 13 -> "world_nether";
         case 15 -> "world_the_end";
         default -> null;
      };

      if (world == null) return;
      player.closeInventory();
      player.performCommand("rtp " + world);
   }

   @EventHandler
   public void onInventoryDrag(InventoryDragEvent event) {
      Component invTitle = event.getView().title();
      Component expected = LEGACY.deserialize(RtpGui.getTitle());
      if (invTitle.equals(expected)) {
         event.setCancelled(true);
      }
   }
}
