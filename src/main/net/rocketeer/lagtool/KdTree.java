package net.rocketeer.lagtool;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class KdTree<T> {
  private final PointGetter<T> getter;
  private final int nd;
  private final Node root;

  /**
   * Creates a k-d tree using median of medians heuristic.
   * @param points the points
   * @param getter retriever of int[] point data for <code>points</code>
   */
  public KdTree(List<T> points, PointGetter<T> getter) {
    this.getter = getter;
    this.nd = getter.point(points.get(0)).length;
    this.root = this.construct(points, 0);
  }

  /**
   * In-order traversal to build list of all nodes.
   * @return the list of nodes
   */
  public List<T> toList() {
    List<T> list = new LinkedList<>();
    toList(this.root, list);
    return list;
  }

  public T root() {
    return this.root.data;
  }

  /**
   * In-order traversal to build list of all nodes.
   * @param root the current node
   * @param list the list to build
   */
  private void toList(Node root, List<T> list) {
    if (root.left != null)
      toList(root.left, list);
    list.add(root.data);
    if (root.right != null)
      toList(root.right, list);
  }

  /**
   * Constructs a k-d tree node from <code>points</code>.
   * @param points the points to build k-d tree from
   * @param dim the number of dimensions
   * @return the constructed node
   */
  private Node construct(List<T> points, int dim) {
    if (points.isEmpty())
      return null;
    int index = dim % this.nd;
    T medianHeuristic = this.medianOfMedians(points, index);
    Node root = new Node(medianHeuristic, this.getter.point(medianHeuristic));
    List<T> smaller = new LinkedList<>();
    List<T> larger = new LinkedList<>();
    for (T point : points) {
      if (point == medianHeuristic)
        continue;
      if (this.getter.point(point)[index] < this.getter.point(medianHeuristic)[index])
        smaller.add(point);
      else
        larger.add(point);
    }
    if (!smaller.isEmpty())
      root.left(this.construct(smaller, dim + 1));
    if (!larger.isEmpty())
      root.right(this.construct(larger, dim + 1));
    return root;
  }

  /**
   * Returns an estimated median from points using median of medians.
   * @param points the data points
   * @param dim the number of dimensions
   * @return the estimated median of points
   */
  private T medianOfMedians(List<T> points, int dim) {
    if (points.size() == 1)
      return points.get(0);
    List<List<T>> groups = new LinkedList<>();
    for (int i = 0; i < points.size();) {
      LinkedList<T> group = new LinkedList<>();
      groups.add(group);
      int oldi = i;
      while (i < oldi + 5 && i < points.size()) {
        group.add(points.get(i));
        ++i;
      }
    }

    List<T> medians = new LinkedList<>();
    groups.forEach(g -> {
      Collections.sort(g, (a, b) -> this.getter.point(a)[dim] - this.getter.point(b)[dim]);
      medians.add(g.get(g.size() / 2));
    });

    return medianOfMedians(medians, dim);
  }

  /**
   * Returns all nodes contained within hyperrectangle <code>[a, b]</code>.
   * @param a the lower bounded point such that a[x] <= b[x] for all x
   * @param b the upper bounded point such that b[x] >= a[x] for all x
   * @return the list of nodes satisfying the criteria
   */
  public List<T> range(int[] a, int[] b) {
    return this.range(a, b, this.root, 0);
  }

  private List<T> range(int[] a, int[] b, Node node, int dim) {
    int index = dim % this.nd;
    List<T> elements = new LinkedList<>();
    int[] currPoint = node.point;
    if (a[index] <= currPoint[index] && currPoint[index] <= b[index])
      elements.add(node.data);
    if (node.left != null && a[index] <= currPoint[index])
      elements.addAll(this.range(a, b, node.left, dim + 1));
    if (node.right != null && currPoint[index] <= b[index])
      elements.addAll(this.range(a, b, node.right, dim + 1));
    return elements;
  }

  /**
   * Retrieves a geometric representation from an object of type <code>T</code>.
   * @param <T> the class
   */
  @FunctionalInterface
  public interface PointGetter<T> {
    int[] point(T Object);
  }

  private class Node {
    private final T data;
    private final int[] point;
    private Node left;
    private Node right;

    Node(T data, int[] point) {
      this.data = data;
      this.point = point;
    }

    /**
     * Sets left node.
     * @param left the left node to set
     * @return the original node
     */
    Node left(Node left) {
      this.left = left;
      return this;
    }

    /**
     * Sets right node.
     * @param right the left node to set
     * @return the original node
     */
    Node right(Node right) {
      this.right = right;
      return this;
    }
  }
}
