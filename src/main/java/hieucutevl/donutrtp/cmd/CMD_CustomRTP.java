package hieucutevl.donutrtp.cmd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import hieucutevl.donutrtp.DonutRTP;
import org.jetbrains.annotations.NotNull;
import dev.dejvokep.boostedyaml.YamlDocument;
import hieucutevl.donutrtp.utils.config.ConfigManager;
import hieucutevl.donutrtp.utils.config.Lang;
import hieucutevl.donutrtp.utils.location.GetSafeRTP;
import hieucutevl.donutrtp.utils.location.RtpProfile;
import hieucutevl.donutrtp.utils.location.RtpStrategy;
import hieucutevl.donutrtp.utils.location.SafeLocation;
import hieucutevl.donutrtp.utils.location.SafeLocationRequest;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class CMD_CustomRTP implements CommandExecutor, TabCompleter {
   private final JavaPlugin plugin;
   private final ConfigManager configManager;
   private final List<String> OPTIONAL_KEYS = List.of("tries:25", "feedback:true", "feedback:false", "cooldown:true", "cooldown:false", "delay:5", "strategy:AUTO", "strategy:HIGHEST_BLOCK", "strategy:BOTTOM_UP", "strategy:TOP_DOWN");

   public CMD_CustomRTP(JavaPlugin plugin, ConfigManager configManager) {
      this.plugin = plugin;
      this.configManager = configManager;
   }

   public boolean onCommand(@NotNull CommandSender s, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      YamlDocument lang = this.configManager.getLang();
      if (!s.hasPermission("donutrtp.custom")) {
         s.sendMessage(Lang.get(lang, "no-permissions"));
         return true;
      } else if (args.length < 6) {
         s.sendMessage(Lang.of("&cUsage: /customrtp <player/%player%> <world> <startRadius> <endRadius> <originX> <originZ> [params...]"));
         return true;
      } else {
         Player targetPlayer;
         if (args[0].equalsIgnoreCase("%player%")) {
            if (!(s instanceof Player)) {
               s.sendMessage(Lang.of("&cConsole must specify a player name."));
               return true;
            }

            targetPlayer = (Player)s;
         } else {
            targetPlayer = Bukkit.getPlayer(args[0]);
            if (targetPlayer == null) {
               s.sendMessage(Lang.of("&cPlayer '" + args[0] + "' not found."));
               return true;
            }
         }

         World world = Bukkit.getWorld(args[1]);
         if (world == null) {
            s.sendMessage(Lang.of("&cWorld '" + args[1] + "' not found."));
            return true;
         } else {
            int startRadius;
            int endRadius;
            int originX;
            int originZ;
            try {
               startRadius = Integer.parseInt(args[2]);
               endRadius = Integer.parseInt(args[3]);
               originX = Integer.parseInt(args[4]);
               originZ = Integer.parseInt(args[5]);
            } catch (NumberFormatException var35) {
               s.sendMessage(Lang.of("&cInvalid number provided for radius or origin."));
               return true;
            }

            YamlDocument config = this.configManager.getConfig();
            SafeLocationRequest r = new SafeLocationRequest(targetPlayer, world, config, this.configManager.getSoundProfile(), this.plugin.getLogger());
            int maxTries = r.getMaxTries();
            boolean feedback = false;
            boolean cooldown = false;
            int delay = 0;
            RtpStrategy strategy = RtpStrategy.AUTO;

            for(int i = 6; i < args.length; ++i) {
               String arg = args[i];
               if (!arg.contains(":")) {
                  s.sendMessage(Lang.of("&cInvalid argument format: '" + arg + "'. Use key:value"));
                  return true;
               }

               String[] split = arg.split(":", 2);
               String key = split[0].toLowerCase();
               String value = split[1];

               try {
                  switch (key) {
                     case "tries":
                        maxTries = Integer.parseInt(value);
                        break;
                     case "feedback":
                        feedback = Boolean.parseBoolean(value);
                        break;
                     case "cooldown":
                        cooldown = Boolean.parseBoolean(value);
                        break;
                     case "delay":
                        delay = Integer.parseInt(value);
                        break;
                     case "strategy":
                        try {
                           strategy = RtpStrategy.valueOf(value.toUpperCase());
                        } catch (IllegalArgumentException e) {
                           s.sendMessage(Lang.of("&cInvalid strategy. Use AUTO, HIGHEST_BLOCK, BOTTOM_UP, or TOP_DOWN."));
                           return true;
                        }
                        break;
                     default:
                        s.sendMessage(Lang.of("&cUnknown parameter: " + key));
                        return true;
                  }
               } catch (NumberFormatException e) {
                  s.sendMessage(Lang.of("&cInvalid number for key " + key));
                  return true;
               }
            }


            DonutRTP rtp = DonutRTP.getInstance();
            if (rtp.isPreventSpam() && CMD_RTP.getSearchingPlayers().contains(targetPlayer.getUniqueId())) {
               if (feedback) {
                  s.sendMessage(Lang.get(lang, "chat.searching-already"));
               }

               return true;
            } else {
               if (cooldown) {
                  long remainingSec = rtp.getCooldownManager().getRemainingSeconds(targetPlayer, r.getCooldownTime(), world.getName());
                  if (remainingSec > 0L) {
                     targetPlayer.sendMessage(Lang.of(lang.getString("chat.cooldown").replace("%time%", String.valueOf(remainingSec))));
                     return true;
                  }
               }

               CompletableFuture<SafeLocation> safeLocation = GetSafeRTP.getSafeRtpLocationAsync((new RtpProfile.Builder(world, startRadius, endRadius, originX, originZ, maxTries)).setStrategy(strategy).build(), this.plugin);
               if (rtp.isPreventSpam()) {
                  CMD_RTP.getSearchingPlayers().add(targetPlayer.getUniqueId());
               }

               final boolean finalFeedback = feedback;
               final boolean finalCooldown = cooldown;
               final int finalDelay = delay;
               final SafeLocationRequest finalR = r;

               Runnable task = () -> safeLocation.thenAccept((safe) -> {
                     if (safe == null) {
                        if (rtp.isPreventSpam()) {
                           CMD_RTP.getSearchingPlayers().remove(targetPlayer.getUniqueId());
                        }

                     } else {
                        Bukkit.getScheduler().runTask(this.plugin, () -> {
                           if (!targetPlayer.isOnline()) {
                              if (rtp.isPreventSpam()) {
                                 CMD_RTP.getSearchingPlayers().remove(targetPlayer.getUniqueId());
                              }

                           } else {
                              if (finalCooldown) {
                                 rtp.getCooldownManager().setCooldown(targetPlayer, world.getName());
                              }



                              if (rtp.isForceCloseInventories()) {
                                 targetPlayer.closeInventory();
                              }

                              if (rtp.isPaper()) {
                                 targetPlayer.teleportAsync(safe.location()).thenRun(() -> {
                                    if (finalFeedback) {
                                       CMD_RTP.sendEndFeedback(finalR, targetPlayer, this.plugin.getLogger(), lang, safe);
                                    }
                                 });
                              } else {
                                 targetPlayer.teleport(safe.location());
                                 if (finalFeedback) {
                                    CMD_RTP.sendEndFeedback(finalR, targetPlayer, this.plugin.getLogger(), lang, safe);
                                 }
                              }

                              if (rtp.isPreventSpam()) {
                                 CMD_RTP.getSearchingPlayers().remove(targetPlayer.getUniqueId());
                              }

                           }
                        });
                     }
                  }).exceptionally((ex) -> {
                     if (rtp.isPreventSpam()) {
                        CMD_RTP.getSearchingPlayers().remove(targetPlayer.getUniqueId());
                     }

                     if (finalFeedback) {
                        targetPlayer.sendMessage(Lang.get(lang, "error"));
                     }

                     this.plugin.getLogger().severe("An error occurred while finding a safe location: " + ex.getMessage());
                     ex.printStackTrace();

                     return null;
                  });
               if (finalDelay == 0) {
                  task.run();
               } else {
                  CMD_RTP.getScheduler().schedule(task, (long)finalDelay, TimeUnit.SECONDS);
               }

               return true;
            }
         }
      }
   }

   public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
      if (!sender.hasPermission("donutrtp.custom")) {
         return Collections.emptyList();
      } else {
         List<String> completions = new ArrayList();
         if (args.length == 1) {
            completions.add("%player%");
            Bukkit.getOnlinePlayers().forEach((p) -> completions.add(p.getName()));
         } else if (args.length == 2) {
            Bukkit.getWorlds().forEach((w) -> completions.add(w.getName()));
         } else if (args.length == 3) {
            completions.add("1000");
         } else if (args.length == 4) {
            completions.add("5000");
         } else if (args.length == 5) {
            completions.add("0");
         } else if (args.length == 6) {
            completions.add("0");
         } else {
            Set<String> usedBaseKeys = new HashSet();

            for(int i = 0; i < args.length - 1; ++i) {
               String arg = args[i];
               if (arg.contains(":")) {
                  String baseKey = arg.split(":")[0].toLowerCase();
                  usedBaseKeys.add(baseKey);
               }
            }

            for(String entry : this.OPTIONAL_KEYS) {
               String entryBaseKey = entry.split(":")[0].toLowerCase();
               if (!usedBaseKeys.contains(entryBaseKey)) {
                  completions.add(entry);
               }
            }
         }

         String lastPart = args[args.length - 1].toLowerCase();
         return (List)completions.stream().filter((s) -> s.toLowerCase().startsWith(lastPart)).sorted().collect(Collectors.toList());
      }
   }
}
