package com.blocktyper.blueprinter.listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.scheduler.BukkitRunnable;

import com.blocktyper.blueprinter.LocalizedMessageEnum;
import com.blocktyper.blueprinter.data.ConstructionReciept;
import com.blocktyper.v1_1_8.helpers.ComplexMaterial;
import com.blocktyper.v1_1_8.helpers.InvisibleLoreHelper;
import com.blocktyper.v1_1_8.nbt.NBTItem;

public class ConstructionReceiptInventoryListener extends LayoutBaseListener {

	public static String BLUEPRINTER_CONSTRUCTION_RECEIPT_INVIS_PREFIX = "#BLUEPRINTER_CONSTRUCTION_RECEIPT";

	public static String CONSTRUCTION_MENU_ITEM_ROOT_KEY = "ConstructionReceiptMenuItem";

	public static String MENU_ITEM_KEY_SHOW = "show";
	public static String MENU_ITEM_KEY_HIDE = "hide";
	public static String MENU_ITEM_KEY_SYMBOL = "symbol";
	public static String MENU_ITEM_KEY_SYMBOL_VALUE = "symbol-value";

	private NBTItem getMenuItem(String displayName, Material material, String menuItemKey, List<String> loreLines) {
		return getMenuItem(displayName, new ComplexMaterial(material, null), menuItemKey, loreLines);
	}

	@SuppressWarnings("deprecation")
	private NBTItem getMenuItem(String displayName, ComplexMaterial complexMaterial, String menuItemKey,
			List<String> loreLines) {
		Material material = complexMaterial.getMaterial();
		ItemStack menuItem = new ItemStack(material);
		ItemMeta itemMeta = menuItem.getItemMeta();

		if (complexMaterial.getData() != null) {
			menuItem.setData(new MaterialData(material, complexMaterial.getData()));
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
			inventoryName = InvisibleLoreHelper.convertToVisibleString(inventoryName);
			if (inventoryName.startsWith(BLUEPRINTER_CONSTRUCTION_RECEIPT_INVIS_PREFIX)) {
				event.setCancelled(true);
				return;
			}
		}

		HumanEntity player = event.getWhoClicked();

		ItemStack itemInHand = player.getEquipment().getItemInMainHand();

		if (itemInHand == null || itemInHand.getType() == Material.AIR) {
			return;
		}

		ConstructionReciept constructionReciept = plugin.getConstructionReciept(itemInHand);

		List<ItemStack> menuItems = new ArrayList<>();

		menuItems.add(getMenuItem("Hide <", Material.BUCKET, MENU_ITEM_KEY_HIDE, null).getItem());
		menuItems.add(getMenuItem("Show  > ", Material.WATER_BUCKET, MENU_ITEM_KEY_SHOW, null).getItem());

		for (Character symbol : constructionReciept.getSymbolMap().keySet()) {
			ComplexMaterial complexMaterial = constructionReciept.getSymbolMap().get(symbol);
			List<String> lore = Arrays.asList("Change material for symbol group " + symbol);
			NBTItem menuItem = getMenuItem(null, complexMaterial, MENU_ITEM_KEY_SYMBOL, lore);
			menuItem.setString(MENU_ITEM_KEY_SYMBOL_VALUE, symbol + "");
			menuItems.add(menuItem.getItem());
		}

		int rows = (menuItems.size() / 9) + (menuItems.size() % 9 > 0 ? 1 : 0);

		String constructionReceiptInventoryName = plugin
				.getLocalizedMessage(LocalizedMessageEnum.CONSTRUCTION_RECEIPT.getKey(), event.getWhoClicked());

		constructionReceiptInventoryName = InvisibleLoreHelper.convertToInvisibleString(
				BLUEPRINTER_CONSTRUCTION_RECEIPT_INVIS_PREFIX) + ChatColor.RESET + constructionReceiptInventoryName;

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

		event.setCancelled(true);
		return;
	}

}
