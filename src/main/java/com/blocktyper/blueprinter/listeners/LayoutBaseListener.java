package com.blocktyper.blueprinter.listeners;

import org.bukkit.event.Listener;

import com.blocktyper.blueprinter.BlueprinterPlugin;
import com.blocktyper.v1_1_8.BlockTyperListener;

public abstract class LayoutBaseListener extends BlockTyperListener {
	protected BlueprinterPlugin plugin;

	public LayoutBaseListener(BlueprinterPlugin plugin) {
		super();
		init(plugin);
	}

}
