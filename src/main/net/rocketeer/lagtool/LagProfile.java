package net.rocketeer.lagtool;

import org.bukkit.World;

import java.util.List;
import java.util.Map;

public class LagProfile {
  public final long durationTicks;
  public final Map<World, List<PositionLagSnapshot>> worldSnapshots;
  public LagProfile(long durationTicks, Map<World, List<PositionLagSnapshot>> worldSnapshots) {
    this.durationTicks = durationTicks;
    this.worldSnapshots = worldSnapshots;
  }
}
