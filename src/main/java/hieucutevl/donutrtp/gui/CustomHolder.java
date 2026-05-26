package hieucutevl.donutrtp.gui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

public interface CustomHolder extends InventoryHolder {
    void onClick(InventoryClickEvent event);
}
