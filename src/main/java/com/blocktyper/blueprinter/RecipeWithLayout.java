package com.blocktyper.blueprinter;

import java.util.List;

import org.bukkit.entity.HumanEntity;

import com.blocktyper.v1_2_6.recipes.AbstractBlockTyperRecipe;
import com.blocktyper.v1_2_6.recipes.IRecipe;

public class RecipeWithLayout extends AbstractBlockTyperRecipe {

	Layout layout;

	RecipeWithLayout(IRecipe recipe, BlueprinterPlugin plugin, Layout layout) {
		super(recipe, plugin);
		this.layout = layout;
	}

	@Override
	public List<String> getLocalizedLoreForPlugin(IRecipe recipe, HumanEntity player) {
		return layout != null ? layout.getLocalizedLoreForPlugin(recipe, player, plugin) : null;
	}
}
