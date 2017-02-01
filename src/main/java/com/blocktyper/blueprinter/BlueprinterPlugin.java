package com.blocktyper.blueprinter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.blocktyper.blueprinter.data.ConstructionReciept;
import com.blocktyper.blueprinter.listeners.ConstructionReceiptListener;
import com.blocktyper.blueprinter.listeners.PlaceLayoutItemListener;
import com.blocktyper.blueprinter.listeners.RequireMatsClickListener;
import com.blocktyper.v1_1_8.nbt.NBTItem;
import com.blocktyper.v1_1_8.plugin.BlockTyperPlugin;
import com.blocktyper.v1_1_8.recipes.IRecipe;

public class BlueprinterPlugin extends BlockTyperPlugin {

	public static final String RESOURCE_NAME = "com.blocktyper.blueprinter.resources.BlueprinterMessages";
	public static final String RECIPES_KEY = "HOUSE_IN_BOTTLE_RECIPE_KEY";

	public BlueprinterPlugin() {
		super();
	}

	public void onEnable() {
		super.onEnable();
		new PlaceLayoutItemListener(this);
		new RequireMatsClickListener(this);
		new ConstructionReceiptListener(this);
	}

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
	
	public String getConstructionRecieptKey() {
		return "CONSTRUCTION_RECEIPT-" + getRecipesNbtKey();
	}
	
	public String getBPChangePrefix() {
		return "bpchange";
	}

	public Layout getLayout(String recipesKey) throws BuildException {
		return Layout.getLayout(recipesKey, this);
	}

	public Layout getLayout(ItemStack item) {
		if (item == null) {
			return null;
		}
		return getObject(item, getLayoutKey(), Layout.class);
	}
	
	public ConstructionReciept getConstructionReciept(ItemStack item) {
		if (item == null) {
			return null;
		}
		return getObject(item, getConstructionRecieptKey(), ConstructionReciept.class);
	}
	
	public <T> T getObject(ItemStack item, String key, Class<T> type) {
		if (item == null) {
			return null;
		}

		NBTItem nbtItem = new NBTItem(item);
		T outObject = nbtItem.getObject(key, type);
		return outObject;
	}

	public ItemStack setLayout(ItemStack item, Layout layout) {
		if (item == null) {
			return null;
		}

		NBTItem nbtItem = new NBTItem(item);
		nbtItem.setObject(getLayoutKey(), layout);
		return nbtItem.getItem();
	}

	
	
	@Override
	public String getRecipesNbtKey() {
		return RECIPES_KEY;
	}

	@Override
	public IRecipe bootstrapRecipe(IRecipe recipe) {
		if (Layout.hasLayout(recipe.getKey(), this)) {
			try {
				Layout layout = Layout.getLayout(recipe, this);
				RecipeWithLayout recipeWithLayout = new RecipeWithLayout(recipe, this, layout);
				return recipeWithLayout;
			} catch (BuildException e) {
				e.printStackTrace();
			}

		}
		return recipe;
	}

	@Override
	// begin localization
	public ResourceBundle getBundle(Locale locale) {
		return ResourceBundle.getBundle(RESOURCE_NAME, locale);
	}
	// end localization
	
	@Override
	public void onPrepareItemCraft(PrepareItemCraftEvent event) {
		ItemStack result = event.getInventory().getResult();
		String recipeKey = getRecipeKey(result);

		if (recipeKey == null) {
			return;
		}

		if (Layout.hasLayout(recipeKey, this)) {
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
				e.sendMessages(event.getViewers().get(0), this);
			}
		}
	}

}
