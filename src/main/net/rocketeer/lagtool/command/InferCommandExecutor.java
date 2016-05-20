package net.rocketeer.lagtool.command;

import net.rocketeer.lagtool.profile.LagInference;
import net.rocketeer.lagtool.profile.LagProfile;
import net.rocketeer.lagtool.profile.LagProfileStore;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InferCommandExecutor implements CommandExecutor {
  private final JavaPlugin plugin;
  private final LagProfileStore store;
  private volatile Lock lock = new ReentrantLock();
  private static final Pattern limitRegex = Pattern.compile("^l:(\\d+)$");

  public InferCommandExecutor(JavaPlugin plugin, LagProfileStore store) {
    this.plugin = plugin;
    this.store = store;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
    if (args.length == 0)
      return false;
    List<Integer> profiles = new LinkedList<>();
    int limit = 10;
    try {
      for (String arg : args) {
        Matcher matcher = limitRegex.matcher(arg);
        if (matcher.matches()) {
          limit = Integer.parseInt(matcher.group(1));
          continue;
        }
        profiles.add(Integer.parseInt(arg));
      }
    } catch (Exception e) {
      return false;
    }

    final int finalLimit = limit;
    Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
      lock.lock();
      try {
        LagInference inference = this.store.infer(profiles);
        Bukkit.getScheduler().runTask(plugin, () ->{
          Player p = (Player) sender;
          List<LagInference.Cluster> clusters = inference.worstAscending(LagInference.Metric.LOWER_CI, p.getWorld());
          clusters = clusters.subList(Math.max(0, clusters.size() - finalLimit), clusters.size());
          for (LagInference.Cluster cluster : clusters)
            p.sendMessage("Centroid: " + Arrays.toString(cluster.centroid()) + ", TPS: [" +
                Math.round(cluster.ci95()[0] * 100) / 100.0 + ", " + Math.round(cluster.ci95()[1] * 100) / 100.0 + "]");
          this.store.updatePlayerState(p, inference);
        });
      } finally {
        lock.unlock();
      }
    });

    return true;
  }
}
