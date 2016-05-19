package net.rocketeer.lagtool;

import org.bukkit.World;

import java.util.*;
import java.util.stream.Collectors;

public class LagInference {
  private final static int epsilon = 16 * 8;
  private final long minPoints;
  private final Map<World, ClusterGraph> worldClusterMap = new HashMap<>();
  private LagInference(Map<World, KdTree<PositionLagSnapshot>> worldKdTrees, long durationTicks) {
    this.minPoints = Math.max(durationTicks / 20 / 3, 600);
    worldKdTrees.forEach((world, kdTree) -> {
      Map<PositionLagSnapshot, List<PositionLagSnapshot>> adjList = new IdentityHashMap<>();
      int maxCluster = 0;
      for (PositionLagSnapshot ss : kdTree.toList()) {
        int[] lowerBound = new int[]{ ss.x - epsilon, ss.z - epsilon };
        int[] upperBound = new int[]{ ss.x + epsilon, ss.z + epsilon };
        List<PositionLagSnapshot> withinRange = kdTree.range(lowerBound, upperBound);
        Set<Integer> addedClusters = new HashSet<>();
        List<PositionLagSnapshot> neighbors = new LinkedList<>();
        if (neighbors.size() < minPoints)
          continue;
        adjList.put(ss, neighbors);
        for (PositionLagSnapshot ss2 : withinRange) {
          if (!adjList.containsKey(ss2))
            adjList.put(ss2, new LinkedList<>());
          List<PositionLagSnapshot> otherNeighbors = adjList.get(ss2);
          if (!ss2.partOfCluster()) {
            neighbors.add(ss2);
            otherNeighbors.add(ss);
          } else if (!addedClusters.contains(ss2.clusterNo())) {
            addedClusters.add(ss2.clusterNo());
            neighbors.add(ss2);
            otherNeighbors.add(ss);
            ss.setClusterNo(ss2.clusterNo());
          }
        }

        int clusterNo = maxCluster;
        if (!addedClusters.isEmpty())
          clusterNo = addedClusters.iterator().next();
        else if (!neighbors.isEmpty())
          ++maxCluster;
        for (PositionLagSnapshot neighbor : neighbors)
          neighbor.setClusterNo(clusterNo);
        ss.setClusterNo(clusterNo);
      }

      ClusterGraph clusterGraph = new ClusterGraph(adjList);
      this.worldClusterMap.put(world, clusterGraph);
    });
  }

  public static LagInference infer(List<LagProfile> profiles) {
    if (profiles.size() == 1)
      return new LagInference(profiles.get(0).worldSnapshots, profiles.get(0).durationTicks);
    Map<World, KdTree<PositionLagSnapshot>> aggregation = new HashMap<>();
    long durationTicks = 0;
    for (LagProfile profile : profiles) {
      profile.worldSnapshots.forEach(aggregation::put);
      durationTicks += profile.durationTicks;
    }
    return new LagInference(aggregation, durationTicks);
  }

  private class ClusterGraph {
    public final Map<PositionLagSnapshot, List<PositionLagSnapshot>> adjList;
    public ClusterGraph(Map<PositionLagSnapshot, List<PositionLagSnapshot>> adjList) {
      this.adjList = adjList;
    }

    public List<List<PositionLagSnapshot>> clusters() {
      List<List<PositionLagSnapshot>> clusters = new LinkedList<>();
      Set<PositionLagSnapshot> open = this.adjList.keySet();
      Set<PositionLagSnapshot> closed = new HashSet<>();

      Iterator<PositionLagSnapshot> openIt = open.iterator();
      while (!open.isEmpty()) {
        List<PositionLagSnapshot> fringe = new LinkedList<>();
        fringe.add(openIt.next());

        List<PositionLagSnapshot> cluster = new LinkedList<>();
        for (PositionLagSnapshot ss : fringe) {
          cluster.add(ss);
          closed.add(ss);
          List<PositionLagSnapshot> neighbors = this.adjList.get(ss);
          for (PositionLagSnapshot neighbor : neighbors)
            if (!closed.contains(neighbor))
              fringe.add(neighbor);
        }
        clusters.add(cluster);
      }
      return clusters;
    }
  }
}
