package com.blocktyper.blueprinter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class Layout {
	private List<String> floorNumbers;
	private Map<String, List<String>> rowsNumberPerFloor;
	private Map<String, Map<String, String>> floorNumberRowNumberRowMap;
	private Map<String, Material> matMap;
	private boolean requireMatsLoaded;
	private boolean requireMatsInBag;
	private Map<Material, Integer> requirements;
	private Map<Material, Integer> supplies;

	private static Set<Material> NON_REQUIRED_MATERIALS;

	static {
		NON_REQUIRED_MATERIALS = new HashSet<Material>(
				Arrays.asList(Material.AIR, Material.STATIONARY_LAVA, Material.STATIONARY_WATER, Material.WATER));
	}

	public List<String> getFloorNumbers() {
		return floorNumbers;
	}

	public void setFloorNumbers(List<String> floorNumbers) {
		this.floorNumbers = floorNumbers;
	}

	public Map<String, List<String>> getRowsNumberPerFloor() {
		return rowsNumberPerFloor;
	}

	public void setRowsNumberPerFloor(Map<String, List<String>> rowsNumberPerFloor) {
		this.rowsNumberPerFloor = rowsNumberPerFloor;
	}

	public Map<String, Map<String, String>> getFloorNumberRowNumberRowMap() {
		return floorNumberRowNumberRowMap;
	}

	public void setFloorNumberRowNumberRowMap(Map<String, Map<String, String>> floorNumberRowNumberRowMap) {
		this.floorNumberRowNumberRowMap = floorNumberRowNumberRowMap;
	}

	public Map<String, Material> getMatMap() {
		return matMap;
	}

	public void setMatMap(Map<String, Material> matMap) {
		this.matMap = matMap;
	}

	public boolean isRequireMatsInBag() {
		return requireMatsInBag;
	}

	public void setRequireMatsInBag(boolean requireMatsInBag) {
		this.requireMatsInBag = requireMatsInBag;
	}

	public boolean isRequireMatsLoaded() {
		return requireMatsLoaded;
	}

	public void setRequireMatsLoaded(boolean requireMatsLoaded) {
		this.requireMatsLoaded = requireMatsLoaded;
	}

	public Map<Material, Integer> getRequirements() {
		return requirements;
	}

	public void setRequirements(Map<Material, Integer> requirements) {
		this.requirements = requirements;
	}

	public Map<Material, Integer> getSupplies() {
		return supplies;
	}

	public void setSupplies(Map<Material, Integer> supplies) {
		this.supplies = supplies;
	}

	public boolean requireMats() {
		return isRequireMatsInBag() || isRequireMatsLoaded();
	}

	public static boolean hasLayout(String recipesKey, BlueprinterPlugin plugin) {
		String layoutKey = plugin.getConfig().getString(recipesKey + ".layout");
		return layoutKey != null && !layoutKey.isEmpty();
	}

	public static Layout getLayout(String recipesKey, BlueprinterPlugin plugin) throws BuildException {
		Layout layout = new Layout();

		String layoutKey = plugin.getConfig().getString(recipesKey + ".layout");
		String matsDefinitionsKey = plugin.getConfig().getString(recipesKey + ".mats");
		boolean requireMatsInBag = plugin.getConfig().getBoolean(recipesKey + ".require-mats-in-bag", false);
		boolean requireMatsLoaded = false;

		if (!requireMatsInBag) {
			requireMatsLoaded = plugin.getConfig().getBoolean(recipesKey + ".require-mats-loaded", false);
		}

		List<String> symbols = plugin.getConfig().getStringList("layout." + layoutKey + ".mats.symbols");

		Map<String, Material> matMap = new HashMap<>();
		for (String symbol : symbols) {
			String mat = plugin.getConfig().getString("layout." + matsDefinitionsKey + ".mats.definitions." + symbol);

			if (mat != null && !mat.isEmpty()) {
				matMap.put(symbol, Material.matchMaterial(mat));
			} else {
				throw new BuildException("Material symbol was null or empty: " + symbol);
			}
		}

		layout.setMatMap(matMap);
		layout.setFloorNumbers(plugin.getConfig().getStringList("layout." + layoutKey + ".floors"));
		layout.setRowsNumberPerFloor(new HashMap<>());
		layout.setFloorNumberRowNumberRowMap(new HashMap<>());
		layout.setRequireMatsInBag(requireMatsInBag);
		layout.setRequireMatsLoaded(requireMatsLoaded);

		if (layout.requireMats()) {
			layout.setRequirements(new HashMap<>());
		}

		Map<String, Map<String, String>> rowsPerFloor = new HashMap<>();

		for (String floorNumber : layout.getFloorNumbers()) {
			rowsPerFloor.put(floorNumber, new HashMap<>());

			List<String> rowNumbers = plugin.getConfig()
					.getStringList("layout." + layoutKey + ".floor." + floorNumber + ".rows");

			layout.getRowsNumberPerFloor().put(floorNumber, rowNumbers);
			layout.getFloorNumberRowNumberRowMap().put(floorNumber, new HashMap<>());
			for (String rowNumber : rowNumbers) {
				String row = plugin.getConfig()
						.getString("layout." + layoutKey + ".floor." + floorNumber + ".row." + rowNumber);
				layout.getFloorNumberRowNumberRowMap().get(floorNumber).put(rowNumber, row);

				for (char mat : row.toCharArray()) {

					Material material = layout.getMatMap().get(mat + "");

					if (material == null) {
						throw new BuildException("NULL MAT!!! " + mat);
					}

					if (layout.requireMats()) {
						if (!NON_REQUIRED_MATERIALS.contains(material)) {
							if (!layout.getRequirements().containsKey(material)) {
								layout.getRequirements().put(material, 1);
							} else {
								layout.getRequirements().put(material, layout.getRequirements().get(material) + 1);
							}
						}
					}
				}
			}
		}

		return layout;
	}

	public static boolean itemIsSuitableForLoading(ItemStack item) {
		if (item == null) {
			return false;
		}

		if (item.getItemMeta() == null || (item.getItemMeta().getDisplayName() == null
				&& (item.getItemMeta().getLore() == null || item.getItemMeta().getLore().isEmpty()))) {
			return true;
		}
		return false;
	}

}
