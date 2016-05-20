package net.rocketeer.lagtool.command;

import net.rocketeer.lagtool.profile.LagInference;
import net.rocketeer.lagtool.profile.LagProfile;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class InferCommandExecutor implements Observer, CommandExecutor {
  private final JavaPlugin plugin;
  private List<LagProfile> lagProfiles = new ArrayList<>();
  private Map<List<Integer>, LagInference> cache = new HashMap<>();
  private volatile Lock lock = new ReentrantLock();

  public InferCommandExecutor(JavaPlugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
    if (args.length == 0)
      return false;
    List<LagProfile> profiles = new LinkedList<>();
    try {
      for (String arg : args)
        profiles.add(this.lagProfiles.get(Integer.parseInt(arg)));
    } catch (Exception e) {
      return false;
    }

    Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
      lock.lock();
      try {
        LagInference inference = LagInference.infer(profiles);
        Bukkit.getScheduler().runTask(plugin, () ->{
          Player p = (Player) sender;
          for (LagInference.Cluster cluster : inference.worstAscending(LagInference.Metric.LOWER_CI, p.getWorld()))
            p.sendMessage("Centroid: " + Arrays.toString(cluster.centroid()) + ", TPS: [" +
                Math.round(cluster.ci95()[0] * 100) / 100.0 + ", " + Math.round(cluster.ci95()[1] * 100) / 100.0 + "]");
        });
      } finally {
        lock.unlock();
      }
    });

    return true;
  }

  @Override
  public void update(Observable o, Object arg) {
    assert(arg instanceof LagProfile);
    LagProfile profile = (LagProfile) arg;
    this.lagProfiles.add(profile);
  }
}
