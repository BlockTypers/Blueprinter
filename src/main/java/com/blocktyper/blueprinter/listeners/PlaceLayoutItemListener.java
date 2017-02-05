package com.blocktyper.blueprinter.listeners;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.blocktyper.blueprinter.BuildException;
import com.blocktyper.blueprinter.BuildProcess;
import com.blocktyper.blueprinter.Layout;
import com.blocktyper.blueprinter.data.ConstructionReceipt;
import com.blocktyper.v1_1_8.nbt.NBTItem;
import com.blocktyper.v1_1_8.recipes.IRecipe;

public class PlaceLayoutItemListener extends LayoutBaseListener {

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) {
		if (event.getBlock() == null) {
			return;
		}

		ItemStack itemInHand = getPlayerHelper().getItemInHand(event.getPlayer());

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
			ConstructionReceipt constructionReceipt = buildProcess.validateAndDoFirstBuild(player, location,
					event.getBlockReplacedState());

			if (constructionReceipt != null && player.isOp()) {
				ItemStack intemInOffHand = player.getInventory().getItemInOffHand();

				ItemStack plansItem = recipeRegistrar().getItemFromRecipe("construction-receipt", player, null, null);

				if (plansItem != null && intemInOffHand != null && plansItem.getType() == intemInOffHand.getType()
						&& intemInOffHand.getItemMeta().getDisplayName() == null && intemInOffHand.getAmount() == 1) {
					ItemMeta itemMeta = plansItem.getItemMeta();
					itemMeta.setDisplayName("Construction Receipt");
					plansItem.setItemMeta(itemMeta);

					String uuid = UUID.randomUUID().toString();
					constructionReceipt.setUuid(uuid);
					
					NBTItem nbtItem = new NBTItem(plansItem);
					nbtItem.setObject(plugin.getConstructionRecieptKey(), constructionReceipt);
					nbtItem.setString(IRecipe.NBT_BLOCKTYPER_UNIQUE_ID, uuid);

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
