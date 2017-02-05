package com.blocktyper.blueprinter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.ItemStack;

import com.blocktyper.v1_1_8.IBlockTyperPlugin;
import com.blocktyper.v1_1_8.helpers.ComplexMaterial;
import com.blocktyper.v1_1_8.recipes.IRecipe;

public class Layout {
	
	public String recipesKey;

	private List<String> layerNumbers;

	private Map<String, List<String>> rowsNumberPerLayer;

	private Map<String, Map<String, String>> layerNumberRowNumberRowMap;

	private Map<String, Material> matMap;

	private Map<String, Byte> matDataMap;

	private boolean buildDown;

	private boolean allowReplacement;

	private boolean requireMatsLoaded;

	private boolean requireMatsInBag;

	private Map<ComplexMaterial, Integer> requirements;

	private Map<ComplexMaterial, Integer> supplies;

	private static final Set<Material> NON_REQUIRED_MATERIALS = new HashSet<>(
			Arrays.asList(Material.AIR, Material.STATIONARY_LAVA, Material.STATIONARY_WATER, Material.WATER));

	private static final String CONFIG_PREFIX_STRUCTURES = "structures.";
	private static final String CONFIG_SUFFIX_LAYOUT = ".layout";
	private static final String CONFIG_SUFFIX_MATS = ".mats";
	private static final String CONFIG_SUFFIX_ALLOW_REPLACEMENT = ".allow-replacement";
	private static final String CONFIG_SUFFIX_BUILD_DOWN = ".build-down";
	private static final String CONFIG_SUFFIX_REQUIRE_MATS_IN_BAG = ".require-mats-in-bag";
	private static final String CONFIG_SUFFIX_REQUIRE_MATS_LOADED = ".require-mats-loaded";

	private static final String CONFIG_PREFIX_LAYOUTS = "layouts.";
	private static final String CONFIG_SUFFIX_SYMBOLS = ".symbols";
	private static final String CONFIG_SUFFIX_LAYERS = ".layers";
	private static final String CONFIG_SUFFIX_LAYER = ".layer.";
	private static final String CONFIG_SUFFIX_ROWS = ".rows";
	private static final String CONFIG_SUFFIX_ROW = ".row.";

	private static final String CONFIG_PREFIX_MATS = "mats.";

	public static final String SKIP_MATERIAL = "BLUEPRINTER_SKIP";
	public static final String MATERIAL_DATA_SEPARATOR = "-";
	
	

	public String getRecipesKey() {
		return recipesKey;
	}

	public void setRecipesKey(String recipesKey) {
		this.recipesKey = recipesKey;
	}

	public List<String> getLayerNumbers() {
		return layerNumbers;
	}

	public void setLayerNumbers(List<String> layerNumbers) {
		this.layerNumbers = layerNumbers;
	}

	public Map<String, List<String>> getRowsNumberPerLayer() {
		return rowsNumberPerLayer;
	}

	public void setRowsNumberPerLayer(Map<String, List<String>> rowsNumberPerLayer) {
		this.rowsNumberPerLayer = rowsNumberPerLayer;
	}

	public Map<String, Map<String, String>> getLayerNumberRowNumberRowMap() {
		return layerNumberRowNumberRowMap;
	}

