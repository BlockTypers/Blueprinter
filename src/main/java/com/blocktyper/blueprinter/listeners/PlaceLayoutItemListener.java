package com.blocktyper.blueprinter.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.blocktyper.blueprinter.BlueprinterPlugin;
import com.blocktyper.blueprinter.BuildException;
import com.blocktyper.blueprinter.BuildProcess;
import com.blocktyper.blueprinter.Layout;
import com.blocktyper.blueprinter.data.ConstructionReciept;
import com.blocktyper.v1_1_8.nbt.NBTItem;

public class PlaceLayoutItemListener extends LayoutBaseListener {

	public PlaceLayoutItemListener(BlueprinterPlugin plugin) {
		super(plugin);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) {
		if (event.getBlock() == null) {
			return;
		}

		ItemStack itemInHand = plugin.getPlayerHelper().getItemInHand(event.getPlayer());

		Layout layout = plugin.getLayout(itemInHand);

		if (layout == null) {
			plugin.debugInfo("No layout");
			return;
		}

		HumanEntity player = event.getPlayer();
		Block block = event.getBlock();
		Location location = block.getLocation();

		try {
			BuildProcess buildProcess = new BuildProcess(plugin, layout);
			buildProcess.init();
			ConstructionReciept constructionReceipt = buildProcess.validateAndDoFirstBuild(player, location,
					event.getBlockReplacedState());

			if (constructionReceipt != null && player.isOp()) {
				ItemStack plansItem = player.getInventory().getItemInOffHand();

				if (plansItem != null && plansItem.getType() == Material.PAPER
						&& plansItem.getItemMeta().getDisplayName() == null) {
					ItemMeta itemMeta = plansItem.getItemMeta();
					itemMeta.setDisplayName("Construction Receipt");
					plansItem.setItemMeta(itemMeta);
					NBTItem nbtItem = new NBTItem(plansItem);

					nbtItem.setObject(plugin.getConstructionRecieptKey(), constructionReceipt);

					player.getInventory().setItemInOffHand(null);
					player.getWorld().dropItem(location, nbtItem.getItem());
				}
			}

		} catch (BuildException e) {
			e.sendMessages(event.getPlayer(), plugin);
			event.setCancelled(true);
		}
	}

}
