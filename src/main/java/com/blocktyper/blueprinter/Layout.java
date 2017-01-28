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
	private Map<String, Byte> matDataMap;
	private boolean buildDown;
	private boolean allowReplacement;
	private boolean requireMatsLoaded;
	private boolean requireMatsInBag;
	private Map<Material, Integer> requirements;
	private Map<Material, Integer> supplies;

	private static final Set<Material> NON_REQUIRED_MATERIALS = 
			new HashSet<>(Arrays.asList(
					Material.AIR, 
					Material.STATIONARY_LAVA, 
					Material.STATIONARY_WATER, 
					Material.WATER));
	
	private static final String CONFIG_PREFIX_STRUCTURES = "structures.";
	private static final String CONFIG_SUFFIX_LAYOUT = ".layout";
	private static final String CONFIG_SUFFIX_MATS = ".mats";
	private static final String CONFIG_SUFFIX_ALLOW_REPLACEMENT = ".allow-replacement";
	private static final String CONFIG_SUFFIX_BUILD_DOWN = ".build-down";
	private static final String CONFIG_SUFFIX_REQUIRE_MATS_IN_BAG = ".require-mats-in-bag";
	private static final String CONFIG_SUFFIX_REQUIRE_MATS_LOADED = ".require-mats-loaded";
	
	private static final String CONFIG_PREFIX_LAYOUTS = "layouts.";
	private static final String CONFIG_SUFFIX_SYMBOLS = ".symbols";
	private static final String CONFIG_SUFFIX_FLOORS = ".floors";
	private static final String CONFIG_SUFFIX_FLOOR = ".floor.";
	private static final String CONFIG_SUFFIX_ROWS = ".rows";
	private static final String CONFIG_SUFFIX_ROW = ".row.";
	
	
	private static final String CONFIG_PREFIX_MATS = "mats.";
	

	public static final String SKIP_MATERIAL = "BLUEPRINTER_SKIP";
	public static final String MATERIAL_DATA_SEPARATOR = "-";

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

	public Map<String, Byte> getMatDataMap() {
		return matDataMap;
	}

	public void setMatDataMap(Map<String, Byte> matDataMap) {
		this.matDataMap = matDataMap;
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

	public boolean isBuildDown() {
		return buildDown;
	}

	public void setBuildDown(boolean buildDown) {
		this.buildDown = buildDown;
	}

	public boolean isAllowReplacement() {
		return allowReplacement;
	}

	public void setAllowReplacement(boolean allowReplacement) {
		this.allowReplacement = allowReplacement;
	}

	public boolean requireMats() {
		return isRequireMatsInBag() || isRequireMatsLoaded();
	}
	
	public static String getLayoutKey(String recipesKey, BlueprinterPlugin plugin) {
		return plugin.getConfig().getString(CONFIG_PREFIX_STRUCTURES + recipesKey + CONFIG_SUFFIX_LAYOUT);
	}

	public static boolean hasLayout(String recipesKey, BlueprinterPlugin plugin) {
		String layoutKey = getLayoutKey(recipesKey, plugin);
		return layoutKey != null && !layoutKey.isEmpty();
	}

	public static Layout getLayout(String recipesKey, BlueprinterPlugin plugin) throws BuildException {
		Layout layout = new Layout();

		String layoutKey = getLayoutKey(recipesKey, plugin);
		String matsDefinitionsKey = plugin.getConfig().getString(CONFIG_PREFIX_STRUCTURES + recipesKey + CONFIG_SUFFIX_MATS);
		boolean buildDown = plugin.getConfig().getBoolean(CONFIG_PREFIX_STRUCTURES + recipesKey + CONFIG_SUFFIX_BUILD_DOWN, false);
		boolean allowReplacement = plugin.getConfig().getBoolean(CONFIG_PREFIX_STRUCTURES + recipesKey + CONFIG_SUFFIX_ALLOW_REPLACEMENT, false);
		boolean requireMatsInBag = plugin.getConfig().getBoolean(CONFIG_PREFIX_STRUCTURES + recipesKey + CONFIG_SUFFIX_REQUIRE_MATS_IN_BAG, false);
		boolean requireMatsLoaded = false;

		
		if (!requireMatsInBag) {
			requireMatsLoaded = plugin.getConfig().getBoolean(CONFIG_PREFIX_STRUCTURES + recipesKey + CONFIG_SUFFIX_REQUIRE_MATS_LOADED, false);
		}

		List<String> symbols = plugin.getConfig().getStringList(CONFIG_PREFIX_LAYOUTS + layoutKey + CONFIG_SUFFIX_SYMBOLS);

		Map<String, Material> matMap = new HashMap<>();
		Map<String, Byte> matDataMap = new HashMap<>();
		for (String symbol : symbols) {

			String mat = plugin.getConfig().getString(CONFIG_PREFIX_MATS + matsDefinitionsKey + "." + symbol);
			
			if (mat != null && !mat.isEmpty()) {
				if(mat.equals(SKIP_MATERIAL)){
					matMap.put(symbol, null);
				}else{
					Byte data = 0;
					if(mat.contains(MATERIAL_DATA_SEPARATOR)){
						String dataString = mat.substring(mat.indexOf(MATERIAL_DATA_SEPARATOR)+1);
						data = Byte.parseByte(dataString);
						mat = mat.substring(0, mat.indexOf(MATERIAL_DATA_SEPARATOR));
					}
					Material material = Material.matchMaterial(mat);
					if(material == null){
						String undefinedMaterial = LocalizedMessageEnum.UNDEFINED_MATERIAL.getKey();
						throw new BuildException(undefinedMaterial, new Object[] { symbol+"="+mat });
					}
					matMap.put(symbol, material);
					matDataMap.put(symbol, data);
				}
				
			} else {
				String undefinedMaterial = LocalizedMessageEnum.UNDEFINED_MATERIAL.getKey();
				throw new BuildException(undefinedMaterial, new Object[] { symbol });
			}
		}

		layout.setMatMap(matMap);
		layout.setMatDataMap(matDataMap);
		layout.setFloorNumbers(plugin.getConfig().getStringList(CONFIG_PREFIX_LAYOUTS + layoutKey + CONFIG_SUFFIX_FLOORS));
		layout.setRowsNumberPerFloor(new HashMap<>());
		layout.setFloorNumberRowNumberRowMap(new HashMap<>());
		layout.setRequireMatsInBag(requireMatsInBag);
		layout.setRequireMatsLoaded(requireMatsLoaded);
		
		layout.setAllowReplacement(allowReplacement);
		layout.setBuildDown(buildDown);

		if (layout.requireMats()) {
			layout.setRequirements(new HashMap<>());
		}

		for (String floorNumber : layout.getFloorNumbers()) {

			List<String> rowNumbers = plugin.getConfig()
					.getStringList(CONFIG_PREFIX_LAYOUTS + layoutKey + CONFIG_SUFFIX_FLOOR + floorNumber + CONFIG_SUFFIX_ROWS);

			layout.getRowsNumberPerFloor().put(floorNumber, rowNumbers);
			layout.getFloorNumberRowNumberRowMap().put(floorNumber, new HashMap<>());
			for (String rowNumber : rowNumbers) {
				String row = plugin.getConfig()
						.getString(CONFIG_PREFIX_LAYOUTS + layoutKey + CONFIG_SUFFIX_FLOOR + floorNumber + CONFIG_SUFFIX_ROW + rowNumber);
				layout.getFloorNumberRowNumberRowMap().get(floorNumber).put(rowNumber, row);

				for (char mat : row.toCharArray()) {

					Material material = layout.getMatMap().get(mat + "");

					if (material != null) {
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
