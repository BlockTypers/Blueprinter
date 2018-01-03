package com.blocktyper.blueprinter.listeners;

import com.blocktyper.blueprinter.BlueprinterPlugin;
import com.blocktyper.v1_2_6.BlockTyperListener;
import com.blocktyper.v1_2_6.IBlockTyperPlugin;

public abstract class LayoutBaseListener extends BlockTyperListener {
	protected BlueprinterPlugin plugin;

	@Override
	public void init(IBlockTyperPlugin plugin) {
		super.init(plugin);
		this.plugin = (BlueprinterPlugin)plugin;
		
	}
	
	

}
