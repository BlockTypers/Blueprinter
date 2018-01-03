package com.blocktyper.blueprinter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.blocktyper.blueprinter.data.ConstructionReceipt;
import com.blocktyper.blueprinter.listeners.ConstructionReceiptInventoryListener;
import com.blocktyper.blueprinter.listeners.PlaceLayoutItemListener;
import com.blocktyper.blueprinter.listeners.RequireMatsClickListener;
import com.blocktyper.v1_2_6.BlockTyperBasePlugin;
import com.blocktyper.v1_2_6.nbt.NBTItem;
import com.blocktyper.v1_2_6.recipes.IRecipe;

public class BlueprinterPlugin extends BlockTyperBasePlugin {

	public static final String RESOURCE_NAME = "com.blocktyper.blueprinter.resources.BlueprinterMessages";
	public static final String RECIPES_KEY = "HOUSE_IN_BOTTLE_RECIPE_KEY";

	public BlueprinterPlugin() {
		super();
	}

	public void onEnable() {
		super.onEnable();
		registerListener(PlaceLayoutItemListener.class);
		registerListener(RequireMatsClickListener.class);
		registerListener(ConstructionReceiptInventoryListener.class);
	}

	public String getLayoutKey() {
		return "LAYOUT-" + getRecipesNbtKey();
	}

	public String getConstructionRecieptKey() {
		return "CONSTRUCTION_RECEIPT-" + getRecipesNbtKey();
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

	public ConstructionReceipt getConstructionReciept(ItemStack item) {
		if (item == null) {
			return null;
		}
		return getObject(item, getConstructionRecieptKey(), ConstructionReceipt.class);
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
		super.onPrepareItemCraft(event);
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
