package com.blocktyper.blueprinter.listeners;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import com.blocktyper.blueprinter.BlueprinterPlugin;

public class TestListener implements Listener {

	private BlueprinterPlugin plugin;

	public TestListener(BlueprinterPlugin plugin) {
		this.plugin = plugin;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	private static class StallOrientation {
		static int X = 1;
		static int Z = 0;

		int orientation = -1;
		boolean positive;
		boolean away;
	}

	private static class BuildException extends Exception {
		private static final long serialVersionUID = 1L;

		public BuildException(String message) {
			super(message);
		}
	}

	private StallOrientation getStallOrientation(Player player, Location clickedLocation) {

		int playerX = player.getLocation().getBlockX();
		int playerZ = player.getLocation().getBlockZ();

		int blockX = clickedLocation.getBlockX();
		int blockZ = clickedLocation.getBlockZ();

		int dx = playerX - blockX;
		int dz = playerZ - blockZ;

		if ((dx == 0 && dz == 0) || (dx != 0 && dz != 0)) {
			return null;
		}

		StallOrientation stallOrientation = new StallOrientation();
		if (dz != 0) {
			stallOrientation.orientation = StallOrientation.X;
			stallOrientation.positive = dz > 0;
			stallOrientation.away = dz < 0;
		} else {
			stallOrientation.orientation = StallOrientation.Z;
			stallOrientation.positive = dx < 0;
			stallOrientation.away = dx < 0;
		}

		return stallOrientation;
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	public void onBlockPlace(BlockPlaceEvent event) {
		if (event.getBlock() == null) {
			return;
		}

		if (event.getBlock().getType() != Material.IRON_BLOCK) {
			plugin.debugInfo("Not an iron block");
			return;
		} else {
			plugin.debugInfo("Iron block");
		}

		Location location = event.getBlock().getLocation();

		List<String> symbols = plugin.getConfig().getStringList("layout.starter-house.mats.symbols");

		Map<String, Material> matMap = new HashMap<>();
		for (String symbol : symbols) {
			String mat = plugin.getConfig().getString("layout.starter-house.mats.definitions." + symbol);

			if (mat != null && !mat.isEmpty()) {
				matMap.put(symbol, Material.matchMaterial(mat));
			} else {
				plugin.warning("Material symbol was null or empty: " + symbol);
			}

		}

		StallOrientation stallOrientation = getStallOrientation(event.getPlayer(), event.getBlock().getLocation());

		if (stallOrientation == null) {
			event.getPlayer().sendMessage(ChatColor.RED + ":_(");
			event.setCancelled(true);
			return;
		}

		try {
			event.getPlayer().sendMessage(ChatColor.GREEN + ".");
			buildStructure(true, stallOrientation, location.clone(), matMap);
			event.getPlayer().sendMessage(ChatColor.GREEN + "..");
			buildStructure(false, stallOrientation, location.clone(), matMap);
			event.getPlayer().sendMessage(ChatColor.GREEN + ":)");
		} catch (BuildException e) {
			event.getPlayer().sendMessage(ChatColor.RED + ":(");
			event.getPlayer().sendMessage(ChatColor.RED + e.getMessage());
			event.setCancelled(true);
		}
	}

	private void buildStructure(boolean isWaterAndAirTest, StallOrientation stallOrientation, Location location,
			Map<String, Material> matMap) throws BuildException {
		
		double triggerBlockX = location.getX();
		double triggerBlockZ = location.getZ();

		List<String> floorNumbers = plugin.getConfig().getStringList("layout.starter-house.floors");
		String firstFloorNumber = floorNumbers.get(0);
		String firstRowNumber = plugin.getConfig()
				.getStringList("layout.starter-house.floor." + firstFloorNumber + ".rows").get(0);
		String firstRow = plugin.getConfig()
				.getString("layout.starter-house.floor." + firstFloorNumber + ".row." + firstRowNumber);

		int shiftMaginitude = firstRow.length() / 2;
		int shift = shiftMaginitude * (stallOrientation.positive ? -1 : 1);

		boolean zAxis = stallOrientation.orientation == StallOrientation.Z;

		if (zAxis) {
			location.setZ(location.getZ() + shift);
		} else {
			location.setX(location.getX() + shift);
		}

		double startX = location.getX();
		double startZ = location.getZ();

		for (String floorNumber : floorNumbers) {
			List<String> rowNumbers = plugin.getConfig()
					.getStringList("layout.starter-house.floor." + floorNumber + ".rows");

			for (String rowNumber : rowNumbers) {
				String row = plugin.getConfig()
						.getString("layout.starter-house.floor." + floorNumber + ".row." + rowNumber);


				if (row != null) {
					for (char mat : row.toCharArray()) {

						Material material = matMap.get(mat + "");

						if (material == null) {
							throw new BuildException("NULL MAT!!! " + mat);
						}

						if (location.getBlock() != null) {

							if (isWaterAndAirTest) {
								if(triggerBlockX != location.getX() || triggerBlockZ != location.getZ()){
									if (location.getBlock().getType() != Material.AIR 
											&& location.getBlock().getType() != Material.STATIONARY_WATER
											&& location.getBlock().getType() != null) {
										throw new BuildException("Non Air/Stationary Water Block!");
									}
								}
							} else {
								location.getBlock().setType(material);
							}
						} else {
							throw new BuildException("Null block at location: " + location.getBlockX() + ","
									+ location.getBlockY() + "," + location.getBlockZ());
						}

						if (zAxis) {
							location.setZ(location.getZ() + (stallOrientation.positive ? 1 : -1));
						} else {
							location.setX(location.getX() + (stallOrientation.positive ? 1 : -1));
						}
					}
				}

				if (zAxis) {
					location.setZ(startZ);
					location.setX(location.getX() + (stallOrientation.away ? 1 : -1));
				} else {
					location.setX(startX);
					location.setZ(location.getZ() + (stallOrientation.away ? 1 : -1));
				}

			}

			if (zAxis) {
				location.setX(startX);
			} else {
				location.setZ(startZ);
			}

			location.setY(location.getY() + 1);
		}
	}

}
