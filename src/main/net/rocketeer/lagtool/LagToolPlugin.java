package net.rocketeer.lagtool;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class LagToolPlugin extends JavaPlugin {
  @Override
  public void onEnable() {
    this.saveDefaultConfig();
    Config config = new Config();
    this.getCommand("ltcollect").setExecutor(new CollectCommandExecutor(this, config));
    this.getCommand("ltinfer").setExecutor(new InferCommandExecutor(config));
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
