package net.rocketeer.lagtool;

import org.bukkit.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LagReport {
  private final static int epsilon = 16 * 8;
  private final long minPoints;
  private LagReport(Map<World, KdTree<PositionLagSnapshot>> worldKdTrees, long durationTicks) {
    this.minPoints = Math.max(durationTicks / 20 / 3, 600);
    worldKdTrees.forEach((world, kdTree) -> {
      Map<int[], Integer> pointClusterTable = new HashMap<>();
      int maxCluster = 0;
      for (PositionLagSnapshot ss : kdTree.toList()) {
        int[] lowerBound = new int[]{ ss.x - epsilon, ss.z - epsilon };
        int[] upperBound = new int[]{ ss.x + epsilon, ss.z + epsilon };
        List<PositionLagSnapshot> withinRange = kdTree.range(lowerBound, upperBound);
        List<Integer> clusters = this.clusters(withinRange);
        if (withinRange.size() > this.minPoints && clusters.isEmpty()) {
          pointClusterTable.put(new int[] {ss.x, ss.z}, maxCluster);
          for (PositionLagSnapshot ss2 : withinRange)
            pointClusterTable.put(new int[] {ss2.x, ss2.z}, maxCluster);
          ++maxCluster;
        } else if (withinRange.size() > this.minPoints) {

        }
      }
    });
  }


}
