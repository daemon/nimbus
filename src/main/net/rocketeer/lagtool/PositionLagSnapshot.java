package net.rocketeer.lagtool;

public class PositionLagSnapshot {
  public final int x;
  public final int z;
  public  final double tps;

  public PositionLagSnapshot(int x, int z, double tps) {
    this.x = x;
    this.z = z;
    this.tps = tps;
  }

  public static class Getter implements KdTree.PointGetter<PositionLagSnapshot> {
    @Override
    public int[] point(PositionLagSnapshot snapshot) {
      return new int[] { snapshot.x, snapshot.z };
    }
  }
}
