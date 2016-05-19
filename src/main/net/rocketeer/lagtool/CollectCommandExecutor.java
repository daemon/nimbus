package net.rocketeer.lagtool;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class CollectCommandExecutor extends Observable implements CommandExecutor
{
  private final LagToolPlugin.Config config;
  private final TpsMonitor monitor;
  private int monCount = 0;
  private final JavaPlugin plugin;

  public CollectCommandExecutor(JavaPlugin plugin, LagToolPlugin.Config config) {
    this.config = config;
    this.monitor = new TpsMonitor(plugin);
    this.plugin = plugin;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
  {
    if (args.length < 1)
      return false;
    long durationTicks;
    try {
      durationTicks = (long) (Double.parseDouble(args[0]) * 60 * 20);
    } catch (NumberFormatException e) {
      return false;
    }

    if (this.monCount == 0)
      this.monitor.start();
    ++this.monCount;
    final Map<World, List<PositionLagSnapshot>> snapshots = new HashMap<>();
    sender.sendMessage("Starting profile...");
    final BukkitTask task = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
      double tps = this.monitor.tps();
      for (Player p : Bukkit.getOnlinePlayers()) {
        if (!snapshots.containsKey(p.getWorld()))
          snapshots.put(p.getWorld(), new LinkedList<>());
        int x = p.getLocation().getBlockX();
        int z = p.getLocation().getBlockZ();
        snapshots.get(p.getWorld()).add(new PositionLagSnapshot(x, z, tps));
      }
    }, 0, 20);
    final Map<World, KdTree<PositionLagSnapshot>> kdtrees = new HashMap<>();
    Bukkit.getScheduler().runTaskLaterAsynchronously(this.plugin, () -> {
      task.cancel();
      snapshots.forEach((world, snaps) -> {
        KdTree<PositionLagSnapshot> kdtree = new KdTree<>(snaps, new PositionLagSnapshot.Getter());
        kdtrees.put(world, kdtree);
      });
      --this.monCount; // TODO sync
      if (this.monCount == 0)
        this.monitor.stop();
      setChanged();
      notifyObservers(new LagProfile(durationTicks, kdtrees));
      Bukkit.getScheduler().runTask(this.plugin, () -> sender.sendMessage("Completed profile."));
    }, durationTicks);
    return true;
  }
}
