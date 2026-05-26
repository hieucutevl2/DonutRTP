package hieucutevl.donutrtp.utils.config;

/**
 * Immutable cache of all sound settings read from config.
 * Built once on plugin load/reload, shared across all RTP requests.
 */
public record SoundProfile(
      boolean enabled,
      String end,
      float  endPitch,
      String movetimerCanceled,
      float  movetimerCanceledPitch,
      String movetimerTick,
      float  movetimerTickPitch
) {
   public static final SoundProfile DEFAULT = new SoundProfile(
         true,
         "ENTITY_PLAYER_LEVELUP", 1.0f,
         "ENTITY_VILLAGER_NO", 1.0f,
         "UI_BUTTON_CLICK", 1.0f
   );
}
