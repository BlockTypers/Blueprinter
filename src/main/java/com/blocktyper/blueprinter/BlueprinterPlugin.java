package com.blocktyper.blueprinter;

import java.util.Locale;
import java.util.ResourceBundle;

import com.blocktyper.blueprinter.listeners.TestListener;
import com.blocktyper.v1_1_8.plugin.BlockTyperPlugin;

public class BlueprinterPlugin extends BlockTyperPlugin {
	
	public static final String RESOURCE_NAME = "com.blocktyper.blueprinter.resources.BlueprinterMessages";
	public static final String RECIPES_KEY = "HOUSE_IN_BOTTLE_RECIPE_KEY";
	
	public BlueprinterPlugin(){
		super();
	}

	public void onEnable() {
		super.onEnable();
		new TestListener(this);
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

}