	public void setLayerNumberRowNumberRowMap(Map<String, Map<String, String>> layerNumberRowNumberRowMap) {
		this.layerNumberRowNumberRowMap = layerNumberRowNumberRowMap;
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

	public Map<ComplexMaterial, Integer> getRequirements() {
		return requirements;
	}

	public void setRequirements(Map<ComplexMaterial, Integer> requirements) {
		this.requirements = requirements;
	}

	public Map<ComplexMaterial, Integer> getSupplies() {
		return supplies;
	}

	public void setSupplies(Map<ComplexMaterial, Integer> supplies) {
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
		return getLayout(plugin.recipeRegistrar().getRecipeFromKey(recipesKey), plugin);
	}

	public static Layout getLayout(IRecipe recipe, BlueprinterPlugin plugin) throws BuildException {

		Layout layout = new Layout();

		String recipesKey = recipe.getKey();

		String layoutKey = getLayoutKey(recipesKey, plugin);
		String matsDefinitionsKey = plugin.getConfig()
				.getString(CONFIG_PREFIX_STRUCTURES + recipesKey + CONFIG_SUFFIX_MATS);
		boolean buildDown = plugin.getConfig()
				.getBoolean(CONFIG_PREFIX_STRUCTURES + recipesKey + CONFIG_SUFFIX_BUILD_DOWN, false);
		boolean allowReplacement = plugin.getConfig()
				.getBoolean(CONFIG_PREFIX_STRUCTURES + recipesKey + CONFIG_SUFFIX_ALLOW_REPLACEMENT, false);
		boolean requireMatsInBag = plugin.getConfig()
				.getBoolean(CONFIG_PREFIX_STRUCTURES + recipesKey + CONFIG_SUFFIX_REQUIRE_MATS_IN_BAG, false);
		boolean requireMatsLoaded = false;

		if (!requireMatsInBag) {
			requireMatsLoaded = plugin.getConfig()
					.getBoolean(CONFIG_PREFIX_STRUCTURES + recipesKey + CONFIG_SUFFIX_REQUIRE_MATS_LOADED, false);
		}

		List<String> symbols = plugin.getConfig()
				.getStringList(CONFIG_PREFIX_LAYOUTS + layoutKey + CONFIG_SUFFIX_SYMBOLS);

		Map<String, Material> matMap = new HashMap<>();
		Map<String, Byte> matDataMap = new HashMap<>();
		for (String symbol : symbols) {

			String mat = plugin.getConfig().getString(CONFIG_PREFIX_MATS + matsDefinitionsKey + "." + symbol);

			if (mat != null && !mat.isEmpty()) {
				if (mat.equals(SKIP_MATERIAL)) {
					matMap.put(symbol, null);
				} else {
					Byte data = 0;
					if (mat.contains(MATERIAL_DATA_SEPARATOR)) {
						String dataString = mat.substring(mat.indexOf(MATERIAL_DATA_SEPARATOR) + 1);
						data = Byte.parseByte(dataString);
						mat = mat.substring(0, mat.indexOf(MATERIAL_DATA_SEPARATOR));
					}
					Material material = Material.matchMaterial(mat);
					if (material == null) {
						String undefinedMaterial = LocalizedMessageEnum.UNDEFINED_MATERIAL.getKey();
						throw new BuildException(undefinedMaterial, new Object[] { symbol + "=" + mat });
					}
					matMap.put(symbol, material);
					matDataMap.put(symbol, data);
				}

			} else {
				String undefinedMaterial = LocalizedMessageEnum.UNDEFINED_MATERIAL.getKey();
				throw new BuildException(undefinedMaterial, new Object[] { symbol });
			}
		}

		layout.setRecipesKey(recipesKey);
		layout.setMatMap(matMap);
		layout.setMatDataMap(matDataMap);
		layout.setLayerNumbers(
				plugin.getConfig().getStringList(CONFIG_PREFIX_LAYOUTS + layoutKey + CONFIG_SUFFIX_LAYERS));
		layout.setRowsNumberPerLayer(new HashMap<>());
		layout.setLayerNumberRowNumberRowMap(new HashMap<>());
		layout.setRequireMatsInBag(requireMatsInBag);
		layout.setRequireMatsLoaded(requireMatsLoaded);

		layout.setAllowReplacement(allowReplacement);
		layout.setBuildDown(buildDown);

		if (layout.requireMats()) {
			layout.setRequirements(new HashMap<>());
		}

		for (String layerNumber : layout.getLayerNumbers()) {

			List<String> rowNumbers = plugin.getConfig().getStringList(
					CONFIG_PREFIX_LAYOUTS + layoutKey + CONFIG_SUFFIX_LAYER + layerNumber + CONFIG_SUFFIX_ROWS);

			layout.getRowsNumberPerLayer().put(layerNumber, rowNumbers);
			layout.getLayerNumberRowNumberRowMap().put(layerNumber, new HashMap<>());
			for (String rowNumber : rowNumbers) {
				String row = plugin.getConfig().getString(CONFIG_PREFIX_LAYOUTS + layoutKey + CONFIG_SUFFIX_LAYER
						+ layerNumber + CONFIG_SUFFIX_ROW + rowNumber);
				layout.getLayerNumberRowNumberRowMap().get(layerNumber).put(rowNumber, row);

				for (char mat : row.toCharArray()) {

					Material material = layout.getMatMap().get(mat + "");
					

					if (material != null) {
						Byte data = layout.getMatDataMap().get(mat + "");
						ComplexMaterial complexMaterial = new ComplexMaterial(material, data);
						if (layout.requireMats()) {
							if (!NON_REQUIRED_MATERIALS.contains(complexMaterial)) {
								if (!layout.getRequirements().containsKey(complexMaterial)) {
									layout.getRequirements().put(complexMaterial, 1);
								} else {
									layout.getRequirements().put(complexMaterial, layout.getRequirements().get(complexMaterial) + 1);
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

	List<String> getLocalizedLoreForPlugin(IRecipe recipe, HumanEntity player, IBlockTyperPlugin plugin) {
		List<String> additionalLore = new ArrayList<>();

		if (isRequireMatsInBag()) {
			String loreLine = plugin.getLocalizedMessage(LocalizedMessageEnum.REQUIRE_MATS_IN_BAG.getKey(), player);
			additionalLore.add(loreLine);
		} else if (isRequireMatsLoaded()) {
			String loreLine = plugin.getLocalizedMessage(LocalizedMessageEnum.REQUIRE_MATS_LOADED.getKey(), player);
			additionalLore.add(loreLine);
		}

		String loreLine = plugin.getLocalizedMessage(LocalizedMessageEnum.BUILD_DOWN.getKey(), player);
		
		if (isBuildDown()) {
			additionalLore.add(loreLine);
		}

		loreLine = plugin.getLocalizedMessage(LocalizedMessageEnum.ALLOW_REPLACEMENT.getKey(), player);
		
		if (isAllowReplacement()) {
			additionalLore.add(loreLine);
		}
		
		loreLine = plugin.getLocalizedMessage(LocalizedMessageEnum.HEIGHT.getKey(), player);
		
		additionalLore.add("height: " + getLayerNumbers().size());

		return additionalLore;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (allowReplacement ? 1231 : 1237);
		result = prime * result + (buildDown ? 1231 : 1237);
		result = prime * result + ((layerNumberRowNumberRowMap == null) ? 0 : layerNumberRowNumberRowMap.hashCode());
		result = prime * result + ((layerNumbers == null) ? 0 : layerNumbers.hashCode());
		result = prime * result + ((matDataMap == null) ? 0 : matDataMap.hashCode());
		result = prime * result + ((matMap == null) ? 0 : matMap.hashCode());
		result = prime * result + (requireMatsInBag ? 1231 : 1237);
		result = prime * result + (requireMatsLoaded ? 1231 : 1237);
		result = prime * result + ((rowsNumberPerLayer == null) ? 0 : rowsNumberPerLayer.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Layout other = (Layout) obj;
		if (allowReplacement != other.allowReplacement)
			return false;
		if (buildDown != other.buildDown)
			return false;
		if (layerNumberRowNumberRowMap == null) {
			if (other.layerNumberRowNumberRowMap != null)
				return false;
		} else if (!layerNumberRowNumberRowMap.equals(other.layerNumberRowNumberRowMap))
			return false;
		if (layerNumbers == null) {
			if (other.layerNumbers != null)
				return false;
		} else if (!layerNumbers.equals(other.layerNumbers))
			return false;
		if (matDataMap == null) {
			if (other.matDataMap != null)
				return false;
		} else if (!matDataMap.equals(other.matDataMap))
			return false;
		if (matMap == null) {
			if (other.matMap != null)
				return false;
		} else if (!matMap.equals(other.matMap))
			return false;
		if (requireMatsInBag != other.requireMatsInBag)
			return false;
		if (requireMatsLoaded != other.requireMatsLoaded)
			return false;
		if (rowsNumberPerLayer == null) {
			if (other.rowsNumberPerLayer != null)
				return false;
		} else if (!rowsNumberPerLayer.equals(other.rowsNumberPerLayer))
			return false;
		return true;
	}
	

}
