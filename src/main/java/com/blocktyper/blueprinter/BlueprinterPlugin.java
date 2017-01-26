package com.blocktyper.blueprinter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.bukkit.Material;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.blocktyper.blueprinter.listeners.PlaceLayoutItemListener;
import com.blocktyper.v1_1_8.nbt.NBTItem;
import com.blocktyper.v1_1_8.plugin.BlockTyperPlugin;

public class BlueprinterPlugin extends BlockTyperPlugin {

	public static final String RESOURCE_NAME = "com.blocktyper.blueprinter.resources.BlueprinterMessages";
	public static final String RECIPES_KEY = "HOUSE_IN_BOTTLE_RECIPE_KEY";

	public BlueprinterPlugin() {
		super();
	}

	public void onEnable() {
		super.onEnable();
		new PlaceLayoutItemListener(this);
	}

	// begin localization
	private ResourceBundle bundle = null;

	public ResourceBundle getBundle() {
		if (bundle == null)
			bundle = ResourceBundle.getBundle(RESOURCE_NAME, locale);
		return bundle;
	}

	@Override
	public String getRecipesNbtKey() {
		return RECIPES_KEY;
	}

	@Override
	// begin localization
	public ResourceBundle getBundle(Locale locale) {
		return ResourceBundle.getBundle(RESOURCE_NAME, locale);
	}
	// end localization

	public String getRecipeKey(ItemStack item) {
		if (item != null) {
			NBTItem nbtItem = new NBTItem(item);
			if (nbtItem.hasKey(getRecipesNbtKey())) {
				String recipeKey = nbtItem.getString(getRecipesNbtKey());
				return recipeKey;
			}
		}
		return null;
	}

	public String getLayoutKey() {
		return "LAYOUT-" + getRecipesNbtKey();
	}

	public boolean hasLayout(String recipesKey) {
		List<String> symbols = getConfig().getStringList("layout." + recipesKey + ".mats.symbols");
		return symbols != null && !symbols.isEmpty();
	}

	public Layout getLayout(String recipesKey) throws BuildException {
		Layout layout = new Layout();

		List<String> symbols = getConfig().getStringList("layout." + recipesKey + ".mats.symbols");

		Map<String, Material> matMap = new HashMap<>();
		for (String symbol : symbols) {
			String mat = getConfig().getString("layout." + recipesKey + ".mats.definitions." + symbol);

			if (mat != null && !mat.isEmpty()) {
				matMap.put(symbol, Material.matchMaterial(mat));
			} else {
				throw new BuildException("Material symbol was null or empty: " + symbol);
			}

		}
		layout.setMatMap(matMap);
		layout.setFloorNumbers(getConfig().getStringList("layout." + recipesKey + ".floors"));
		layout.setRowsNumberPerFloor(new HashMap<>());
		layout.setFloorNumberRowNumberRowMap(new HashMap<>());
		Map<String, Map<String, String>> rowsPerFloor = new HashMap<>();

		for (String floorNumber : layout.getFloorNumbers()) {
			rowsPerFloor.put(floorNumber, new HashMap<>());

			List<String> rowNumbers = getConfig()
					.getStringList("layout." + recipesKey + ".floor." + floorNumber + ".rows");

			layout.getRowsNumberPerFloor().put(floorNumber, rowNumbers);
			layout.getFloorNumberRowNumberRowMap().put(floorNumber, new HashMap<>());
			for (String rowNumber : rowNumbers) {
				String row = getConfig()
						.getString("layout." + recipesKey + ".floor." + floorNumber + ".row." + rowNumber);
				layout.getFloorNumberRowNumberRowMap().get(floorNumber).put(rowNumber, row);
			}
		}

		return layout;
	}

	@Override
	public void onPrepareItemCraft(PrepareItemCraftEvent event) {
		ItemStack result = event.getInventory().getResult();
		String recipeKey = getRecipeKey(result);

		if (recipeKey == null) {
			info("Recipe key was null");
			return;
		}else{
			info("Recipe key: " + recipeKey);
		}

		if (hasLayout(recipeKey)) {
			info("Layout detected");
			try {
				Layout layout = getLayout(recipeKey);
				ItemMeta itemMeta = result.getItemMeta();
				List<String> lore = itemMeta.getLore();
				lore = lore == null ? new ArrayList<>() : lore;
				lore.add(layout.hashCode() + "");
				itemMeta.setLore(lore);
				result.setItemMeta(itemMeta);
				NBTItem nbtItem = new NBTItem(result);
				nbtItem.setObject(getLayoutKey(), layout);
				event.getInventory().setResult(nbtItem.getItem());
			} catch (BuildException e) {
				event.getInventory().setResult(null);
				e.printStackTrace();
			}
		}else{
			info("No Layout detected");
		}
	}

}
