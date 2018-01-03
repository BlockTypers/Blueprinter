package com.blocktyper.blueprinter.listeners;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import com.blocktyper.blueprinter.BuildProcess;
import com.blocktyper.blueprinter.LocalizedMessageEnum;
import com.blocktyper.blueprinter.data.BlockChange;
import com.blocktyper.blueprinter.data.ConstructionReceipt;
import com.blocktyper.v1_2_6.helpers.ComplexMaterial;
import com.blocktyper.v1_2_6.helpers.Coord;
import com.blocktyper.v1_2_6.helpers.InvisHelper;
import com.blocktyper.v1_2_6.nbt.NBTItem;
import com.blocktyper.v1_2_6.nbt.NbtHelper;

public class ConstructionReceiptInventoryListener extends LayoutBaseListener {

	public static String BLUEPRINTER_CONSTRUCTION_RECEIPT_INVIS_PREFIX = "#BLUEPRINTER_CONSTRUCTION_RECEIPT";

	public static String CONSTRUCTION_MENU_ITEM_ROOT_KEY = "ConstructionReceiptMenuItem";

	public static String MENU_ITEM_KEY_SHOW = "show";
	public static String MENU_ITEM_KEY_HIDE = "hide";
	public static String MENU_ITEM_KEY_TELEPORT = "teleport";
	public static String MENU_ITEM_KEY_RETURN = "return";
	public static String MENU_ITEM_KEY_SYMBOL = "symbol";
	public static String MENU_ITEM_KEY_SYMBOL_VALUE = "symbol-value";

	private Map<String, ConstructionReceipt> playerActiveReceiptMap = new HashMap<>();
	private Map<String, Location> playerLastTeleportedMap = new HashMap<>();

	private Map<String, String> playerLastSymbolMap = new HashMap<>();

	private NBTItem getMenuItem(String displayName, Material material, String menuItemKey, List<String> loreLines) {
		return getMenuItem(displayName, new ComplexMaterial(material, null), menuItemKey, loreLines);
	}

	@SuppressWarnings("deprecation")
	private NBTItem getMenuItem(String displayName, ComplexMaterial complexMaterial, String menuItemKey,
			List<String> loreLines) {

		Short damage = 1;
		ItemStack menuItem = new ItemStack(complexMaterial.getMaterial(), 1, damage,
				complexMaterial.getData() == null ? 0 : complexMaterial.getData());
		ItemMeta itemMeta = menuItem.getItemMeta();

		if (itemMeta == null) {
			itemMeta = new ItemStack(Material.COBBLESTONE).getItemMeta();
		}

		if (loreLines != null) {
			itemMeta.setLore(loreLines);
		}
		if (displayName != null) {
			itemMeta.setDisplayName(displayName);
		}

		menuItem.setItemMeta(itemMeta);

		NBTItem nbtItem = new NBTItem(menuItem);
		nbtItem.setString(CONSTRUCTION_MENU_ITEM_ROOT_KEY, menuItemKey);
		return nbtItem;
	}

	private String getMenuItemKey(ItemStack clickedItem) {
		NBTItem nbtItem = new NBTItem(clickedItem);
		return nbtItem.getString(CONSTRUCTION_MENU_ITEM_ROOT_KEY);
	}

