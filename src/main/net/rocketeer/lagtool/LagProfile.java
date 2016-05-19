package net.rocketeer.lagtool;

import org.bukkit.World;

import java.util.Map;

public class LagProfile {
  public final long durationTicks;
  public final Map<World, KdTree<PositionLagSnapshot>> worldSnapshots;
  public LagProfile(long durationTicks, Map<World, KdTree<PositionLagSnapshot>> worldSnapshots) {
    this.durationTicks = durationTicks;
    this.worldSnapshots = worldSnapshots;
  }
}
