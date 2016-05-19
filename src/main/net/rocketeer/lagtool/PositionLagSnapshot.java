package net.rocketeer.lagtool;

import java.util.Arrays;

public class PositionLagSnapshot {
  public final int x;
  public final int z;
  public final double tps;
  private int clusterNo = -1;

  public PositionLagSnapshot(int x, int z, double tps) {
    this.x = x;
    this.z = z;
    this.tps = tps;
  }

  public boolean partOfCluster() {
    return this.clusterNo != -1;
  }

  public int clusterNo() {
    return this.clusterNo;
  }

  public void setClusterNo(int clusterNo) {
    this.clusterNo = clusterNo;
  }

  public static class Getter implements KdTree.PointGetter<PositionLagSnapshot> {
    @Override
    public int[] point(PositionLagSnapshot snapshot) {
      return new int[] { snapshot.x, snapshot.z };
    }
  }
}
