package com.macxgaming.hubmc;

import java.util.ArrayList;
import java.util.logging.Level;

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
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.FieldAccessException;

public class hubmc extends JavaPlugin implements Listener {
	private ArrayList<String> usingClock;
	ProtocolManager protocolManager;
	
	public void onEnable() {
	  Bukkit.getPluginManager().registerEvents(this, this);
	  if (getConfig().getBoolean("magicclock")) { this.usingClock = new ArrayList<String>(); }
	  getConfig().options().copyDefaults(true);
	  saveConfig();
	  rainCheck();
	  /***** BLOCK TAB *****/
	  if (getConfig().getBoolean("notab")) {
		  this.protocolManager = ProtocolLibrary.getProtocolManager();
		    this.protocolManager.addPacketListener(new PacketAdapter(this, ListenerPriority.NORMAL, new PacketType[] { PacketType.Play.Client.TAB_COMPLETE })
		    {
		      public void onPacketReceiving(PacketEvent event)
		      {
		        if (event.getPacketType() == PacketType.Play.Client.TAB_COMPLETE) {
		          try
		          {
		            if (event.getPlayer().hasPermission("hubmc.commandtab.bypass")) {
		              return;
		            }
		            PacketContainer packet = event.getPacket();
		            String message = ((String)packet.getSpecificModifier(String.class).read(0)).toLowerCase();
		            if (((message.startsWith("/")) && (!message.contains(" "))) || ((message.startsWith("/ver")) && (!message.contains("  "))) || ((message.startsWith("/version")) && (!message.contains("  "))) || ((message.startsWith("/?")) && (!message.contains("  "))) || ((message.startsWith("/about")) && (!message.contains("  "))) || ((message.startsWith("/help")) && (!message.contains("  ")))) {
		              event.setCancelled(true);
		            }
		          }
		          catch (FieldAccessException e)
		          {
		            getLogger().log(Level.SEVERE, "Couldn't access field.", e);
		          }
		        }
		      }
		    });
	  }
	}
	/***** CLEAR INV ON QUIT *****/
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		player.getInventory().clear();
	}
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		/***** FORCE SPAWN *****/
		if (getConfig().getBoolean("forcespawn") && (!e.getPlayer().hasPermission("hubmc.fs.no"))) {
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
		/***** MAGIC CLOCK *****/
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
		if(cmd.getName().equalsIgnoreCase("hreload") && (p.getPlayer().hasPermission("hubmc.admin"))) {
				saveConfig();
				reloadConfig();
				p.sendMessage(ChatColor.GREEN + "Reloaded the config.yml");
				getLogger().info("Reloaded the config.yml");
		}else{
			p.sendMessage(getConfig().getString("no-perm-message").replaceAll("&", "§"));
		}
		return true;
	}
	/***** BLOCKED CMD *****/
	@EventHandler(priority=EventPriority.HIGHEST)
	public void onCommand(PlayerCommandPreprocessEvent event) {
		if((getConfig().getBoolean("blocked-cmd")) && (!event.getPlayer().hasPermission("hubmc.block.override")) && (!getConfig().getBoolean("block-all-commands"))) {
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
		/***** BLOCKED ALL CMD *****/
		if((getConfig().getBoolean("block-all-commands")) && (!event.getPlayer().hasPermission("hubmc.block.override"))) {
			Player p = event.getPlayer();
			p.sendMessage(getConfig().getString("no-perm-message").replaceAll("&", "§"));
			event.setCancelled(true);
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
	@EventHandler(priority=EventPriority.HIGH)
	  public void onWeatherChange(WeatherChangeEvent e)
	  {
	    final World w = e.getWorld();
	    if ((!getConfig().getBoolean("rain." + w.getName())) && 
	      (!w.hasStorm())) {
	      e.setCancelled(true);
	    }
	    Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable()
	    {
	      public void run()
	      {
	        try
	        {
	          if ((!getConfig().getBoolean("rain." + w.getName())) && 
	            (w.hasStorm())) {
	            w.setStorm(false);
	          }
	        }
	        catch (Exception localException) {}
	      }
	    }, 5L);
	  }
	private void rainCheck()
	  {
	    for (World w : getServer().getWorlds()) {
	      if (!getConfig().getBoolean("rain." + w.getName())) {
	        if (w.hasStorm()) {
	          w.setStorm(false);
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
