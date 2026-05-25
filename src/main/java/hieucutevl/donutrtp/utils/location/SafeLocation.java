package hieucutevl.donutrtp.utils.location;

import org.bukkit.Location;

public record SafeLocation(Location location, int tries) {
}
