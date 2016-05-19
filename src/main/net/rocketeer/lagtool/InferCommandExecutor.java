package net.rocketeer.lagtool;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class InferCommandExecutor implements Observer, CommandExecutor {
  List<LagProfile> lagProfiles = new ArrayList<>();

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

    LagInference inference = LagInference.infer(profiles);
    Player p = (Player) sender;
    inference.clustersOf(p.getWorld()).forEach(cluster -> p.sendMessage(Arrays.toString(cluster.centroid())));
    return true;
  }

  @Override
  public void update(Observable o, Object arg) {
    assert(arg instanceof LagProfile);
    LagProfile profile = (LagProfile) arg;
    this.lagProfiles.add(profile);
  }
}
