package net.rocketeer.lagtool.command;

import net.rocketeer.lagtool.profile.LagInference;
import net.rocketeer.lagtool.profile.LagProfileStore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedList;
import java.util.List;

public class ViewCommandExecutor implements CommandExecutor {
  private final LagProfileStore store;
  private final JavaPlugin plugin;

  public ViewCommandExecutor(JavaPlugin plugin, LagProfileStore store) {
    this.store = store;
    this.plugin = plugin;
  }

  private static int distance(int[] a, int[] b) {
    return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]);
  }

  @Override
  public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
    if (!(commandSender instanceof Player)) {
      commandSender.sendMessage("You must be a player");
      return true;
    }

    Player player = (Player) commandSender;
    LagInference inference = this.store.findByPlayer(player);
    if (inference == null) {
      player.sendMessage("You don't have a current inference to view");
      return true;
    }

    World world = player.getWorld();
    int[] pos = {player.getLocation().getBlockX(), player.getLocation().getBlockZ()};
    Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
      List<LagInference.Cluster> clusters = inference.clustersOf(world);
      List<int[]> nearPositions = new LinkedList<>();
      for (LagInference.Cluster cluster : clusters) {
        int[] centroid = cluster.centroid();
        if (distance(centroid, pos) < 320) {
          int size = cluster.points().size();
          cluster.points().forEach(point -> {
            if (Math.random() > 100.0 / size)
              return;
            if (distance(new int[]{point.x, point.z}, pos) < 160)
              nearPositions.add(new int[]{point.x, point.y, point.z});
          });
        }
      }

      Bukkit.getScheduler().runTask(this.plugin, () -> {
        for (int[] point : nearPositions)
          player.sendBlockChange(new Location(world, point[0], point[1], point[2]), Material.GLOWSTONE, (byte) 0);
      });
    });
    return true;
  }
}
