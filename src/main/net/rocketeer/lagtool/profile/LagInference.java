package net.rocketeer.lagtool.profile;

import net.rocketeer.lagtool.KdTree;
import org.bukkit.World;

import java.util.*;

public class LagInference {
  public enum Metric { LOWER_CI, UPPER_CI, MEAN }
  private final static int epsilon = 16 * 8;
  private final long minPoints;
  private final Map<World, ClusterGraph> worldClusterMap = new HashMap<>();

  /**
   * Computes and creates an inference object.
   * @param worldKdTrees the k-d trees for each world
   * @param durationTicks the duration of the profile
   */
  private LagInference(Map<World, KdTree<PositionLagSnapshot>> worldKdTrees, long durationTicks) {
    this.minPoints = Math.min(durationTicks / 20 / 3, 600);
    worldKdTrees.forEach((world, kdTree) -> {
      Map<PositionLagSnapshot, List<PositionLagSnapshot>> adjList = new IdentityHashMap<>();
      int maxCluster = 0;
      List<PositionLagSnapshot> kdTreeList = kdTree.toList();
      kdTreeList.forEach(PositionLagSnapshot::resetClusterNo);
      for (PositionLagSnapshot ss : kdTreeList) {
        int[] lowerBound = new int[]{ ss.x - epsilon, ss.z - epsilon };
        int[] upperBound = new int[]{ ss.x + epsilon, ss.z + epsilon };
        List<PositionLagSnapshot> withinRange = kdTree.range(lowerBound, upperBound);
        Set<Integer> addedClusters = new HashSet<>();
        if (ss.partOfCluster())
          addedClusters.add(ss.clusterNo());
        if (withinRange.size() < minPoints)
          continue;
        List<PositionLagSnapshot> neighbors = adjList.get(ss);
        if (neighbors == null) {
          neighbors = new LinkedList<>();
          adjList.put(ss, neighbors);
        }
        for (PositionLagSnapshot ss2 : withinRange) {
          if (ss2 == ss)
            continue;
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

  public List<Cluster> worstAscending(Metric metric, World world) {
    List<Cluster> snapshots = this.clustersOf(world);
    if (metric == Metric.LOWER_CI)
      Collections.sort(snapshots, (a, b) -> (int) (b.ci95()[0] * 100 - a.ci95()[0] * 100));
    else if (metric == Metric.UPPER_CI)
      Collections.sort(snapshots, (a, b) -> (int) (b.ci95()[1] * 100 - a.ci95()[1] * 100));
    else
      Collections.sort(snapshots, (a, b) -> (int) (b.mean * 100 - a.mean * 100));
    return snapshots;
  }

  /**
   * Builds an inference object from a list of lag profiles.
   * @param profiles the profiles
   * @return the inference object
   */
  public static LagInference infer(List<LagProfile> profiles) {
    Map<World, KdTree<PositionLagSnapshot>> kdTrees = new HashMap<>();
    Map<World, List<PositionLagSnapshot>> aggregation = new HashMap<>();
    long durationTicks = 0;
    for (LagProfile profile : profiles) {
      profile.worldSnapshots.forEach((world, snapshots) -> {
        if (!aggregation.containsKey(world))
          aggregation.put(world, new LinkedList<>());
        aggregation.get(world).addAll(snapshots);
      });
      durationTicks += profile.durationTicks;
    }

    aggregation.forEach((world, snapshots) -> kdTrees.put(world, new KdTree<>(snapshots, new PositionLagSnapshot.Getter())));
    return new LagInference(kdTrees, durationTicks);
  }

  /**
   * Returns the computed clusters in <code>world</code>.
   * @param world the world
   * @return clusters of data
   */
  public List<Cluster> clustersOf(World world) {
    ClusterGraph clusterGraph = this.worldClusterMap.get(world);
    if (clusterGraph == null)
      return new LinkedList<>();
    return clusterGraph.clusters();
  }

  /**
   * A cluster of spatially similar lag samples.
   */
  public static class Cluster {
    private List<PositionLagSnapshot> points;
    private final double mean;
    private final double variance;
    private final int[] centroid;
    private final double[] ci95;
    Cluster(List<PositionLagSnapshot> points) {
      this.points = points;
      double sum = 0;
      double mse = 0;
      int[] centroid = new int[] {0, 0};
      for (PositionLagSnapshot ss : points) {
        sum += ss.tps;
        centroid[0] += ss.x;
        centroid[1] += ss.z;
      }

      this.mean = sum / points.size();
      for (PositionLagSnapshot ss : points) {
        mse += (ss.tps - this.mean) * (ss.tps - this.mean);
      }
      this.variance = mse / points.size();
      centroid[0] /= points.size();
      centroid[1] /= points.size();
      this.centroid = centroid;
      double interval = 1.96 * Math.sqrt(variance / points.size());
      this.ci95 = new double[]{this.mean - interval, this.mean + interval};
    }

    /**
     * @return the mean of the cluster
     */
    public double mean() {
      return this.mean;
    }

    /**
     * @return the 95% confidence interval of the cluster
     */
    public double[] ci95() {
      return this.ci95;
    }

    /**
     * @return the centroid of the cluster
     */
    public int[] centroid() {
      return this.centroid;
    }

    public List<PositionLagSnapshot> points() {
      return this.points;
    }
  }

  /**
   * Represents a graph of clusters. Contains routines to split the graph into a forest (discrete clusters).
   */
  private static class ClusterGraph {
    public final Map<PositionLagSnapshot, List<PositionLagSnapshot>> adjList;
    public final List<Cluster> clusters;
    public ClusterGraph(Map<PositionLagSnapshot, List<PositionLagSnapshot>> adjList) {
      this.adjList = adjList;
      this.clusters = this.computeClusters();
    }

    /**
     * BFS on graph to determine discrete clusters.
     * @return clusters in this graph
     */
    private List<Cluster> computeClusters() {
      List<Cluster> clusters = new LinkedList<>();
      Map<PositionLagSnapshot, Object> open = new IdentityHashMap<>();
      this.adjList.keySet().forEach(elem -> open.put(elem, null));
      while (!open.isEmpty()) {
        Queue<PositionLagSnapshot> fringe = new LinkedList<>();
        List<PositionLagSnapshot> cluster = new LinkedList<>();
        fringe.add(open.keySet().iterator().next());
        while (!fringe.isEmpty()) {
          PositionLagSnapshot ss = fringe.poll();
          cluster.add(ss);
          open.remove(ss);
          List<PositionLagSnapshot> neighbors = this.adjList.get(ss);
          for (PositionLagSnapshot neighbor : neighbors)
            if (open.keySet().contains(neighbor))
              fringe.add(neighbor);
        }
        clusters.add(new Cluster(cluster));
      }
      return clusters;
    }

    public List<Cluster> clusters() {
      return this.clusters;
    }
  }
}
