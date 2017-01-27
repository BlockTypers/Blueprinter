package com.blocktyper.blueprinter.listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import com.blocktyper.blueprinter.BlueprinterPlugin;
import com.blocktyper.blueprinter.Layout;

public class RequireMatsClickListener extends LayoutBaseListener {

	public RequireMatsClickListener(BlueprinterPlugin plugin) {
		super(plugin);
	}

	/*
	 * ON INVENTORY CLICK
	 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
	public void onInventoryClickEvent(InventoryClickEvent event) {

		plugin.debugInfo("### onInventoryClickEvent: " + event.getClick().name());

		if (!(event.getWhoClicked() instanceof Player)) {
			return;
		}

		if (event.getClickedInventory() == null) {
			plugin.debugInfo("### clicked inventory was null");
			return;
		}

		Player player = ((Player) event.getWhoClicked());

		ItemStack item = event.getCurrentItem();

		if (item == null || item.getAmount() > 1) {
			plugin.debugInfo("### item == null || item.getAmount() > 1");
			return;
		}

		Layout layout = plugin.getLayout(item);

		if (layout != null) {

			if (!layout.requireMats()) {
				plugin.debugInfo("### !layout.requireMats()");
				return;
			}

			if (layout.isRequireMatsLoaded() && event.getAction().equals(InventoryAction.SWAP_WITH_CURSOR)) {
				plugin.debugInfo("### SWAP_WITH_CURSOR");
				ItemStack cursor = event.getCursor();
				
				if (cursor == null) {
					plugin.debugInfo("### cursor == null");
					return;
				} else if (!Layout.itemIsSuitableForLoading(cursor)) {
					plugin.debugInfo("### !Layout.itemIsSuitableForLoading(cursor): " + cursor.getType().name());
					return;
				}
				
				Material cursorType = cursor.getType();

				if (!layout.getRequirements().containsKey(cursorType)) {
					plugin.debugInfo("###!layout.getRequirements().containsKey(cursor.getType())");
					return;
				}

				int requiredAmount = layout.getRequirements().get(cursorType);

				if (requiredAmount < 1) {
					plugin.debugInfo("### requiredAmount < 1");
					return;
				}
				
				if (layout.getSupplies() == null) {
					layout.setSupplies(new HashMap<>());
				}
				
				if(!layout.getSupplies().containsKey(cursorType)){
					layout.getSupplies().put(cursorType, 0);
				}

				Integer amountLoaded = layout.getSupplies().get(cursorType);
				amountLoaded = amountLoaded != null ? amountLoaded : 0;

				int amountLeft = requiredAmount - amountLoaded;

				if (amountLeft < 1) {
					plugin.debugInfo("### amountLeft < 1");
					return;
				}

				if (amountLeft >= cursor.getAmount()) {
					amountLoaded = amountLoaded + cursor.getAmount();
					cursor.setAmount(0);
					player.setItemOnCursor(null);
				} else {
					amountLoaded = amountLoaded + amountLeft;
					cursor.setAmount(cursor.getAmount() - amountLeft);
					player.setItemOnCursor(cursor);
				}

				layout.getSupplies().put(cursorType, amountLoaded);

				

				item = plugin.setLayout(item, layout);

				event.getClickedInventory().setItem(event.getSlot(), item);
				event.setCancelled(true);
				return;
			} else if (event.getClick().equals(ClickType.RIGHT)) {
				List<ItemStack> requiredItems = new ArrayList<>();
				for (Material material : layout.getRequirements().keySet()) {
					int requiredAmount = layout.getRequirements().get(material);

					Integer amountObtained = getAmountObtained(player, layout, material);
					amountObtained = amountObtained != null ? amountObtained : 0;
					ItemStack requiredItem = new ItemStack(material);
					ItemMeta itemMeta = requiredItem.getItemMeta();
					
					if(itemMeta != null){
						List<String> lore = Arrays.asList(amountObtained + "/" + requiredAmount);
						itemMeta.setLore(lore);
						requiredItem.setItemMeta(itemMeta);
					}
					
					if (requiredAmount > amountObtained) {
						itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
						requiredItem.addUnsafeEnchantment(Enchantment.LUCK, 1);
					}
					requiredItems.add(requiredItem);
				}

				int rows = (requiredItems.size() / 9) + (requiredItems.size() % 9 > 0 ? 1 : 0);

				Inventory requiredMaterialsInventory = Bukkit.createInventory(null, rows * 9, "Required Materials");

				for (ItemStack requiredItem : requiredItems) {
					requiredMaterialsInventory.addItem(requiredItem);
				}

				new BukkitRunnable() {
					@Override
					public void run() {
						player.closeInventory();
						player.openInventory(requiredMaterialsInventory);

					}
				}.runTaskLater(plugin, 1L);

				event.setCancelled(true);
				return;
			}
		}
	}

	private Integer getAmountObtained(Player player, Layout layout, Material material) {
		Integer amountObtained = null;

		if (layout.isRequireMatsLoaded()) {
			amountObtained = layout.getSupplies() != null && layout.getSupplies().get(material) != null
					? layout.getSupplies().get(material) : 0;
		} else {
			amountObtained = getAmountOfMaterialInBag(player, material);
		}

		return amountObtained;
	}

}
