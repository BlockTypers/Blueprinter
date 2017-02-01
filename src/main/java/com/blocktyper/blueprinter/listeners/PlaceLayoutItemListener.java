package com.blocktyper.blueprinter.listeners;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.blocktyper.blueprinter.BlueprinterPlugin;
import com.blocktyper.blueprinter.BuildException;
import com.blocktyper.blueprinter.BuildProcess;
import com.blocktyper.blueprinter.Layout;
import com.blocktyper.blueprinter.data.BlockChange;
import com.blocktyper.blueprinter.data.ConstructionReciept;
import com.blocktyper.v1_1_8.nbt.NBTItem;

public class PlaceLayoutItemListener extends LayoutBaseListener {

	Map<String, Date> lastUndoRedoMap = new HashMap<>();
	private static final int UNDO_REDO_COOL_DOWN_MS = 1000;

	public PlaceLayoutItemListener(BlueprinterPlugin plugin) {
		super(plugin);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPlayerUndoRedoConstruction(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		ItemStack itemInHand = player.getEquipment().getItemInMainHand();

		if (itemInHand == null || itemInHand.getType() == Material.AIR) {
			return;
		}

		if (lastUndoRedoMap.containsKey(player.getName())) {
			if (new Date().getTime() < lastUndoRedoMap.get(player.getName()).getTime() + UNDO_REDO_COOL_DOWN_MS) {
				return;
			}
		}

		ConstructionReciept constructionReciept = plugin.getConstructionReciept(itemInHand);

		if (constructionReciept == null) {
			plugin.debugInfo("No constructionReciept");
			return;
		}

		BuildProcess buildProcess = new BuildProcess(plugin, constructionReciept, itemInHand);

		if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
			player.sendMessage("Undo");
			buildProcess.restoreOriginalBlocks(player.getWorld());
		} else if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			player.sendMessage("Redo");
			buildProcess.applyChanges(player.getWorld());
		}

		lastUndoRedoMap.put(player.getName(), new Date());

		event.setCancelled(true);
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

			plugin.info("writing construction receipt...");
			if (constructionReceipt != null) {
				plugin.info("construction receipt returned");
				ItemStack plansItem = player.getInventory().getItemInOffHand();

				if (plansItem != null && plansItem.getType() == Material.PAPER
						&& plansItem.getItemMeta().getDisplayName() == null) {
					plugin.info("saving construction receipt... ");
					ItemMeta itemMeta = plansItem.getItemMeta();
					itemMeta.setDisplayName("Construction Receipt");
					plansItem.setItemMeta(itemMeta);
					NBTItem nbtItem = new NBTItem(plansItem);

					nbtItem.setObject(plugin.getConstructionRecieptKey(), constructionReceipt);
					
					int blockChangeCount = 0;
					if(constructionReceipt.getChanges() != null && !constructionReceipt.getChanges().isEmpty()){
						for(BlockChange change : constructionReceipt.getChanges()){
							nbtItem.setObject("bp-change" + blockChangeCount, change);
							blockChangeCount ++;
						}
					}
					
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
