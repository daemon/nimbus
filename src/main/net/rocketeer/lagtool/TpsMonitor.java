package net.rocketeer.lagtool;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Observable;

public class TpsMonitor extends Observable {
  private final JavaPlugin plugin;
  private volatile boolean running = false;
  private BukkitTask task;
  private double lastLoggedTps;

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
    long a = System.currentTimeMillis();
    this.task = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
      this.lastLoggedTps = 20.0 * 1000.0 / (System.currentTimeMillis() - a);
      this.notifyObservers(this.lastLoggedTps);
      this.setChanged();
    }, 0, 20);
  }

  public void stop() {
    this.task.cancel();
    this.running = false;
  }
}
