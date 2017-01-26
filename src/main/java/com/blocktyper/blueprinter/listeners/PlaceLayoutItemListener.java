package com.blocktyper.blueprinter.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import com.blocktyper.blueprinter.BlueprinterPlugin;
import com.blocktyper.blueprinter.BuildException;
import com.blocktyper.blueprinter.Layout;
import com.blocktyper.v1_1_8.nbt.NBTItem;

public class PlaceLayoutItemListener implements Listener {

	private BlueprinterPlugin plugin;

	public PlaceLayoutItemListener(BlueprinterPlugin plugin) {
		this.plugin = plugin;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	private PlacementOrientation getPlacementOrientation(Player player, Location clickedLocation) {

		int playerX = player.getLocation().getBlockX();
		int playerZ = player.getLocation().getBlockZ();

		int blockX = clickedLocation.getBlockX();
		int blockZ = clickedLocation.getBlockZ();

		int dx = playerX - blockX;
		int dz = playerZ - blockZ;

		if ((dx == 0 && dz == 0) || (dx != 0 && dz != 0)) {
			return null;
		}

		PlacementOrientation stallOrientation = new PlacementOrientation();
		if (dz != 0) {
			stallOrientation.setOrientation(PlacementOrientation.X);
			stallOrientation.setPositive(dz > 0);
			stallOrientation.setAway(dz < 0);
		} else {
			stallOrientation.setOrientation(PlacementOrientation.Z);
			stallOrientation.setPositive(dx < 0);
			stallOrientation.setAway(dx < 0);
		}

		return stallOrientation;
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	public void onBlockPlace(BlockPlaceEvent event) {
		if (event.getBlock() == null) {
			return;
		}

		ItemStack itemInHand = plugin.getPlayerHelper().getItemInHand(event.getPlayer());
		
		if(itemInHand == null){
			return;
		}
		
		NBTItem nbtItem = new NBTItem(itemInHand);
		
		Layout layout = nbtItem.getObject(plugin.getLayoutKey(), Layout.class);
		
		if (layout == null) {
			plugin.debugInfo("No layout");
			return;
		}

		Location location = event.getBlock().getLocation();

		PlacementOrientation stallOrientation = getPlacementOrientation(event.getPlayer(),
				event.getBlock().getLocation());

		if (stallOrientation == null) {
			event.getPlayer().sendMessage(ChatColor.RED + ":_(");
			event.setCancelled(true);
			return;
		}

		try {
			event.getPlayer().sendMessage(ChatColor.GREEN + ".");
			buildStructure(true, stallOrientation, location.clone(), layout);
			event.getPlayer().sendMessage(ChatColor.GREEN + "..");
			buildStructure(false, stallOrientation, location.clone(), layout);
			event.getPlayer().sendMessage(ChatColor.GREEN + ":)");
		} catch (BuildException e) {
			event.getPlayer().sendMessage(ChatColor.RED + ":(");
			event.getPlayer().sendMessage(ChatColor.RED + e.getMessage());
			event.setCancelled(true);
		}
	}

	private void buildStructure(boolean isWaterAndAirTest, PlacementOrientation stallOrientation, Location location,
			Layout layout) throws BuildException {

		double triggerBlockX = location.getX();
		double triggerBlockZ = location.getZ();

		String firstFloorNumber = layout.getFloorNumbers().get(0);
		String firstRowNumber = layout.getRowsNumberPerFloor().get(firstFloorNumber).get(0);
		String firstRow = layout.getFloorNumberRowNumberRowMap().get(firstFloorNumber).get(firstRowNumber);

		int shiftMaginitude = firstRow.length() / 2;
		int shift = shiftMaginitude * (stallOrientation.isPositive() ? -1 : 1);

		boolean zAxis = stallOrientation.getOrientation() == PlacementOrientation.Z;

		if (zAxis) {
			location.setZ(location.getZ() + shift);
		} else {
			location.setX(location.getX() + shift);
		}

		double startX = location.getX();
		double startZ = location.getZ();

		for (String floorNumber : layout.getFloorNumbers()) {

			for (String rowNumber : layout.getRowsNumberPerFloor().get(floorNumber)) {
				String row = layout.getFloorNumberRowNumberRowMap().get(floorNumber).get(rowNumber);

				if (row != null) {
					for (char mat : row.toCharArray()) {

						Material material = layout.getMatMap().get(mat + "");

						if (material == null) {
							throw new BuildException("NULL MAT!!! " + mat);
						}

						if (location.getBlock() != null) {

							if (isWaterAndAirTest) {
								if (triggerBlockX != location.getX() || triggerBlockZ != location.getZ()) {
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
							location.setZ(location.getZ() + (stallOrientation.isPositive() ? 1 : -1));
						} else {
							location.setX(location.getX() + (stallOrientation.isPositive() ? 1 : -1));
						}
					}
				}

				if (zAxis) {
					location.setZ(startZ);
					location.setX(location.getX() + (stallOrientation.isAway() ? 1 : -1));
				} else {
					location.setX(startX);
					location.setZ(location.getZ() + (stallOrientation.isAway() ? 1 : -1));
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

	private static class PlacementOrientation {
		public static int X = 1;
		public static int Z = 0;

		private int orientation = -1;
		private boolean positive;
		private boolean away;

		public int getOrientation() {
			return orientation;
		}

		public void setOrientation(int orientation) {
			this.orientation = orientation;
		}

		public boolean isPositive() {
			return positive;
		}

		public void setPositive(boolean positive) {
			this.positive = positive;
		}

		public boolean isAway() {
			return away;
		}

		public void setAway(boolean away) {
			this.away = away;
		}

	}

}
