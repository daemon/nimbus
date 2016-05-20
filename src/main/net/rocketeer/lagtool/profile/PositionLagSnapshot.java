package net.rocketeer.lagtool.profile;

import net.rocketeer.lagtool.KdTree;

import java.util.Arrays;

public class PositionLagSnapshot {
  public final int x;
  public final int y;
  public final int z;
  public final float tps;
  private int clusterNo = -1;

  public PositionLagSnapshot(int x, int y, int z, double tps) {
    this.x = x;
    this.y = y;
    this.z = z;
    this.tps = (float) tps;
  }

  public boolean partOfCluster() {
    return this.clusterNo != -1;
  }

  /**
   * The sub-cluster number; each cluster may have multiple sub-cluster numbers, but distinct clusters have distinct
   * sub-cluster number. Used internally for reducing spanning tree size.
   * @return the sub-cluster number
   */
  public int clusterNo() {
    return this.clusterNo;
  }

  public void resetClusterNo() {
    this.clusterNo = -1;
  }

  public void setClusterNo(int clusterNo) {
    this.clusterNo = clusterNo;
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(new float[] {x, z, tps});
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof PositionLagSnapshot))
      return false;
    PositionLagSnapshot o = (PositionLagSnapshot) other;
    return o.x == this.x && o.z == this.z && this.tps == o.tps;
  }

  public static class Getter implements KdTree.PointGetter<PositionLagSnapshot> {
    @Override
    public int[] point(PositionLagSnapshot snapshot) {
      return new int[] { snapshot.x, snapshot.z };
    }
  }
}
