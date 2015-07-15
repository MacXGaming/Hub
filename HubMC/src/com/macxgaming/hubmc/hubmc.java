package com.macxgaming.hubmc;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public class hubmc extends JavaPlugin implements Listener {
	private ArrayList<String> usingClock;

	public void onEnable() {
	  Bukkit.getPluginManager().registerEvents(this, this);
	  if (getConfig().getBoolean("magicclock")) { this.usingClock = new ArrayList<String>(); }
	  getConfig().options().copyDefaults(true);
	  saveConfig();
	}
		 
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		if (getConfig().getBoolean("forcespawn")) {
			Player p = e.getPlayer();
			if (getConfig().getConfigurationSection("spawn") == null) {
				p.sendMessage(ChatColor.RED + "The spawn has not yet been set!");
			}else{
				World w = Bukkit.getServer().getWorld(getConfig().getString("spawn.world"));
				double x = getConfig().getDouble("spawn.x");
				double y = getConfig().getDouble("spawn.y");
				double z = getConfig().getDouble("spawn.z");
				float yaw = (float)getConfig().getDouble("spawn.yaw");
				float pitch = (float)getConfig().getDouble("spawn.pitch");
				p.teleport(new Location(w, x, y, z, yaw, pitch));
			}
		}
		if (getConfig().getBoolean("magicclock") && (e.getPlayer().hasPermission("hubmc.use"))) {
	        ItemStack magicClock = new ItemStack(Material.WATCH, 1);
	       
	        ItemMeta magicClockMeta = magicClock.getItemMeta();
	        magicClockMeta.setDisplayName(getConfig().getString("clockitemname").replaceAll("&", "§"));
	        magicClock.setItemMeta(magicClockMeta);
	       
	        e.getPlayer().getInventory().setItem(getConfig().getInt("clockslot"),magicClock);
	       
	        for (Player p : Bukkit.getServer().getOnlinePlayers()) {
	                if (p != e.getPlayer()) {
	                        if (usingClock.contains(p.getName())) {
	                                p.hidePlayer(e.getPlayer()); // If they are currently using the clock, hide the new player. 
	                        }
	                        else {
	                                p.showPlayer(e.getPlayer()); // Else, show the new player.
	                        }
	                }
	        }
		}
	}
	/***** COMMANDS *****/
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		Player p = (Player)sender;
		if (cmd.getName().equalsIgnoreCase("hsetspawn") && (p.hasPermission("hubmc.admin"))) {
			if(p.isOp()){
				getConfig().set("spawn.world", p.getLocation().getWorld().getName());
				getConfig().set("spawn.x", Double.valueOf(p.getLocation().getX()));
				getConfig().set("spawn.y", Double.valueOf(p.getLocation().getY()));
				getConfig().set("spawn.z", Double.valueOf(p.getLocation().getZ()));
				getConfig().set("spawn.yaw", Float.valueOf(p.getLocation().getYaw()));
				getConfig().set("spawn.pitch", Float.valueOf(p.getLocation().getPitch()));
				saveConfig();
				p.sendMessage(ChatColor.GREEN + "Spawn set!");
				getLogger().info("Hub Spawn set!");
			}else{
				p.sendMessage(getConfig().getString("no-perm-message").replaceAll("&", "§"));
			}
		}
		if(cmd.getName().equalsIgnoreCase("hreload") && (p.getPlayer().hasPermission("hubmc.admin"))) {
			if(p.isOp()){
				saveConfig();
				reloadConfig();
				p.sendMessage(ChatColor.GREEN + "Reloaded the config.yml");
				getLogger().info("Reloaded the config.yml");
				return true;
			}else{
				p.sendMessage(getConfig().getString("no-perm-message").replaceAll("&", "§"));
				return true;
			}
		}
		return true;
	}
	/***** BLOCKED CMD *****/
	@EventHandler(priority=EventPriority.HIGHEST)
	public void onCommand(PlayerCommandPreprocessEvent event) {
		if((!event.getPlayer().hasPermission("hubmc.block.override"))) {
			String command = event.getMessage();
			for (int i = 0; i < getConfig().getList("blocked-cmds").size(); i++) {
				String playercommand = (String)getConfig().getList("blocked-cmds").get(i);
				if (command.toUpperCase().contains("/" + playercommand.toUpperCase())) {
					Player p = event.getPlayer();
					p.sendMessage(getConfig().getString("no-perm-message").replaceAll("&", "§"));
					event.setCancelled(true);
				}
			}
		}
	}
	/***** NO INV MOVE *****/
	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if(event.getWhoClicked().hasPermission("hubmc.inv.move")){event.setCancelled(true);}
	}
	@EventHandler
	public void onInventoryDrag(InventoryDragEvent event) {
		if(event.getWhoClicked().hasPermission("hubmc.inv.move")){event.setCancelled(true);}
	}
	/***** NO DROP *****/
	@EventHandler
	public void onPlayerDropItem(PlayerDropItemEvent event) {
		if (getConfig().getBoolean("nodrop") && (event.getPlayer().hasPermission("hubmc.use"))) {
			event.setCancelled(true);
		}
	}
	/***** NO RAIN *****/
	@EventHandler(priority=EventPriority.HIGHEST)
	public void rain(WeatherChangeEvent e) {
		if ((getConfig().getBoolean("weather")) && (e.toWeatherState())) {
			List<String> worlds = getConfig().getStringList("worlds");
			for (String w : worlds) {
				World world = Bukkit.getServer().getWorld(w);
				if (e.getWorld().equals(world)) {
					e.setCancelled(true);
					world.setStorm(false);
				}
			}
		}
	}
	/***** MAGIC CLOCK *****/
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent e) {
		if (getConfig().getBoolean("magicclock") && (e.getPlayer().hasPermission("hubmc.use"))) {
			Player player = e.getPlayer();
			if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) { return; }
			if (e.getItem().getType() != Material.WATCH) { return; }
			if (
			!e.getItem().hasItemMeta() ||
			!e.getItem().getItemMeta().hasDisplayName() ||
			!e.getItem().getItemMeta().getDisplayName().equals(getConfig().getString("clockitemname").replaceAll("&", "§"))
			) { return; }			
			if (usingClock.contains(e.getPlayer().getName())) {
				usingClock.remove(e.getPlayer().getName());
				player.sendMessage(getConfig().getString("show-players-message").replaceAll("&", "§"));
				for (Player p : Bukkit.getServer().getOnlinePlayers()) {
					if (p != e.getPlayer()) {
						e.getPlayer().showPlayer(p);
					}
				}
			}else{
				usingClock.add(e.getPlayer().getName());
				player.sendMessage(getConfig().getString("hide-players-message").replaceAll("&", "§"));
				for (Player p : Bukkit.getServer().getOnlinePlayers()) {
					if (p != e.getPlayer() && (!e.getPlayer().hasPermission("hubmc.magic"))) {
						e.getPlayer().hidePlayer(p);
					}
				}
			}
		}
	}
	/***** JUMPPAD *****/
	@EventHandler
	  public void onPlayerMove(PlayerMoveEvent event) {
		if (getConfig().getBoolean("jumppads") && (event.getPlayer().hasPermission("hubmc.use"))) {
		    Player player = event.getPlayer();
		    Location playerLoc = player.getLocation();
		    int ID = playerLoc.getWorld().getBlockAt(playerLoc)
		      .getRelative(0, -1, 0).getTypeId();
		    int plate = playerLoc.getWorld().getBlockAt(playerLoc).getTypeId();
		    if (((player instanceof Player)) && 
		      (ID == getConfig().getInt("BottomBlockId")) && 
		      (plate == getConfig().getInt("JumpPadID")))
		    {
		      player.setVelocity(player.getLocation().getDirection()
		        .multiply(getConfig().getInt("VelocityMultiplier")));
		      player.setVelocity(new Vector(player.getVelocity().getX(), 
		        1.0D, player.getVelocity().getZ()));
		    }
		}
	  }
	/***** NO FALL DAMAGE *****/
	@EventHandler
	public void onEntityDamage(EntityDamageEvent event) {
		if (getConfig().getBoolean("no-fall-damage")) {
			if (!(event.getEntity() instanceof Player)) {
				return;
			}
			if ((event.getCause().equals(EntityDamageEvent.DamageCause.FALL))) {
				event.setCancelled(true);
			}
		}
	}
	/***** VOID *****/
	  @EventHandler
	  public void onMove(PlayerMoveEvent event) {
		  if (getConfig().getBoolean("void") && (event.getPlayer().hasPermission("hubmc.use"))) {
			  Player player = event.getPlayer();
			  if (player.getLocation().getY() < getConfig().getInt("voidY")){
				  if (getConfig().getConfigurationSection("spawn") == null) {
						player.sendMessage(ChatColor.RED + "The spawn has not yet been set!");
					}else{
						World w = Bukkit.getServer().getWorld(getConfig().getString("spawn.world"));
						double x = getConfig().getDouble("spawn.x");
						double y = getConfig().getDouble("spawn.y");
						double z = getConfig().getDouble("spawn.z");
						float yaw = (float)getConfig().getDouble("spawn.yaw");
						float pitch = (float)getConfig().getDouble("spawn.pitch");
						player.teleport(new Location(w, x, y, z, yaw, pitch));
					}
			  }
		  }
		  /***** DOUBLE JUMP *****/
		  if (getConfig().getBoolean("doublejump") && (event.getPlayer().hasPermission("hubmc.use")) && (event.getPlayer().getGameMode() != GameMode.CREATIVE)) {
			  if ((event.getPlayer().getGameMode() != GameMode.CREATIVE) && (event.getPlayer().getLocation().getBlock().getRelative(BlockFace.DOWN).getType() != Material.AIR)) {
		      event.getPlayer().setAllowFlight(true);
		  }
	    }
	  }
	  @EventHandler
	  public void onFly(PlayerToggleFlightEvent event)
	  {
		  if (getConfig().getBoolean("doublejump") && (event.getPlayer().hasPermission("hubmc.use")) && (event.getPlayer().getGameMode() != GameMode.CREATIVE)) {
		      Player player = event.getPlayer();
		      event.setCancelled(true);
		      player.setAllowFlight(false);
		      player.setFlying(false);
		      player.setVelocity(player.getLocation().getDirection().multiply(1.6D).setY(1.0D));
		  }
	  }
}
