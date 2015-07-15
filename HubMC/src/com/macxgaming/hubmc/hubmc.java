package com.macxgaming.hubmc;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class hubmc
  extends JavaPlugin
  implements Listener
{
  public void onEnable()
  {
    saveDefaultConfig();
    Bukkit.getPluginManager().registerEvents(this, this);
  }
  
  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent e)
  {
    if (getConfig().getBoolean("forcespawn"))
    {
      Player p = e.getPlayer();
      if (getConfig().getConfigurationSection("spawn") == null)
      {
        p.sendMessage(ChatColor.RED + "The spawn has not yet been set!");
      }
      else
      {
        World w = Bukkit.getServer().getWorld(getConfig().getString("spawn.world"));
        double x = getConfig().getDouble("spawn.x");
        double y = getConfig().getDouble("spawn.y");
        double z = getConfig().getDouble("spawn.z");
        float yaw = (float)getConfig().getDouble("spawn.yaw");
        float pitch = (float)getConfig().getDouble("spawn.pitch");
        p.teleport(new Location(w, x, y, z, yaw, pitch));
      }
    }
  }
  
  public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args)
  {
    Player p = (Player)sender;
    if (cmd.getName().equalsIgnoreCase("hsetspawn"))
    {
      getConfig().set("spawn.world", p.getLocation().getWorld().getName());
      getConfig().set("spawn.x", Double.valueOf(p.getLocation().getX()));
      getConfig().set("spawn.y", Double.valueOf(p.getLocation().getY()));
      getConfig().set("spawn.z", Double.valueOf(p.getLocation().getZ()));
      getConfig().set("spawn.yaw", Float.valueOf(p.getLocation().getYaw()));
      getConfig().set("spawn.pitch", Float.valueOf(p.getLocation().getPitch()));
      saveConfig();
      p.sendMessage(ChatColor.GREEN + "Spawn set!");
      getLogger().info("Hub Spawn set!");
    }
    if (cmd.getName().equalsIgnoreCase("hreload"))
    {
      if (p.isOp())
      {
        saveConfig();
        reloadConfig();
        p.sendMessage(ChatColor.GREEN + "Reloaded the config.yml");
        getLogger().info("Reloaded the config.yml");
        return true;
      }
      p.sendMessage("&cYou dont have the permission");
      return true;
    }
    return true;
  }
  
  @EventHandler(priority=EventPriority.HIGHEST)
  public void onCommand(PlayerCommandPreprocessEvent event)
  {
    String command = event.getMessage();
    for (int i = 0; i < getConfig().getList("blocked-cmds").size(); i++)
    {
      String playercommand = (String)getConfig().getList("blocked-cmds").get(i);
      if (command.toUpperCase().contains("/" + playercommand.toUpperCase()))
      {
        Player p = event.getPlayer();
        p.sendMessage(getConfig().getString("blocked-cmd-message").replaceAll("&", "§"));
        event.setCancelled(true);
      }
    }
  }
  
  @EventHandler
  public void onPlayerDropItem(PlayerDropItemEvent event)
  {
    if (getConfig().getBoolean("nodrop")) {
      event.setCancelled(true);
    }
  }
  
  @EventHandler(priority=EventPriority.HIGHEST)
  public void rain(WeatherChangeEvent e)
  {
    if ((getConfig().getBoolean("weather")) && 
      (e.toWeatherState()))
    {
      List<String> worlds = getConfig().getStringList("worlds");
      for (String w : worlds)
      {
        World world = Bukkit.getServer().getWorld(w);
        if (e.getWorld().equals(world))
        {
          e.setCancelled(true);
          world.setStorm(false);
        }
      }
    }
  }
}
