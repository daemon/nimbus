package net.rocketeer.lagtool.profile;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LagProfileStore implements Observer {
  private final JavaPlugin plugin;
  private final Lock lock = new ReentrantLock();
  private Map<List<Integer>, LagInference> cache = new HashMap<>();
  private List<LagProfile> profiles = new ArrayList<>();
  private Map<Player, LagInference> playerStates = new HashMap<>();

  public LagProfileStore(JavaPlugin plugin) {
    this.plugin = plugin;
  }

  public void updatePlayerState(Player player, LagInference inference) {
    this.playerStates.put(player, inference);
  }

  @Override
  public void update(Observable o, Object arg) {
    profiles.add((LagProfile) arg);
  }

  public LagInference findByPlayer(Player player) {
    return this.playerStates.get(player);
  }

  public LagInference infer(List<Integer> profileIndices) {
    if (cache.containsKey(profileIndices))
      return cache.get(profileIndices);
    List<LagProfile> profiles = new LinkedList<>();
    for (Integer index : profileIndices)
      profiles.add(this.profiles.get(index));
    LagInference inference = LagInference.infer(profiles);
    this.cache.put(profileIndices, inference);
    return inference;
  }
}

