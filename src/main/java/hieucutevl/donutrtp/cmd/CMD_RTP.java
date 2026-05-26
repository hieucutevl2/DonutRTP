package hieucutevl.donutrtp.cmd;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import hieucutevl.donutrtp.DonutRTP;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import dev.dejvokep.boostedyaml.YamlDocument;
import hieucutevl.donutrtp.utils.config.ConfigManager;
import hieucutevl.donutrtp.utils.config.EndPlaceholder;
import hieucutevl.donutrtp.utils.config.Lang;
import hieucutevl.donutrtp.utils.location.GetSafeRTP;
import hieucutevl.donutrtp.utils.location.SafeLocation;
import hieucutevl.donutrtp.utils.config.SoundProfile;
import hieucutevl.donutrtp.utils.location.SafeLocationRequest;
import hieucutevl.donutrtp.gui.RtpGui;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class CMD_RTP implements CommandExecutor, TabCompleter {
   private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
   private static final Set<UUID> inCountdown = ConcurrentHashMap.newKeySet();
   private static final Map<UUID, Long> lastCommandTick = new ConcurrentHashMap<>();
   private static final Map<UUID, Integer> countdownGeneration = new ConcurrentHashMap<>();
   private static final Set<UUID> searchingPlayers = ConcurrentHashMap.newKeySet();
   private final JavaPlugin plugin;
   private final Logger logger;
   private final YamlDocument config;
   private final ConfigManager configManager;

   public CMD_RTP(JavaPlugin plugin, ConfigManager configManager) {
      this.plugin = plugin;
      this.logger = plugin.getLogger();
      this.configManager = configManager;
      this.config = configManager.getConfig();
   }

   public static ScheduledExecutorService getScheduler() {
      return scheduler;
   }

   public static Set<UUID> getInCountdownSet() {
      return inCountdown;
   }

   public static Set<UUID> getSearchingPlayers() {
      return searchingPlayers;
   }

   /**
    * Cleanup tất cả state liên quan đến player khi quit.
    * Bump generation để countdown đang chạy tự dừng ở bước tiếp theo.
    */
   public static void cleanupPlayer(UUID uuid) {
      searchingPlayers.remove(uuid);
      inCountdown.remove(uuid);
      countdownGeneration.remove(uuid);
      lastCommandTick.remove(uuid);
   }

   /**
    * Trigger RTP trực tiếp cho player vào world chỉ định.
    * Dùng thay cho performCommand("rtp ...") từ GUI để tránh overhead command pipeline.
    */
   public static void triggerRtp(Player player, String worldName) {
      DonutRTP rtp = DonutRTP.getInstance();
      ConfigManager cm = rtp.getConfigManager();

      if (!player.hasPermission("donutrtp.world.*") && !player.hasPermission("donutrtp.world." + worldName)) {
         player.sendMessage(Lang.get(cm.getLang(), "no-permissions"));
         return;
      }

      World targetWorld = Bukkit.getWorld(worldName);
      if (targetWorld == null) {
         player.sendMessage(Lang.get(cm.getLang(), "error"));
         return;
      }

      SafeLocationRequest request;
      try {
         request = new SafeLocationRequest(player, targetWorld, cm.getConfig(), cm.getSoundProfile(), rtp.getLogger());
      } catch (IllegalArgumentException e) {
         player.sendMessage(Lang.get(cm.getLang(), "error"));
         return;
      }

      if (request.isCooldown()) {
         long remaining = rtp.getCooldownManager().getRemainingSeconds(player, request.getCooldownTime(), worldName);
         if (remaining > 0L) {
            net.kyori.adventure.text.Component msg = Lang.of(cm.getLang().getString("chat.cooldown").replace("%time%", String.valueOf(remaining)));
            player.sendMessage(msg);
            player.sendActionBar(msg);
            return;
         }
      }

      if (rtp.isPreventSpam() && searchingPlayers.contains(player.getUniqueId())) return;

      // Dùng instance CMD_RTP tạm để gọi các private method — lấy qua plugin
      CMD_RTP cmd = (CMD_RTP) rtp.getCommand("rtp").getExecutor();
      if (request.isMovetimer()) {
         cmd.startCountdown(player, player, request);
      } else {
         cmd.performTeleport(player, player, request);
      }
   }

   public static void sendEndFeedback(SafeLocationRequest request, Player p, Logger logger, YamlDocument lang, SafeLocation safeLocation) {
      EndPlaceholder ep = new EndPlaceholder(safeLocation);
      SoundProfile sp = request.getSoundProfile();
      
      if (sp.enabled()) {
         net.kyori.adventure.text.Component endMsg = ep.handle(lang.getString("chat.end"));
         p.sendMessage(endMsg);
         p.sendActionBar(endMsg);

         if (sp.end() != null && !sp.end().isBlank() && !sp.end().equalsIgnoreCase("false")) {
            try {
               Sound sound = Registry.SOUNDS.get(NamespacedKey.minecraft(sp.end().toLowerCase()));
               if (sound == null) { logger.warning("Unknown sound: " + sp.end()); return; }
               p.playSound(p.getLocation(), sound, 1.0F, sp.endPitch());
            } catch (IllegalArgumentException e) {
               logger.warning("Invalid sound name for sound.end: " + sp.end());
            }
         }
      }
   }

   public boolean onCommand(@NotNull CommandSender s, @NotNull Command cmd, @NotNull String string, @NotNull String[] args) {
      // Must be a player
      if (!(s instanceof Player p)) {
         s.sendMessage(Lang.get(this.configManager.getLang(), "only-player"));
         return true;
      }

      // Basic RTP permission
      if (!s.hasPermission("donutrtp.rtp")) {
         s.sendMessage(Lang.get(this.configManager.getLang(), "no-permissions"));
         return true;
      }

      // No args → open world selector GUI
      if (args.length == 0) {
         RtpGui.open(p);
         return true;
      }

      // Require exactly one argument: the world name
      if (args.length != 1) {
         RtpGui.open(p);
         return true;
      }

      String worldName = args[0];

      // Resolve world
      World targetWorld = Bukkit.getWorld(worldName);
      if (targetWorld == null) {
         RtpGui.open(p);
         return true;
      }

      // Per-world permission
      if (!s.hasPermission("donutrtp.world.*") && !s.hasPermission("donutrtp.world." + worldName)) {
         RtpGui.open(p);
         return true;
      }

      DonutRTP rtp = DonutRTP.getInstance();

      SafeLocationRequest request;
      try {
         request = new SafeLocationRequest(p, targetWorld, this.config, this.configManager.getSoundProfile(), this.logger);
      } catch (IllegalArgumentException var10) {
         s.sendMessage(Lang.get(this.configManager.getLang(), "error"));
         this.logger.severe("Failed to create SafeLocationRequest! Check your config for invalid world names!");
         return true;
      }

      if (request.isCooldown()) {
         long remainingSec = rtp.getCooldownManager().getRemainingSeconds(p, request.getCooldownTime(), targetWorld.getName());
         if (remainingSec > 0L) {
            net.kyori.adventure.text.Component cooldownMsg = Lang.of(this.configManager.getLang().getString("chat.cooldown").replace("%time%", String.valueOf(remainingSec)));
            p.sendMessage(cooldownMsg);
            p.sendActionBar(cooldownMsg);
            return true;
         }
      }

      if (rtp.isPreventSpam() && searchingPlayers.contains(p.getUniqueId())) {
         return true;
      }

      if (request.isMovetimer()) {
         this.startCountdown(p, s, request);
      } else {
         this.performTeleport(p, s, request);
      }

      return true;
   }


   void startCountdown(Player p, CommandSender s, SafeLocationRequest request) {
      // Rate-limit: silent, max 1 reset per 10 ticks
      long currentTick = p.getWorld().getFullTime();
      Long lastTick = lastCommandTick.get(p.getUniqueId());
      long diff = lastTick != null ? currentTick - lastTick : 10;
      if (diff < 0) diff = 10;
      if (diff < 10) return;
      lastCommandTick.put(p.getUniqueId(), currentTick);

      // Bump generation — old countdown steps will see a stale generation and stop themselves
      int gen = countdownGeneration.merge(p.getUniqueId(), 1, Integer::sum);
      inCountdown.add(p.getUniqueId());

      int initialX = p.getLocation().getBlockX();
      int initialY = p.getLocation().getBlockY();
      int initialZ = p.getLocation().getBlockZ();
      int duration = request.getMovetimerTime();
      p.sendActionBar(Lang.of(this.configManager.getLang().getString("chat.movetimer.remaining").replace("%time%", "" + duration)));

      this.scheduleNextCountdownStep(p, s, request, initialX, initialY, initialZ, duration - 1, gen);
   }

   private void scheduleNextCountdownStep(Player p, CommandSender s, SafeLocationRequest request, int x, int y, int z, int secondsRemaining, int gen) {
      scheduler.schedule(() -> Bukkit.getScheduler().runTask(this.plugin, () -> {
            // Stale generation → a newer countdown has replaced us, stop silently
            if (countdownGeneration.getOrDefault(p.getUniqueId(), 0) != gen) return;

            if (!p.isOnline()) {
               inCountdown.remove(p.getUniqueId());
               countdownGeneration.remove(p.getUniqueId());
            } else if (p.getLocation().getBlockX() == x && p.getLocation().getBlockY() == y && p.getLocation().getBlockZ() == z) {
               if (secondsRemaining < 1) {
                  inCountdown.remove(p.getUniqueId());
                  countdownGeneration.remove(p.getUniqueId());
                  this.performTeleport(p, s, request);
               } else {
                  p.sendActionBar(Lang.of(this.configManager.getLang().getString("chat.movetimer.remaining").replace("%time%", "" + secondsRemaining)));
                  String tickSound = request.getSoundProfile().movetimerTick();
                  if (tickSound != null && !tickSound.isBlank() && !tickSound.equalsIgnoreCase("false")) {
                     try {
                        { Sound _snd = Registry.SOUNDS.get(NamespacedKey.minecraft(tickSound.toLowerCase())); if (_snd != null) p.playSound(p.getLocation(), _snd, 1.0F, request.getSoundProfile().movetimerTickPitch()); }
                     } catch (IllegalArgumentException e) {
                        this.logger.warning("Invalid sound name for movetimer sound-tick: " + tickSound);
                     }
                  }
                  this.scheduleNextCountdownStep(p, s, request, x, y, z, secondsRemaining - 1, gen);
               }
            } else {
               net.kyori.adventure.text.Component cancelMsg = Lang.get(this.configManager.getLang(), "chat.movetimer.cancelled");
               p.sendMessage(cancelMsg);
               p.sendActionBar(cancelMsg);
               inCountdown.remove(p.getUniqueId());
               countdownGeneration.remove(p.getUniqueId());
               SoundProfile spMov = request.getSoundProfile();
               if (spMov.movetimerCanceled() != null && !spMov.movetimerCanceled().isBlank() && !spMov.movetimerCanceled().equalsIgnoreCase("false")) {
                  try {
                     Sound sound = Registry.SOUNDS.get(NamespacedKey.minecraft(spMov.movetimerCanceled().toLowerCase()));
                  if (sound == null) { return; }
                     float pitchMov = spMov.movetimerCanceledPitch();
                     Bukkit.getScheduler().runTaskLater(this.plugin, () -> p.playSound(p.getLocation(), sound, 1.0F, pitchMov), 1L);
                  } catch (IllegalArgumentException e) {
                     this.logger.warning("Invalid sound name for movetimer.sound-canceled: " + spMov.movetimerCanceled());
                  }
               }
            }
         }), 1L, TimeUnit.SECONDS);
   }

   void performTeleport(Player p, CommandSender s, SafeLocationRequest request) {
      DonutRTP rtp = DonutRTP.getInstance();
      if (rtp.isPreventSpam()) {
         searchingPlayers.add(p.getUniqueId());
      }

      CompletableFuture<SafeLocation> future = GetSafeRTP.getSafeRtpLocationFromConfigAsync(request, this.plugin);


      future.thenAccept((safeLocation) -> {
         if (safeLocation == null) {
            if (rtp.isPreventSpam()) {
               searchingPlayers.remove(p.getUniqueId());
            }
            s.sendMessage(Lang.get(this.configManager.getLang(), "error"));
         } else if (!p.isOnline()) {
            if (rtp.isPreventSpam()) {
               searchingPlayers.remove(p.getUniqueId());
            }
         } else {
            if (request.isCooldown()) {
               rtp.getCooldownManager().setCooldown(p, request.getTargetWorld().getName());
            }

            if (rtp.isForceCloseInventories()) {
               Bukkit.getScheduler().runTask(this.plugin, () -> p.closeInventory());
            }

            if (rtp.isPaper()) {
               p.teleportAsync(safeLocation.location()).thenRun(() ->
                     Bukkit.getScheduler().runTask(this.plugin, () ->
                           sendEndFeedback(request, p, this.logger, this.configManager.getLang(), safeLocation)));
            } else {
               Bukkit.getScheduler().runTask(this.plugin, () -> {
                  p.teleport(safeLocation.location());
                  sendEndFeedback(request, p, this.logger, this.configManager.getLang(), safeLocation);
               });
            }
            if (rtp.isPreventSpam()) {
               searchingPlayers.remove(p.getUniqueId());
            }


         }
      }).exceptionally((ex) -> {
         if (rtp.isPreventSpam()) {
            searchingPlayers.remove(p.getUniqueId());
         }
         s.sendMessage(Lang.get(this.configManager.getLang(), "error"));
         this.logger.severe("An error occurred while finding a safe location: " + ex.getMessage());
         ex.printStackTrace();
         return null;
      });
   }

   @Nullable
   public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      if (args.length == 1 && sender.hasPermission("donutrtp.rtp")) {
         List<String> suggestions = new ArrayList<>();
         for (World w : Bukkit.getWorlds()) {
            String name = w.getName();
            if (sender.hasPermission("donutrtp.world.*") || sender.hasPermission("donutrtp.world." + name)) {
               suggestions.add(name);
            }
         }
         String prefix = args[0].toLowerCase();
         return suggestions.stream()
               .filter(s -> s.toLowerCase().startsWith(prefix))
               .toList();
      }
      return List.of();
   }
}
