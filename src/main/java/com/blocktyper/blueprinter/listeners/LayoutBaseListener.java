package com.blocktyper.blueprinter.listeners;

import org.bukkit.event.Listener;

import com.blocktyper.blueprinter.BlueprinterPlugin;

public abstract class LayoutBaseListener implements Listener {
	protected BlueprinterPlugin plugin;

	public LayoutBaseListener(BlueprinterPlugin plugin) {
		this.plugin = plugin;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

}
