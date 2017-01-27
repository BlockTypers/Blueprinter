package com.blocktyper.blueprinter.listeners;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import com.blocktyper.blueprinter.BlueprinterPlugin;
import com.blocktyper.blueprinter.BuildException;
import com.blocktyper.blueprinter.Layout;
import com.blocktyper.blueprinter.LocalizedMessageEnum;

public class PlaceLayoutItemListener extends LayoutBaseListener {

	public PlaceLayoutItemListener(BlueprinterPlugin plugin) {
		super(plugin);
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
			String improperOrientation = plugin.getLocalizedMessage(LocalizedMessageEnum.IMPROPER_ORIENTATION.getKey(),
					event.getPlayer());
			event.getPlayer().sendMessage(improperOrientation);
			event.setCancelled(true);
			return;
		}

		try {
			validateMaterialAmounts(layout, event.getPlayer());
			buildStructure(true, stallOrientation, location.clone(), layout);
			buildStructure(false, stallOrientation, location.clone(), layout);
			spendMaterialsInBag(layout, event.getPlayer());
		} catch (BuildException e) {
			e.sendMessages(event.getPlayer(), plugin);
			event.setCancelled(true);
		}
	}

	private void buildStructure(boolean isWaterAndAirTest, PlacementOrientation placementOrientation, Location location,
			Layout layout) throws BuildException {

		double triggerBlockX = location.getX();
		double triggerBlockZ = location.getZ();

		boolean zAxis = placementOrientation.getOrientation() == PlacementOrientation.Z;

		shiftStartingPoint(layout, zAxis, location, placementOrientation);

		double startX = location.getX();
		double startZ = location.getZ();

		List<String> floorNumbers = new ArrayList<>(layout.getFloorNumbers());
		if (layout.isBuildDown()) {
			Collections.reverse(floorNumbers);
		}

		for (String floorNumber : floorNumbers) {

			for (String rowNumber : layout.getRowsNumberPerFloor().get(floorNumber)) {
				String row = layout.getFloorNumberRowNumberRowMap().get(floorNumber).get(rowNumber);

				if (row != null) {
					for (char mat : row.toCharArray()) {

						Material material = layout.getMatMap().get(mat + "");

						if (material == null) {
							nextBlock(zAxis, location, placementOrientation);
							continue;
						}

						if (location.getBlock() != null) {

							if (isWaterAndAirTest) {
								if (!layout.isAllowReplacement()
										&& (triggerBlockX != location.getX() || triggerBlockZ != location.getZ())) {
									if (location.getBlock().getType() != Material.AIR
											&& location.getBlock().getType() != Material.STATIONARY_WATER
											&& location.getBlock().getType() != null) {
										String nonAirOrStationaryWaterBlockDetected = LocalizedMessageEnum.NON_AIR_OR_STATIONARY_WATER_BLOCK_DETECTED
												.getKey();
										String coords = "({0},{1},{2})";
										coords = new MessageFormat(coords)
												.format(new Object[] { location.getBlockY() + "",
														location.getBlockY() + "", location.getBlockZ() + "" });
										throw new BuildException(nonAirOrStationaryWaterBlockDetected,
												new Object[] { coords });
									}
								}
							} else {
								location.getBlock().setType(material);
							}
						} else {
							String undefinedBlockDetected = LocalizedMessageEnum.UNDEFINED_BLOCK_DETECTED.getKey();
							String coords = "({0},{1},{2})";
							coords = new MessageFormat(coords).format(new Object[] { location.getBlockY() + "",
									location.getBlockY() + "", location.getBlockZ() + "" });
							throw new BuildException(undefinedBlockDetected, new Object[] { coords });
						}

						nextBlock(zAxis, location, placementOrientation);
					}
				}

				nextRow(zAxis, startZ, startX, location, placementOrientation);

			}

			nextFloor(zAxis, startZ, startX, location, layout.isBuildDown());
		}
	}

	private void shiftStartingPoint(Layout layout, boolean zAxis, Location location,
			PlacementOrientation placementOrientation) {
		String firstFloorNumber = layout.getFloorNumbers().get(0);
		String firstRowNumber = layout.getRowsNumberPerFloor().get(firstFloorNumber).get(0);
		String firstRow = layout.getFloorNumberRowNumberRowMap().get(firstFloorNumber).get(firstRowNumber);

		int shiftMaginitude = firstRow.length() / 2;
		int shift = shiftMaginitude * (placementOrientation.isPositive() ? -1 : 1);

		if (zAxis) {
			location.setZ(location.getZ() + shift);
		} else {
			location.setX(location.getX() + shift);
		}
	}

	private void nextBlock(boolean zAxis, Location location, PlacementOrientation placementOrientation) {
		if (zAxis) {
			location.setZ(location.getZ() + (placementOrientation.isPositive() ? 1 : -1));
		} else {
			location.setX(location.getX() + (placementOrientation.isPositive() ? 1 : -1));
		}
	}

	private void nextRow(boolean zAxis, double startZ, double startX, Location location,
			PlacementOrientation placementOrientation) {
		if (zAxis) {
			location.setZ(startZ);
			location.setX(location.getX() + (placementOrientation.isAway() ? 1 : -1));
		} else {
			location.setX(startX);
			location.setZ(location.getZ() + (placementOrientation.isAway() ? 1 : -1));
		}
	}

	private void nextFloor(boolean zAxis, double startZ, double startX, Location location, boolean isDown) {
		if (zAxis) {
			location.setX(startX);
		} else {
			location.setZ(startZ);
		}

		location.setY(location.getY() + (isDown ? -1 : 1));
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