	/*
	 * ON INVENTORY CLICK
	 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
	public void onConstructionReceiptInventoryClickEvent(InventoryClickEvent event) {

		if (!(event.getWhoClicked() instanceof Player)) {
			return;
		}

		if (event.getClickedInventory() == null) {
			return;
		}

		if (event.getInventory() != null && event.getInventory().getName() != null) {
			String inventoryName = event.getInventory().getName();
			inventoryName = InvisHelper.convertToVisibleString(inventoryName);
			if (inventoryName.startsWith(BLUEPRINTER_CONSTRUCTION_RECEIPT_INVIS_PREFIX)) {
				event.setCancelled(true);
				handleMenuClick(event);
				return;
			}
		}

		if (event.getClick() != ClickType.RIGHT) {
			return;
		}

		if (event.getClickedInventory().getType() != InventoryType.PLAYER) {
			return;
		}

		HumanEntity player = event.getWhoClicked();

		ItemStack clickedItem = event.getCurrentItem();

		if (clickedItem == null || clickedItem.getType() == Material.AIR) {
			return;
		}

		ConstructionReceipt constructionReceipt = plugin.getConstructionReciept(clickedItem);
		
		if(constructionReceipt == null){
			return;
		}

		openInventory(player, constructionReceipt);

		event.setCancelled(true);
		return;
	}
	
	private void resaveConstructionReceipt(ConstructionReceipt constructionReceipt, HumanEntity player){
		String uniqueId = constructionReceipt.getUuid();
		ItemStack receiptInBag = NbtHelper.getMathcingItemByUniqueId(uniqueId, player.getInventory());
		NBTItem receiptInBagNbtItem = new NBTItem(receiptInBag);
		receiptInBagNbtItem.setObject(plugin.getConstructionRecieptKey(), constructionReceipt);

		NbtHelper.replaceUniqueNbtItemInInventory(player, receiptInBagNbtItem.getItem(), uniqueId,
				player.getInventory());
	}

	@SuppressWarnings("deprecation")
	private void handleMenuClick(InventoryClickEvent event) {

		ItemStack clickedItem = event.getCurrentItem();

		HumanEntity player = event.getWhoClicked();
		
		if(!player.isOp()){
			return;
		}

		if (clickedItem == null || clickedItem.getType() == Material.AIR) {
			return;
		}

		ConstructionReceipt constructionReceipt = playerActiveReceiptMap.get(player.getName());

		String key = getMenuItemKey(clickedItem);
		if (key == null) {
			if (playerLastSymbolMap.containsKey(player.getName())) {
				
				if(!clickedItem.getType().isBlock()){
					return;
				}
				
				String symbol = playerLastSymbolMap.get(player.getName());
				playerLastSymbolMap.remove(player.getName());

				Character symbolAsChar = null;
				Byte data = clickedItem.getData() != null ? clickedItem.getData().getData() : null;
				
				ComplexMaterial changeToMaterial = new ComplexMaterial(clickedItem.getType(), data);
				
				for (BlockChange change : constructionReceipt.getChanges()) {
					if (symbol.equals(change.getSymbol() + "")) {
						symbolAsChar = change.getSymbol();
						
						ComplexMaterial expectedMaterial = constructionReceipt.getSymbolMap().get(symbolAsChar);
						
						Block exisitingBlock = player.getWorld().getBlockAt(change.getCoord().getX(), change.getCoord().getY(), change.getCoord().getZ());
						if(exisitingBlock != null){
							ComplexMaterial existingMaterial = new ComplexMaterial(exisitingBlock.getType(), exisitingBlock.getData());
							
							if(expectedMaterial.equals(existingMaterial)){
								change.setTo(changeToMaterial);
							}else{
								change.setTo(existingMaterial);
							}
						}
					}
				}

				constructionReceipt.getSymbolMap().put(symbolAsChar, changeToMaterial);

				BuildProcess buildProcess = new BuildProcess(plugin, constructionReceipt);
				buildProcess.applyChanges(player.getWorld(), false, symbolAsChar);

				resaveConstructionReceipt(constructionReceipt, player);

				openInventory(player, constructionReceipt);
			}

			return;
		}

		BuildProcess buildProcess = new BuildProcess(plugin, constructionReceipt);

		if (key.equals(MENU_ITEM_KEY_HIDE)) {
			buildProcess.restoreOriginalBlocks(player.getWorld());
			playerLastSymbolMap.remove(player.getName());
			resaveConstructionReceipt(constructionReceipt, player);
			openInventory(player, constructionReceipt);
		} else if (key.equals(MENU_ITEM_KEY_SHOW)) {
			buildProcess.applyChanges(player.getWorld(), true, null);
			playerLastSymbolMap.remove(player.getName());
			resaveConstructionReceipt(constructionReceipt, player);
			openInventory(player, constructionReceipt);
		} else if (key.equals(MENU_ITEM_KEY_TELEPORT)) {
			
			World world = plugin.getServer().getWorld(constructionReceipt.getWorld());
			
			if(world == null){
				String missingWorldMessage = getLocalizedMessage(LocalizedMessageEnum.MISSING_WORLD.getKey(), player);
				MessageFormat.format(missingWorldMessage, constructionReceipt.getWorld());
				player.sendMessage(missingWorldMessage);
				return;
			}
			
			playerLastTeleportedMap.put(player.getName(), player.getLocation());
			
			Location location = new Location(world, constructionReceipt.getPlayerX(),
					constructionReceipt.getPlayerY(), constructionReceipt.getPlayerZ());

			player.teleport(location);
			playerLastSymbolMap.remove(player.getName());
		} else if (key.equals(MENU_ITEM_KEY_RETURN)) {
			Location location = playerLastTeleportedMap.get(player.getName());
			player.teleport(location);
			playerLastTeleportedMap.remove(player.getName());
			playerLastSymbolMap.remove(player.getName());
		} else if (key.equals(MENU_ITEM_KEY_SYMBOL)) {

			NBTItem nbtItem = new NBTItem(clickedItem);
			String symbol = nbtItem.getString(MENU_ITEM_KEY_SYMBOL_VALUE);
			String materialSwapMessage = getLocalizedMessage(LocalizedMessageEnum.MATERIAL_SWAP.getKey(), player);
			materialSwapMessage = MessageFormat.format(materialSwapMessage, symbol);
			player.sendMessage(materialSwapMessage);
			playerLastSymbolMap.put(player.getName(), symbol);
		}

	}
	
	private void addTeleportMenuOptions(List<ItemStack> menuItems, HumanEntity player, ConstructionReceipt constructionReceipt){
		
		String teleportMessage = getLocalizedMessage(LocalizedMessageEnum.TELEPORT.getKey(), player);
		
		String locationMessage = Coord.getFormatted(constructionReceipt.getPlayerX(), constructionReceipt.getPlayerY(), constructionReceipt.getPlayerZ(), constructionReceipt.getWorld());
		List<String> locationLore = Arrays.asList(locationMessage);
		
		menuItems.add(getMenuItem(teleportMessage, Material.COMPASS, MENU_ITEM_KEY_TELEPORT, locationLore).getItem());

		if (playerLastTeleportedMap.containsKey(player.getName())) {
			Location returnLocation = playerLastTeleportedMap.get(player.getName());
			String returnMessage = getLocalizedMessage(LocalizedMessageEnum.RETURN.getKey(), player);
			
			locationMessage = Coord.getFormatted(returnLocation, true);
			locationLore = Arrays.asList(locationMessage);
			
			menuItems.add(getMenuItem(returnMessage, Material.BED, MENU_ITEM_KEY_RETURN, locationLore).getItem());
		}

	}

	private void openInventory(HumanEntity player, ConstructionReceipt constructionReceipt) {
		List<ItemStack> menuItems = new ArrayList<>();

		if(constructionReceipt.isShowing()){
			
			String hideMessage = getLocalizedMessage(LocalizedMessageEnum.HIDE.getKey(), player);
			menuItems.add(getMenuItem(hideMessage, Material.BUCKET, MENU_ITEM_KEY_HIDE, null).getItem());
			
			addTeleportMenuOptions(menuItems, player, constructionReceipt);

			for (Character symbol : constructionReceipt.getSymbolMap().keySet()) {
				ComplexMaterial complexMaterial = constructionReceipt.getSymbolMap().get(symbol);
				List<String> lore = Arrays.asList("Change material for symbol group " + symbol);
				NBTItem menuItem = getMenuItem(null, complexMaterial, MENU_ITEM_KEY_SYMBOL, lore);
				menuItem.setString(MENU_ITEM_KEY_SYMBOL_VALUE, symbol + "");
				menuItems.add(menuItem.getItem());
			}
		}else{
			String showMessage = getLocalizedMessage(LocalizedMessageEnum.SHOW.getKey(), player);
			menuItems.add(getMenuItem(showMessage, Material.WATER_BUCKET, MENU_ITEM_KEY_SHOW, null).getItem());
			addTeleportMenuOptions(menuItems, player, constructionReceipt);
		}

		

		playerActiveReceiptMap.put(player.getName(), constructionReceipt);
		playerLastSymbolMap.remove(player.getName());
		openInventory(menuItems, player);
	}

	private void openInventory(List<ItemStack> menuItems, HumanEntity player) {
		String constructionReceiptInventoryName = plugin
				.getLocalizedMessage(LocalizedMessageEnum.CONSTRUCTION_RECEIPT.getKey(), player);

		constructionReceiptInventoryName = InvisHelper.convertToInvisibleString(
				BLUEPRINTER_CONSTRUCTION_RECEIPT_INVIS_PREFIX) + ChatColor.RESET + constructionReceiptInventoryName;

		int rows = (menuItems.size() / 9) + (menuItems.size() % 9 > 0 ? 1 : 0);

		Inventory menuInventory = Bukkit.createInventory(null, rows * 9, constructionReceiptInventoryName);

		for (ItemStack menuItem : menuItems) {
			menuInventory.addItem(menuItem);
		}

		new BukkitRunnable() {
			@Override
			public void run() {
				player.closeInventory();
				player.openInventory(menuInventory);

			}
		}.runTaskLater(plugin, 1L);
	}

}
