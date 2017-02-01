package com.blocktyper.blueprinter.listeners;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import com.blocktyper.blueprinter.BlueprinterPlugin;
import com.blocktyper.blueprinter.BuildProcess;
import com.blocktyper.blueprinter.data.ConstructionReciept;

public class ConstructionReceiptListener extends LayoutBaseListener {

	Map<String, Date> lastUndoRedoMap = new HashMap<>();
	private static final int UNDO_REDO_COOL_DOWN_MS = 1000;

	public ConstructionReceiptListener(BlueprinterPlugin plugin) {
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
		
		if(player.getEquipment().getItemInOffHand() != null && player.getEquipment().getItemInOffHand().getType() == Material.COMPASS){
			Location tpLocation = new Location(player.getWorld(), constructionReciept.getPlayerX(), constructionReciept.getPlayerY(), constructionReciept.getPlayerZ());
			player.teleport(tpLocation);
			return;
		}
		
		String tpMessage = "You must have a compass in your off-hand to teleport with this item.";
		
		if(!player.isOp()){
			player.sendMessage(tpMessage);
			return;
		}
		
		if(player.getEquipment().getItemInOffHand() == null || player.getEquipment().getItemInOffHand().getType() != Material.DIAMOND){
			
			player.sendMessage(tpMessage);
			
			String hideShowMessage = "You must have a Bedrock in your off-hand to hide/show with this item.  Left click=Hide alterations*, Right click=Show the alterations";
			String hideShowDetailsMessage = "Left click=Hide alterations, Right click=Show the alterations";
			player.sendMessage(hideShowMessage);
			player.sendMessage(hideShowDetailsMessage);
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
}
