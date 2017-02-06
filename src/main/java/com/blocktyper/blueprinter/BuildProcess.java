package com.blocktyper.blueprinter;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Furnace;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.blocktyper.blueprinter.data.BlockChange;
import com.blocktyper.blueprinter.data.ConstructionReceipt;
import com.blocktyper.v1_1_8.helpers.BlockDefinition;
import com.blocktyper.v1_1_8.helpers.ComplexMaterial;
import com.blocktyper.v1_1_8.helpers.Coord;
import com.blocktyper.v1_1_8.helpers.IClickedBlockHelper.PlacementOrientation;

public class BuildProcess {

	private transient BlueprinterPlugin plugin;
	private ConstructionReceipt constructionReciept;

	boolean initCalled = false;

	public BuildProcess(BlueprinterPlugin plugin, Layout layout) {
		this.plugin = plugin;
		this.constructionReciept = new ConstructionReceipt();
		this.constructionReciept.setLayout(layout);
	}

	public BuildProcess(BlueprinterPlugin plugin, ConstructionReceipt constructionReciept) {
		this.plugin = plugin;
		this.constructionReciept = constructionReciept;
	}

	public void init() {
		initCalled = true;
		constructionReciept.setSymbolMap(new HashMap<>());
	}

	private List<BlockDefinition> getChanges(boolean isTo) {
		List<BlockDefinition> changes = new ArrayList<>();
		for (BlockChange change : constructionReciept.getChanges()) {
			BlockDefinition newDefinition = new BlockDefinition();
			newDefinition.setCoord(change.getCoord());
			newDefinition.setComplexMaterial(isTo ? change.getTo() : change.getFrom());
			changes.add(newDefinition);
		}
		return changes;
	}

	@SuppressWarnings("deprecation")
	public ConstructionReceipt validateAndDoFirstBuild(HumanEntity player, Location location,
			BlockState replacedBlockState) throws BuildException {

		if (!initCalled) {
			throw new BuildException("init must be called before every call to validateAndDoFirstBuild()");
		}
		initCalled = false;
		
		constructionReciept.setLayout(constructionReciept.getLayout());
		constructionReciept.setX(location.getBlockX());
		constructionReciept.setY(location.getBlockZ());
		constructionReciept.setZ(location.getBlockY());
		constructionReciept.setPlayerX(player.getLocation().getBlockX());
		constructionReciept.setPlayerY(player.getLocation().getBlockY());
		constructionReciept.setPlayerZ(player.getLocation().getBlockZ());
		
		constructionReciept.setReplacedComplexMaterial(
				new ComplexMaterial(replacedBlockState.getType(), replacedBlockState.getRawData()));

		constructionReciept.setWorld(location.getWorld().getName());

		validateMaterialAmounts(constructionReciept.getLayout(), player);

		PlacementOrientation placementOrientation = validatePlacementOrientation(player.getLocation(), location);

		Map<Coord, Block> blocksMap = validateEnvironment(placementOrientation, location.clone(),
				constructionReciept.getLayout(), constructionReciept.getReplacedComplexMaterial());

		List<BlockDefinition> changes = getChanges(true);

		alterBlocks(changes, blocksMap);

		spendMaterialsInBag(constructionReciept.getLayout(), player);

		

		return constructionReciept;
	}

	public void restoreOriginalBlocks(World world) {
		alterBlocks(false, world, true, null);
	}

	public void applyChanges(World world, boolean alterToFromValues, Character symbol) {
		alterBlocks(true, world, alterToFromValues, symbol);
	}

