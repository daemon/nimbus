package net.rocketeer.lagtool.profile;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Observable;

public class TpsMonitor extends Observable {
  private final JavaPlugin plugin;
  private volatile boolean running = false;
  private BukkitTask task;
  private double lastLoggedTps = 20;

  public TpsMonitor(JavaPlugin plugin) {
    this.plugin = plugin;
  }

  public double tps() {
    return this.lastLoggedTps;
  }

  public boolean running() {
    return this.running;
  }

  public synchronized void start() {
    if (this.running)
      return;
    this.running = true;
    final long[] a = {System.currentTimeMillis()};
    this.task = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
      long b = System.currentTimeMillis();
      this.lastLoggedTps = 1000.0 / ((b - a[0]) / 20.0);
      this.setChanged();
      this.notifyObservers(this.lastLoggedTps);
      a[0] = b;
    }, 20, 20);
  }

  public void stop() {
    this.task.cancel();
    this.running = false;
  }
}
