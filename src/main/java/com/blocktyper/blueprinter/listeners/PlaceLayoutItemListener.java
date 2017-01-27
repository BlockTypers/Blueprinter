package com.blocktyper.blueprinter.listeners;

import java.util.HashMap;

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

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) {
		if (event.getBlock() == null) {
			return;
		}

		ItemStack itemInHand = plugin.getPlayerHelper().getItemInHand(event.getPlayer());

		Layout layout = plugin.getLayout(itemInHand);

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
			validateMaterials(layout, event.getPlayer());
			event.getPlayer().sendMessage(ChatColor.GREEN + ".");
			buildStructure(true, stallOrientation, location.clone(), layout);
			event.getPlayer().sendMessage(ChatColor.GREEN + "..");
			buildStructure(false, stallOrientation, location.clone(), layout);
			event.getPlayer().sendMessage(ChatColor.GREEN + ":)");
			spendMaterialsInBag(layout, event.getPlayer());
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

	private void validateMaterials(Layout layout, Player player) throws BuildException {

		if (!layout.requireMats()) {
			return;
		}

		if (layout.isRequireMatsInBag()) {
			for (Material material : layout.getRequirements().keySet()) {
				int amountRequired = layout.getRequirements().get(material);
				boolean requirementMet = amountRequired < 1;

				if (player.getInventory() != null && player.getInventory().getContents() != null) {
					HashMap<Integer, ? extends ItemStack> materialsInBag = player.getInventory().all(material);
					if (materialsInBag != null) {
						int currentAmountFound = 0;
						for (Integer slot : materialsInBag.keySet()) {
							ItemStack itemOfCurrentType = materialsInBag.get(slot);
							if (Layout.itemIsSuitableForLoading(itemOfCurrentType)) {
								currentAmountFound += itemOfCurrentType.getAmount();
							}
							if (currentAmountFound >= amountRequired) {
								requirementMet = true;
								break;
							}
						}

					}
				}

				if (!requirementMet) {
					throw new BuildException("$");
				}
			}
		} else if (layout.isRequireMatsLoaded()) {
			for (Material material : layout.getRequirements().keySet()) {
				int amountRequired = layout.getRequirements().get(material);
				boolean requirementMet = amountRequired < 1;

				if (layout.getSupplies() != null && layout.getSupplies().get(material) != null) {
					requirementMet = layout.getSupplies().get(material) >= amountRequired;
				}

				if (!requirementMet) {
					throw new BuildException("$");
				}
			}
		}
		// throw new BuildException("Non Air/Stationary Water Block!");
	}

	private void spendMaterialsInBag(Layout layout, Player player) {

		if (!layout.isRequireMatsInBag()) {
			return;
		}

		for (Material material : layout.getRequirements().keySet()) {
			int amountRequired = layout.getRequirements().get(material);

			if (player.getInventory() != null && player.getInventory().getContents() != null) {
				HashMap<Integer, ? extends ItemStack> materialsInBag = player.getInventory().all(material);
				if (materialsInBag != null) {
					for (Integer slot : materialsInBag.keySet()) {
						ItemStack itemOfCurrentType = materialsInBag.get(slot);
						if (itemOfCurrentType.getItemMeta() == null
								|| (itemOfCurrentType.getItemMeta().getDisplayName() == null
										&& (itemOfCurrentType.getItemMeta().getLore() == null
												|| itemOfCurrentType.getItemMeta().getLore().isEmpty()))) {

							if (amountRequired >= itemOfCurrentType.getAmount()) {
								amountRequired -= itemOfCurrentType.getAmount();
								itemOfCurrentType.setAmount(0);
								player.getInventory().remove(itemOfCurrentType);
							} else {
								itemOfCurrentType.setAmount(itemOfCurrentType.getAmount() - amountRequired);
								amountRequired = 0;
							}
						}

						if (amountRequired < 1) {
							break;
						}
					}

				}
			}
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
