package net.rocketeer.lagtool;

import net.rocketeer.lagtool.command.CollectCommandExecutor;
import net.rocketeer.lagtool.command.InferCommandExecutor;
import net.rocketeer.lagtool.command.ViewCommandExecutor;
import net.rocketeer.lagtool.profile.LagProfileStore;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class LagToolPlugin extends JavaPlugin {
  @Override
  public void onEnable() {
    this.saveDefaultConfig();
    Config config = new Config();
    LagProfileStore store = new LagProfileStore(this);
    InferCommandExecutor inferCommand = new InferCommandExecutor(this, store);
    CollectCommandExecutor collectCommand = new CollectCommandExecutor(this, config);
    collectCommand.addObserver(store);
    this.getCommand("ltcollect").setExecutor(collectCommand);
    this.getCommand("ltinfer").setExecutor(inferCommand);
    this.getCommand("ltview").setExecutor(new ViewCommandExecutor(this, store));
  }

  @Override
  public void onDisable() {

  }

  public class Config {
    private int viewDistance;

    Config() {
      FileConfiguration fconf = getConfig();
      this.viewDistance = fconf.getInt("view-distance");
    }

    public void reload() {
      FileConfiguration fconf = getConfig();
      this.viewDistance = fconf.getInt("view-distance");
    }

    public int viewDistance() {
      return this.viewDistance;
    }
  }
}
