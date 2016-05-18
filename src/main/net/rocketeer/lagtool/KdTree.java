package net.rocketeer.lagtool;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class KdTree<T> {
  private final PointGetter<T> getter;
  private final int nd;
  private final Node root;

  public KdTree(List<T> points, PointGetter<T> getter) {
    this.getter = getter;
    this.nd = getter.point(points.get(0)).length;
    this.root = this.construct(points, 0);
  }

  public List<T> toList() {
    List<T> list = new LinkedList<>();
    toList(this.root, list);
    return list;
  }

  public T root() {
    return this.root.data;
  }

  private void toList(Node root, List<T> list) {
    list.add(root.data);
    if (root.left != null)
      toList(root.left, list);
    if (root.right != null)
      toList(root.right, list);
  }

  private Node construct(List<T> points, int dim) {
    int index = dim % this.nd;
    T medianHeuristic = this.medianOfMedians(points, index);
    Node root = new Node(medianHeuristic, this.getter.point(medianHeuristic));
    List<T> smaller = new LinkedList<>();
    List<T> larger = new LinkedList<>();
    for (T point : points)
      if (this.getter.point(point)[index] < this.getter.point(medianHeuristic)[index])
        smaller.add(point);
      else
        larger.add(point);
    if (!smaller.isEmpty())
      root.left(this.construct(smaller, dim + 1));
    if (!larger.isEmpty())
      root.right(this.construct(smaller, dim + 1));
    return root;
  }

  private T medianOfMedians(List<T> points, int dim) {
    if (points.size() == 1)
      return points.get(0);
    List<List<T>> groups = new LinkedList<>();
    for (int i = 0; i < points.size();) {
      LinkedList<T> group = new LinkedList<>();
      groups.add(group);
      while (i < i + 5 && i < points.size()) {
        group.add(points.get(i));
        ++i;
      }
    }
    List<T> medians = new LinkedList<>();
    groups.forEach(group -> {
      Collections.sort(group, (a, b) -> this.getter.point(a)[dim] - this.getter.point(b)[dim]);
      medians.add(group.get(group.size() / 2));
    });

    return medianOfMedians(medians, dim);
  }

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

    Node left(Node left) {
      this.left = left;
      return this;
    }

    Node right(Node right) {
      this.right = right;
      return this;
    }
  }
}
