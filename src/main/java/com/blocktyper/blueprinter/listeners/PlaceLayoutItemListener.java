package com.blocktyper.blueprinter.listeners;

import java.util.ArrayList;
import java.util.List;
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
import com.blocktyper.v1_2_0.helpers.Coord;
import com.blocktyper.v1_2_0.nbt.NBTItem;
import com.blocktyper.v1_2_0.recipes.IRecipe;

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

			if (constructionReceipt != null) {
				ItemStack constructionReceiptItem = recipeRegistrar().getItemFromRecipe("construction-receipt", player, null, null);

				if (constructionReceiptItem != null) {
					String uuid = UUID.randomUUID().toString();
					constructionReceipt.setUuid(uuid);
					
					String newDisplayName = constructionReceiptItem.getItemMeta().getDisplayName();
					ItemMeta constructionReceiptMeta = constructionReceiptItem.getItemMeta();
					constructionReceiptMeta.setDisplayName(newDisplayName);
					
					List<String> lore = constructionReceiptMeta.getLore();
					
					if(lore == null){
						lore = new ArrayList<>();
					}
					
					
					String locationMessage = Coord.getFormatted(location, true);
					lore.add(locationMessage);
					
					lore.add(itemInHand.getItemMeta().getDisplayName());
					
					//String biome = location.getWorld().getBiome(location.getBlockX(), location.getBlockZ()).toString();
					//lore.add(biome);
					
					constructionReceiptMeta.setLore(lore);
					constructionReceiptItem.setItemMeta(constructionReceiptMeta);

					NBTItem nbtItem = new NBTItem(constructionReceiptItem);
					nbtItem.setObject(plugin.getConstructionRecieptKey(), constructionReceipt);
					nbtItem.setString(IRecipe.NBT_BLOCKTYPER_UNIQUE_ID, uuid);
					
					player.getWorld().dropItem(location, nbtItem.getItem());
				}

			}

		} catch (BuildException e) {
			e.sendMessages(event.getPlayer(), plugin);
			event.setCancelled(true);
		}
	}

}
