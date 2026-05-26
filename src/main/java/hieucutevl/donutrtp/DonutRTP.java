package hieucutevl.donutrtp;

import java.util.logging.Logger;
import hieucutevl.donutrtp.cmd.CMD_CustomRTP;
import hieucutevl.donutrtp.cmd.CMD_Reload;
import hieucutevl.donutrtp.cmd.CMD_RTP;
import hieucutevl.donutrtp.cmd.services.CooldownManager;
import dev.dejvokep.boostedyaml.YamlDocument;
import org.bstats.bukkit.Metrics;
import hieucutevl.donutrtp.listener.OnDeath;
import hieucutevl.donutrtp.listener.PlayerQuitListener;
import hieucutevl.donutrtp.listener.RTPGuiListener;
import hieucutevl.donutrtp.utils.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class DonutRTP extends JavaPlugin {
   private String version;
   private boolean isPaper = false;
   private boolean forceCloseInventories = true;
   private boolean preventSpam = true;
   private CooldownManager cooldownManager;
   private BukkitTask cooldownCleanUpTask;
   private ConfigManager configManager;

   public static String getAuthor() {
      return "Joni";
   }

   public static DonutRTP getInstance() {
      return (DonutRTP)JavaPlugin.getPlugin(DonutRTP.class);
   }

   public String getVersion() {
      return this.version;
   }


   public boolean isPaper() {
      return this.isPaper;
   }

   public boolean isForceCloseInventories() {
      return this.forceCloseInventories;
   }

   public boolean isPreventSpam() {
      return this.preventSpam;
   }

   public CooldownManager getCooldownManager() {
      return this.cooldownManager;
   }

   public ConfigManager getConfigManager() {
      return this.configManager;
   }

   public void reloadValues(YamlDocument config) {
      this.forceCloseInventories = config.getBoolean("force-close-inventories");
      this.preventSpam = config.getBoolean("prevent-spam", true);
   }

   public void onEnable() {
      this.version = this.getDescription().getVersion();

      try {
         Class.forName("com.destroystokyo.paper.ParticleBuilder");
         this.isPaper = true;
      } catch (ClassNotFoundException var3) {
      }

      ConfigManager configManager = new ConfigManager(this);
      this.configManager = configManager;
      YamlDocument config = configManager.getConfig();
      this.reloadValues(config);
      this.displayInformation();
      this.initCommands(configManager);
      this.initEvents(configManager);
      this.setMetrics(config);
      this.initCooldownManager(configManager);
   }

   private void displayInformation() {
      if (!this.isPaper()) {
         this.getLogger().info("I strongly recommend paper for better performance!");
      }

      Logger var10000 = this.getLogger();
      String var10001 = getAuthor();
      var10000.info("DonutRTP by " + var10001 + " running version: " + this.version);
   }

   private void initCommands(ConfigManager configManager) {
      CMD_RTP cmd_rtp = new CMD_RTP(this, configManager);
      this.getCommand("rtp").setExecutor(cmd_rtp);
      this.getCommand("rtp").setTabCompleter(cmd_rtp);
      CMD_CustomRTP cmd_customrtp = new CMD_CustomRTP(this, configManager);
      this.getCommand("customrtp").setExecutor(cmd_customrtp);
      this.getCommand("customrtp").setTabCompleter(cmd_customrtp);
      CMD_Reload cmd_reload = new CMD_Reload(this, configManager);
      this.getCommand("reload").setExecutor(cmd_reload);
   }

   private void setMetrics(YamlDocument config) {
      if (config.getBoolean("metrics")) {
         int pluginId = 31489;
         new Metrics(this, pluginId);
      }
   }

   private void initEvents(ConfigManager configManager) {
      this.getServer().getPluginManager().registerEvents(new OnDeath(configManager, this), this);
      this.getServer().getPluginManager().registerEvents(new RTPGuiListener(), this);
      this.getServer().getPluginManager().registerEvents(new PlayerQuitListener(), this);
   }

   private void initCooldownManager(ConfigManager configManager) {
      this.cooldownManager = new CooldownManager(configManager);
      this.cooldownCleanUpTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
         if (this.cooldownManager != null) {
            this.cooldownManager.cleanUp();
         }

      }, 36000L, 36000L);
   }

   public void onDisable() {
      if (this.cooldownCleanUpTask != null) {
         this.cooldownCleanUpTask.cancel();
      }

      CMD_RTP.getScheduler().shutdownNow();
   }
}
