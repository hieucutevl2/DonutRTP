package hieucutevl.donutrtp.utils.config;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfigManager {
   private final JavaPlugin pl;
   private final Logger logger;
   private YamlDocument config;
   private YamlDocument lang;
   private SoundProfile soundProfile;

   public ConfigManager(JavaPlugin pl) {
      this.pl = pl;
      this.logger = pl.getLogger();

      try {
         this.config = this.createConfig();
         this.lang = this.createLangConfig(this.config.getString("messages"));
         this.soundProfile = buildSoundProfile(this.config);
      } catch (Exception e) {
         this.logger.severe("Failed to load config files, disabling plugin...");
         this.logger.severe(e.getMessage());
         Bukkit.getPluginManager().disablePlugin(pl);
      }
   }

   public YamlDocument getConfig() { return this.config; }
   public YamlDocument getLang() { return this.lang; }
   public SoundProfile getSoundProfile() { return this.soundProfile; }
   public YamlDocument getNewLang() {
      this.lang = this.createLangConfig(this.config.getString("messages"));
      return this.lang;
   }

   public SoundProfile reloadSoundProfile() {
      this.soundProfile = buildSoundProfile(this.config);
      return this.soundProfile;
   }

   private static SoundProfile buildSoundProfile(YamlDocument cfg) {
      return new SoundProfile(
            cfg.getBoolean("successful-tp.enabled", true),
            cfg.getString("successful-tp.end",                          "ENTITY_PLAYER_LEVELUP"),
            ((Double) cfg.getDouble("successful-tp.end-pitch",          1.0)).floatValue(),
            cfg.getString("movetimer.sound-canceled",           "ENTITY_VILLAGER_NO"),
            ((Double) cfg.getDouble("movetimer.sound-canceled-pitch", 1.0)).floatValue(),
            cfg.getString("movetimer.sound-tick",               "UI_BUTTON_CLICK"),
            ((Double) cfg.getDouble("movetimer.sound-tick-pitch", 1.0)).floatValue()
      );
   }

   private YamlDocument createConfig() {
      try {
         return createSimpleDocument(this.pl.getDataFolder(), "config.yml", "config.yml");
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   private YamlDocument createLangConfig(String lang) {
      if (lang == null || lang.isBlank()) {
         this.logger.warning("Invalid language setting, defaulting to English (en)");
         lang = "en";
      }
      try {
         File langDir = new File(this.pl.getDataFolder(), "lang");
         if (!langDir.exists()) langDir.mkdirs();

         // Extract all bundled lang files
         for (String l : new String[]{"en", "vn"}) {
            createSimpleDocument(langDir, l + ".yml", "lang/" + l + ".yml");
         }

         return createSimpleDocument(langDir, lang + ".yml", "lang/" + lang + ".yml");
      } catch (IOException e) {
         this.logger.severe("Failed to load lang/" + lang + ".yml");
         throw new RuntimeException(e);
      }
   }

   private YamlDocument createSimpleDocument(File folder, String child, String resource) throws IOException {
      return YamlDocument.create(
            new File(folder, child),
            this.pl.getResource(resource),
            GeneralSettings.builder().setUseDefaults(false).build(),
            LoaderSettings.builder().setAutoUpdate(false).build(),
            DumperSettings.DEFAULT,
            UpdaterSettings.builder().setKeepAll(true).build()
      );
   }
}