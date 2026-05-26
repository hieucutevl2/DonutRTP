package hieucutevl.donutrtp.listener;

import hieucutevl.donutrtp.gui.CustomHolder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public class RTPGuiListener implements Listener {

   @EventHandler
   public void onInventoryClick(InventoryClickEvent event) {
      Inventory inv = event.getInventory();
      if (inv.getHolder() instanceof CustomHolder holder) {
          event.setCancelled(true);
          
          // Only trigger the action if the click was in the top (custom) inventory
          Inventory clickedInv = event.getClickedInventory();
          if (clickedInv != null && clickedInv.equals(event.getView().getTopInventory())) {
              holder.onClick(event);
          }
      }
   }

   @EventHandler
   public void onInventoryDrag(InventoryDragEvent event) {
      if (event.getInventory().getHolder() instanceof CustomHolder) {
          event.setCancelled(true);
      }
   }
}
