package hieucutevl.donutrtp.cmd;
import net.kyori.adventure.text.Component;

import java.io.IOException;
import java.util.logging.Logger;
import hieucutevl.donutrtp.DonutRTP;
import hieucutevl.donutrtp.gui.RtpGui;
import org.jetbrains.annotations.NotNull;
import dev.dejvokep.boostedyaml.YamlDocument;
import hieucutevl.donutrtp.utils.config.ConfigManager;
import hieucutevl.donutrtp.utils.config.Lang;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CMD_Reload implements CommandExecutor {
   private final Logger logger;
   private final ConfigManager configManager;

   public CMD_Reload(DonutRTP plugin, ConfigManager configManager) {
      this.logger = plugin.getLogger();
      this.configManager = configManager;
   }

   @Override
   public boolean onCommand(@NotNull CommandSender s, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
      if (!s.hasPermission("donutrtp.reload")) {
         s.sendMessage(Lang.get(this.configManager.getLang(), "no-permissions"));
         return true;
      }

      DonutRTP rtp = DonutRTP.getInstance();
      YamlDocument config = this.configManager.getConfig();

      try {
         config.reload();
         rtp.reloadValues(config);
         this.configManager.getNewLang();
      } catch (IOException e) {
         s.sendMessage(Lang.get(this.configManager.getLang(), "error"));
         this.logger.severe("Failed to reload config!");
         throw new RuntimeException(e);
      }

      this.configManager.reloadSoundProfile();
      rtp.getCooldownManager().clearCooldowns();
      RtpGui.invalidateCache();
      this.logger.info("Config reloaded!");
      if (s instanceof Player) {
         s.sendMessage(Lang.of("&aConfig reloaded!"));
      }
      return true;
   }
}