	private void alterBlocks(List<BlockDefinition> changes, Map<Coord, Block> blockMap) {
		if (changes != null && blockMap != null) {
			for (BlockDefinition change : changes) {
				Coord coord = change.getCoord();
				if (blockMap.containsKey(coord) && blockMap.get(coord) != null) {
					Block block = blockMap.get(coord);
					setBlockType(change.getComplexMaterial(), block, true);
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	private void alterBlocks(boolean isTo, World world, boolean alterToFromValues, Character symbol) {
		if (world != null) {
			constructionReciept.setShowing(isTo);
			for (BlockChange change : constructionReciept.getChanges()) {
				
				if(symbol != null && !symbol.equals(change.getSymbol())){
					continue;
				}
				
				Coord coord = change.getCoord();

				Block block = world.getBlockAt(new Location(world, coord.getX(), coord.getY(), coord.getZ()));

				if (block == null) {
					continue;
				}

				if (alterToFromValues) {
					ComplexMaterial existingComplexMaterial = new ComplexMaterial(block.getType(), block.getData());
					ComplexMaterial expectedExistingComplexMaterial = isTo ? change.getFrom() : change.getTo();

					boolean existingMisMatch = !expectedExistingComplexMaterial.equals(existingComplexMaterial);

					if (existingMisMatch) {
						if (isTo) {
							change.setFrom(existingComplexMaterial);
						} else {
							change.setTo(existingComplexMaterial);
						}
					}
				}

				ComplexMaterial newMaterial = isTo ? change.getTo() : change.getFrom();
				setBlockType(newMaterial, block, isTo);
			}
		}
	}

	@SuppressWarnings("deprecation")
	private void setBlockType(ComplexMaterial complexMaterial, Block block, boolean isTo) {
		if (complexMaterial == null || block == null) {
			return;
		}

		Location dropLocation = new Location(block.getWorld(), constructionReciept.getPlayerX(),
				constructionReciept.getPlayerY(), constructionReciept.getPlayerZ());

		if (block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST) {
			Chest chest = (Chest) block.getState();
			Inventory inventory = chest.getBlockInventory();
			if (inventory != null && inventory.getContents() != null && inventory.getContents().length > 0) {
				for (ItemStack itemInChest : inventory.getContents()) {
					if (itemInChest == null || itemInChest.getType() == Material.AIR) {
						continue;
					}

					block.getWorld().dropItemNaturally(dropLocation, itemInChest);
				}
				inventory.setContents(new ItemStack[0]);
			}
		} else if (block.getType() == Material.FURNACE) {
			Furnace furnace = (Furnace) block.getState();
			if (furnace.getInventory() != null) {
				if (furnace.getInventory().getFuel() != null) {
					block.getWorld().dropItemNaturally(dropLocation, furnace.getInventory().getFuel());
					furnace.getInventory().setFuel(null);
				}
				if (furnace.getInventory().getSmelting() != null) {
					block.getWorld().dropItemNaturally(dropLocation, furnace.getInventory().getSmelting());
					furnace.getInventory().setSmelting(null);
				}
				if (furnace.getInventory().getResult() != null) {
					block.getWorld().dropItemNaturally(dropLocation, furnace.getInventory().getResult());
					furnace.getInventory().setResult(null);
				}
			}
		}

		block.setType(complexMaterial.getMaterial());
		if (complexMaterial.getData() != null && !complexMaterial.getData().equals(0)) {
			block.setData(complexMaterial.getData());
		}
	}

	@SuppressWarnings("deprecation")
	private Map<Coord, Block> validateEnvironment(PlacementOrientation placementOrientation, Location location,
			Layout layout, ComplexMaterial replacedComplexMaterial) throws BuildException {
		Map<Coord, Block> blocksMap = new HashMap<>();

		double triggerBlockX = location.getX();
		double triggerBlockY = location.getY();
		double triggerBlockZ = location.getZ();

		boolean zAxis = placementOrientation.getOrientation() == PlacementOrientation.Z;

		shiftStartingPoint(layout, zAxis, location, placementOrientation);

		double startX = location.getX();
		double startZ = location.getZ();

		List<String> layerNumbers = new ArrayList<>(layout.getLayerNumbers());
		if (layout.isBuildDown()) {
			Collections.reverse(layerNumbers);
		}

		for (String layerNumber : layerNumbers) {

			for (String rowNumber : layout.getRowsNumberPerLayer().get(layerNumber)) {
				String row = layout.getLayerNumberRowNumberRowMap().get(layerNumber).get(rowNumber);

				if (row != null) {
					for (char mat : row.toCharArray()) {

						Material material = layout.getMatMap().get(mat + "");

						if (material == null) {
							nextBlock(zAxis, location, placementOrientation);
							continue;
						}

						if (location.getBlock() != null) {

							boolean isTriggerBlock = triggerBlockX == location.getX()
									&& triggerBlockY == location.getY() && triggerBlockZ == location.getZ();

							if (!layout.isAllowReplacement() && !isTriggerBlock) {
								if (location.getBlock().getType() != Material.AIR
										&& location.getBlock().getType() != Material.STATIONARY_WATER
										&& location.getBlock().getType() != null) {
									String nonAirOrStationaryWaterBlockDetected = LocalizedMessageEnum.NON_AIR_OR_STATIONARY_WATER_BLOCK_DETECTED
											.getKey();
									String coords = "({0},{1},{2})";
									coords = new MessageFormat(coords).format(new Object[] { location.getBlockY() + "",
											location.getBlockY() + "", location.getBlockZ() + "" });
									throw new BuildException(nonAirOrStationaryWaterBlockDetected,
											new Object[] { coords });
								}
							}
							
							

							Map<Character, ComplexMaterial> symbolMap = constructionReciept.getSymbolMap();

							Byte originalData = location.getBlock().getData();
							Byte changeData = layout.getMatDataMap() != null ? layout.getMatDataMap().get(mat + "") : 0;
							
							ComplexMaterial fromComplexMaterial = isTriggerBlock ? replacedComplexMaterial
									: new ComplexMaterial(location.getBlock().getType(), originalData);
							
							ComplexMaterial toComplexMaterial = new ComplexMaterial(material, changeData);

							Coord coord = new Coord(location.getBlock());
							if (!symbolMap.containsKey(mat)) {
								symbolMap.put(mat, toComplexMaterial);
							}

							BlockChange blockChange = new BlockChange();
							blockChange.setTo(fromComplexMaterial.getMaterial() == Material.BEDROCK ? fromComplexMaterial : toComplexMaterial);
							blockChange.setFrom(fromComplexMaterial);
							blockChange.setCoord(coord);
							blockChange.setSymbol(mat);
							constructionReciept.addChange(blockChange);

							blocksMap.put(coord, location.getBlock());

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
			nextLayer(zAxis, startZ, startX, location, layout.isBuildDown());
		}

		return blocksMap;
	}

	private void shiftStartingPoint(Layout layout, boolean zAxis, Location location,
			PlacementOrientation placementOrientation) {
		String firstLayerNumber = layout.getLayerNumbers().get(0);
		String firstRowNumber = layout.getRowsNumberPerLayer().get(firstLayerNumber).get(0);
		String firstRow = layout.getLayerNumberRowNumberRowMap().get(firstLayerNumber).get(firstRowNumber);

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

	private void nextLayer(boolean zAxis, double startZ, double startX, Location location, boolean isDown) {
		if (zAxis) {
			location.setX(startX);
		} else {
			location.setZ(startZ);
		}

		location.setY(location.getY() + (isDown ? -1 : 1));
	}

	protected PlacementOrientation validatePlacementOrientation(Location playerLocation, Location otherLocation)
			throws BuildException {
		PlacementOrientation placementOrientation = plugin.getClickedBlockHelper()
				.getPlacementOrientation(playerLocation, otherLocation);

		if (placementOrientation == null) {
			throw new BuildException(LocalizedMessageEnum.IMPROPER_ORIENTATION.getKey());
		}
		return placementOrientation;
	}

	protected void validateMaterialAmounts(Layout layout, HumanEntity player) throws BuildException {

		if (!layout.requireMats()) {
			return;
		}

		for (String complexMaterialString : layout.getRequirements().keySet()) {
			ComplexMaterial complexMaterial = ComplexMaterial.fromString(complexMaterialString);
			int amountRequired = layout.getRequirements().get(complexMaterialString);
			int amountFound = 0;
			if (layout.isRequireMatsInBag()) {
				amountFound = getAmountOfMaterialInBag(player, complexMaterial);
			} else if (layout.isRequireMatsLoaded()) {
				amountFound = layout.getSupplies() != null && layout.getSupplies().containsKey(complexMaterialString)
						? layout.getSupplies().get(complexMaterialString) : 0;
			}
			if (amountFound < amountRequired) {
				String missingRequiredMaterials = LocalizedMessageEnum.MISSING_REQUIRED_MATERIALS.getKey();
				String rightClickToViewRequirements = LocalizedMessageEnum.RIGHT_CLICK_TO_VIEW_REQUIREMENTS.getKey();
				throw new BuildException(Arrays.asList(missingRequiredMaterials, rightClickToViewRequirements));
			}
		}
	}

	protected int getAmountOfMaterialInBag(HumanEntity player, ComplexMaterial complexMaterial) {
		return plugin.getPlayerHelper().getAmountOfMaterialInBag(player, complexMaterial, false);
	}

	protected boolean itemMatchesComplexMaterial(ItemStack item, ComplexMaterial complexMaterial) {
		return plugin.getClickedBlockHelper().itemMatchesComplexMaterial(item, complexMaterial, false);
	}

	protected void spendMaterialsInBag(Layout layout, HumanEntity player) {
		if (!layout.isRequireMatsInBag()) {
			return;
		}
		
		Map<ComplexMaterial, Integer> requirements = new HashMap<>();
		if(layout.getRequirements() != null){
			for(String key : layout.getRequirements().keySet()){
				ComplexMaterial complexMaterial = ComplexMaterial.fromString(key);
				requirements.put(complexMaterial, layout.getRequirements().get(key));
			}
		}
		
		plugin.getPlayerHelper().spendMaterialsInBag(requirements, player);
	}

	// GETTERS AND SETTERS

}
