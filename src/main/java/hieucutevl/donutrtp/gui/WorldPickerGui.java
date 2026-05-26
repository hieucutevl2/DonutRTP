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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorldPickerGui implements CustomHolder {

    private final Inventory inv;
    private final Map<Integer, String> slotMap = new HashMap<>();

    public WorldPickerGui(String type, List<?> worldsList) {
        YamlDocument lang = DonutRTP.getInstance().getConfigManager().getLang();

        String rawTitle = lang.getString("multiworld_gui." + type + ".title", "Select World");
        String title = rawTitle.replace("\\n", "\n");

        this.inv = Bukkit.createInventory(this, 27,
            net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                  .legacySection().deserialize(title));

        Material mat = switch (type) {
            case "nether" -> Material.NETHERRACK;
            case "end" -> Material.END_STONE;
            default -> Material.GRASS_BLOCK;
        };

        for (Object obj : worldsList) {
            String worldName = null;
            int slot = -1;

            if (obj instanceof Map<?, ?> map) {
                if (!map.isEmpty()) {
                    Object key = map.keySet().iterator().next();
                    worldName = String.valueOf(key).trim();
                    try { slot = Integer.parseInt(String.valueOf(map.get(key)).trim()); } catch (Exception ignored) {}
                }
            } else if (obj instanceof String str) {
                int lastColon = str.lastIndexOf(':');
                if (lastColon > 0 && lastColon < str.length() - 1) {
                    worldName = str.substring(0, lastColon).trim();
                    try { slot = Integer.parseInt(str.substring(lastColon + 1).trim()); } catch (Exception ignored) {}
                }
            }

            if (worldName == null || slot < 0 || slot >= 27) continue;

            String nameRaw = lang.getString("multiworld_gui." + type + ".worlds." + worldName + ".name", "&a" + worldName);
            List<String> loreRaw = lang.getStringList("multiworld_gui." + type + ".worlds." + worldName + ".lore");

            Component name = parse(nameRaw);
            List<Component> lore = loreRaw == null ? List.of() : loreRaw.stream().map(WorldPickerGui::parse).toList();

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(name);
                if (!lore.isEmpty()) meta.lore(lore);
                item.setItemMeta(meta);
            }

            if (!slotMap.containsKey(slot)) {
                this.inv.setItem(slot, item);
                this.slotMap.put(slot, worldName);
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String targetWorld = slotMap.get(event.getRawSlot());
        if (targetWorld != null) {
            player.closeInventory();
            CMD_RTP.triggerRtp(player, targetWorld);
        }
    }
    
    private static Component parse(String s) {
        return ColorTranslator.translateColor(s == null ? "" : s).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);
    }
}